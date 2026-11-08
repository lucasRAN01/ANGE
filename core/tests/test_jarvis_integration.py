from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import httpx
import pytest
from fastapi.testclient import TestClient
from app import create_app
from engine import Config
from jarvis_adapter import JarvisMemory, util, confidentialite, REVISION

TOKEN="jarvis-integration-test-"+"x"*32
HEADERS={"Authorization":"Bearer "+TOKEN}

def test_original_helpers_are_active():
    assert util.sans_accents("Ｆrédéric") == "frederic"
    assert util.nettoyer_reponse_vocale("Un instant... je vérifie. voici la réponse.") == "Voici la réponse."
    result=confidentialite.caviarder("test@example.com sk-"+"a"*20)
    assert "test@example.com" not in result and "sk-" not in result

def test_independent_memory_paths_and_restart(tmp_path):
    first=JarvisMemory(tmp_path/"one"); other=JarvisMemory(tmp_path/"two")
    first.remember("preferences","Aime le café","boisson")
    assert other.recall()==[]
    assert JarvisMemory(tmp_path/"one").recall("CAFE")[0]["text"]=="Aime le café"

def test_upstream_atomic_recovery_and_forget(tmp_path):
    memory=JarvisMemory(tmp_path)
    memory.store.FICHIER.write_text('{bad',encoding="utf-8")
    memory.remember("projects","Projet ANGE","ange")
    assert list((tmp_path/"logs").glob("memory.json.corrompu-*"))[0].read_text()=="{bad"
    assert memory.forget("ange")==1
    assert (tmp_path/"logs/memory.json.bak").exists()
    assert not memory.recall()
    memory.erase()
    assert not list((tmp_path/"logs").glob("memory.json.*"))

def test_duplicate_fact_does_not_duplicate(tmp_path):
    memory=JarvisMemory(tmp_path)
    memory.remember("facts","Un fait"); memory.remember("facts","Un fait")
    assert len(memory.recall())==1
    with pytest.raises(ValueError): memory.remember("invalid","test")
    with pytest.raises(ValueError): memory.forget(" ")

def test_limit_does_not_block_duplicate_or_overwrite_disk(tmp_path):
    memory=JarvisMemory(tmp_path)
    memory.store.sauver({"facts":[f"fact-{i}" for i in range(500)]})
    memory.remember("facts","fact-0")
    with pytest.raises(ValueError): memory.remember("facts","new fact")
    assert len(memory.recall())==500

def test_api_memory_is_explicit_authenticated_and_not_sent_to_model(tmp_path):
    calls=[]
    def model(request):
        calls.append(request.content.decode())
        return httpx.Response(200,json={"message":{"content":"Un instant... voici la réponse test@example.com"}})
    config=Config(token=TOKEN,ollama_model="test",database=str(tmp_path/"notes.db"))
    with TestClient(create_app(config,httpx.MockTransport(model))) as client:
        assert client.get("/v1/memory").status_code==401
        assert client.post("/v1/memory",json={"text":"private"}).status_code==401
        assert client.post("/v1/memory/forget",json={"subject":"x","confirmed":True}).status_code==401
        assert client.post("/v1/memory/erase",json={"confirmed":True}).status_code==401
        assert client.post("/v1/memory",headers=HEADERS,json={"category":"preferences","key":"café","text":"private-memory-marker"}).status_code==201
        assert client.get("/v1/memory?query=CAFE",headers=HEADERS).json()["entries"][0]["text"]=="private-memory-marker"
        answer=client.post("/v1/chat",headers=HEADERS,json={"message":"Bonjour"}).json()
        assert answer["reply"]=="Voici la réponse test@example.com"
        assert "test@example.com" not in answer["speech_reply"]
        assert "private-memory-marker" not in calls[0]
        assert client.get("/v1/status",headers=HEADERS).json()["jarvis_revision"]==REVISION
        assert client.post("/v1/memory/forget",headers=HEADERS,json={"subject":"cafe"}).status_code==403
        assert client.post("/v1/memory/forget",headers=HEADERS,json={"subject":"cafe","confirmed":True}).json()["removed"]==1
        assert client.post("/v1/memory/erase",headers=HEADERS,json={}).status_code==403
        assert client.post("/v1/memory/erase",headers=HEADERS,json={"confirmed":True}).json()["recovery_copies_erased"] is True
        assert client.get("/v1/memory",headers=HEADERS).json()["entries"]==[]
    assert not (tmp_path/"jarvis-memory/memory.json").exists()

def test_no_automatic_memory_from_chat(tmp_path):
    config=Config(token=TOKEN,ollama_model="test",database=str(tmp_path/"notes.db"))
    with TestClient(create_app(config,httpx.MockTransport(lambda _:httpx.Response(200,json={"message":{"content":"Bonjour"}})))) as client:
        client.post("/v1/chat",headers=HEADERS,json={"message":"Je préfère le café"})
        assert client.get("/v1/memory",headers=HEADERS).json()["entries"]==[]
