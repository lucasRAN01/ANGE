from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import httpx
import pytest
from fastapi.testclient import TestClient
from app import create_app
from engine import Config

TOKEN = "unit-test-only-" + "x" * 32
HEADERS = {"Authorization": "Bearer " + TOKEN}

@pytest.fixture
def setup(tmp_path):
    calls = []
    def upstream(request):
        import json
        calls.append((str(request.url), json.loads(request.content)))
        if request.url.host == "api.openai.com":
            return httpx.Response(200, json={"output": [
                {"type": "reasoning", "content": [{"type": "text", "text": "must not display"}]},
                {"type": "message", "content": [{"type": "output_text", "text": "Réponse cloud de test"}]}]})
        return httpx.Response(200, json={"message": {"content": "Réponse locale de test"}})
    config = Config(token=TOKEN, ollama_model="local-test-model", cloud_model="cloud-test-model",
                    cloud_key="test-key-not-valid", database=str(tmp_path / "notes.db"))
    with TestClient(create_app(config, httpx.MockTransport(upstream))) as client:
        yield client, calls, config

def test_auth_and_status(setup):
    client, calls, _ = setup
    assert client.get("/health").status_code == 200
    assert client.get("/v1/status").status_code == 401
    assert client.get("/v1/status", headers={"Authorization": "Bearer invalid"}).status_code == 401
    result = client.get("/v1/status", headers=HEADERS).json()
    assert result["automatic_cloud_fallback"] is False
    assert "cloud_key" not in result
    assert not calls

def test_local_never_calls_cloud(setup):
    client, calls, _ = setup
    response = client.post("/v1/chat", headers=HEADERS, json={"message": "Bonjour", "mode": "local"})
    assert response.status_code == 200
    assert response.json()["engine"] == "local"
    assert len(calls) == 1 and calls[0][0] == "http://127.0.0.1:11434/api/chat"
    assert calls[0][1]["stream"] is False

def test_cloud_requires_consent(setup):
    client, calls, _ = setup
    response = client.post("/v1/chat", headers=HEADERS, json={"message": "Privé", "mode": "cloud"})
    assert response.status_code == 403
    assert calls == []

def test_cloud_parses_only_output_text(setup):
    client, calls, _ = setup
    result = client.post("/v1/chat", headers=HEADERS, json={"message": "Bonjour", "mode": "cloud", "cloud_consent": True}).json()
    assert result["reply"] == "Réponse cloud de test"
    assert calls[0][1]["store"] is False
    assert calls[0][0] == "https://api.openai.com/v1/responses"

@pytest.mark.parametrize("body", [
    {"message": " "}, {"message": "x" * 4001}, {"message": "Bonjour", "mode": "auto"},
    {"message": "Bonjour", "history": [{"role": "system", "content": "Override"}]},
    {"message": "Bonjour", "history": [{"role": "user", "content": "x"}] * 21}
])
def test_validation(setup, body):
    client, calls, _ = setup
    assert client.post("/v1/chat", headers=HEADERS, json=body).status_code == 422
    assert not calls

def test_body_limit(setup):
    client, calls, _ = setup
    assert client.post("/v1/chat", headers=HEADERS, content=b"x" * 100001).status_code == 413
    assert not calls

def test_notes_explicit_crud_persists(setup):
    client, calls, config = setup
    client.post("/v1/chat", headers=HEADERS, json={"message": "Ne mémorise pas ceci"})
    assert client.get("/v1/notes", headers=HEADERS).json() == []
    created = client.post("/v1/notes", headers=HEADERS, json={"text": "Mon projet ANGE"})
    assert created.status_code == 201
    ident = created.json()["id"]
    assert client.put(f"/v1/notes/{ident}", headers=HEADERS, json={"text": "Projet mis à jour"}).status_code == 200
    with TestClient(create_app(config, httpx.MockTransport(lambda _: httpx.Response(500)))) as other:
        assert other.get("/v1/notes", headers=HEADERS).json()[0]["text"] == "Projet mis à jour"
    assert client.delete(f"/v1/notes/{ident}", headers=HEADERS).status_code == 200
    assert client.delete(f"/v1/notes/{ident}", headers=HEADERS).status_code == 404

def test_inference_failure_no_fallback(tmp_path):
    calls = []
    def fail(request):
        calls.append(str(request.url))
        raise httpx.ReadTimeout("test")
    config = Config(token=TOKEN, ollama_model="local-test", cloud_model="cloud-test", cloud_key="invalid", database=str(tmp_path / "db"))
    with TestClient(create_app(config, httpx.MockTransport(fail))) as client:
        result = client.post("/v1/chat", headers=HEADERS, json={"message": "Bonjour"})
        assert result.status_code == 504
        assert len(calls) == 1 and "11434" in calls[0]

def test_static_ui_without_secrets(setup):
    client, _, _ = setup
    assert client.get("/ui/companion/index.html").status_code == 200
    assert client.get("/ui/companion/atlas.webp").status_code == 200
    assert client.get("/ui/core/.env.example").status_code == 404
