"""Explicit local/cloud routing. No tools, commands, or hidden cloud fallback."""
from dataclasses import dataclass
import os
import httpx

SYSTEM = (
    "Tu es ANGE, une compagne IA personnelle calme, chaleureuse, respectueuse et concise. "
    "Tu es une IA, pas une humaine. Réponds dans la langue demandée. "
    "N'invente jamais avoir contrôlé un appareil, consulté un agenda, mémorisé une donnée "
    "ou exécuté une commande. Aucun outil système ne t'est fourni. "
    "N'affiche pas de pensées internes. Une réponse doit rester facile à écouter."
)

class EngineError(Exception):
    def __init__(self, message: str, status: int = 503):
        super().__init__(message)
        self.status = status

@dataclass(frozen=True)
class Config:
    token: str
    ollama_model: str = ""
    ollama_url: str = "http://127.0.0.1:11434"
    cloud_model: str = ""
    cloud_key: str = ""
    database: str = ".local/ange.sqlite3"

    @classmethod
    def from_env(cls):
        token = os.environ.get("ANGE_ACCESS_TOKEN", "")
        if len(token) < 32:
            raise ValueError("ANGE_ACCESS_TOKEN doit contenir au moins 32 caractères.")
        return cls(token=token, ollama_model=os.getenv("ANGE_OLLAMA_MODEL", ""),
                   ollama_url=os.getenv("ANGE_OLLAMA_URL", "http://127.0.0.1:11434"),
                   cloud_model=os.getenv("ANGE_CLOUD_MODEL", ""), cloud_key=os.getenv("OPENAI_API_KEY", ""),
                   database=os.getenv("ANGE_DATABASE", ".local/ange.sqlite3"))

class Engine:
    def __init__(self, config: Config, client: httpx.AsyncClient):
        self.config, self.client = config, client

    async def chat(self, message, history, mode, language, cloud_consent):
        messages = [{"role": m.role, "content": m.content} for m in history]
        messages.append({"role": "user", "content": message})
        instruction = SYSTEM + f" Langue demandée : {language}."
        try:
            if mode == "local":
                if not self.config.ollama_model:
                    raise EngineError("Choisissez un modèle installé dans ANGE_OLLAMA_MODEL sur le PC.")
                response = await self.client.post(self.config.ollama_url.rstrip("/") + "/api/chat", json={
                    "model": self.config.ollama_model, "stream": False,
                    "messages": [{"role": "system", "content": instruction}] + messages,
                    "options": {"num_ctx": 4096, "num_predict": 512}, "keep_alive": "2m"})
                response.raise_for_status()
                text = response.json().get("message", {}).get("content", "")
                model = self.config.ollama_model
            else:
                if not cloud_consent:
                    raise EngineError("Autorisation cloud requise pour ce message et son historique.", 403)
                if not self.config.cloud_key or not self.config.cloud_model:
                    raise EngineError("Clé ou modèle cloud non configuré sur le PC.")
                response = await self.client.post("https://api.openai.com/v1/responses", headers={
                    "Authorization": "Bearer " + self.config.cloud_key}, json={
                    "model": self.config.cloud_model, "instructions": instruction,
                    "input": messages, "max_output_tokens": 700, "store": False})
                response.raise_for_status()
                data = response.json()
                text = "\n".join(c.get("text", "") for item in data.get("output", [])
                                 if item.get("type") == "message" for c in item.get("content", [])
                                 if c.get("type") == "output_text")
                model = self.config.cloud_model
            if not isinstance(text, str) or not text.strip():
                raise EngineError("Le moteur n'a pas fourni de réponse textuelle exploitable.", 502)
            return {"reply": text.strip()[:12000], "engine": mode, "model": model}
        except httpx.TimeoutException:
            raise EngineError("Le moteur a dépassé le délai. Aucun autre fournisseur n'a été appelé.", 504)
        except httpx.HTTPStatusError as exc:
            code = exc.response.status_code
            if code == 429:
                raise EngineError("Le moteur limite les requêtes ou son quota est atteint.", 429)
            raise EngineError("Le moteur a refusé la requête. Vérifiez son modèle et sa configuration.", 502)
        except (httpx.RequestError, ValueError, TypeError, AttributeError):
            raise EngineError("Moteur indisponible ou réponse invalide. Aucun basculement cloud automatique.", 502)
