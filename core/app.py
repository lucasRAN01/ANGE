"""ANGE Core: authenticated chat + explicit personal notes. LAN requires TLS in main.py."""
import asyncio
import secrets
import sqlite3
import time
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Literal
import httpx
from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field, field_validator
from engine import Config, Engine, EngineError

class Turn(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=4000)

class Chat(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    history: list[Turn] = Field(default_factory=list, max_length=20)
    mode: Literal["local", "cloud"] = "local"
    language: Literal["fr", "en", "mg"] = "fr"
    cloud_consent: bool = False

    @field_validator("message")
    @classmethod
    def nonblank(cls, value):
        if not value.strip():
            raise ValueError("Message vide")
        return value.strip()

class Note(BaseModel):
    text: str = Field(min_length=1, max_length=4000)
    @field_validator("text")
    @classmethod
    def nonblank(cls, value):
        if not value.strip():
            raise ValueError("Note vide")
        return value.strip()

def create_app(config=None, transport=None):
    config = config or Config.from_env()
    db_path = Path(config.database)
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(db_path) as db:
        db.execute("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, text TEXT NOT NULL, created REAL NOT NULL)")
    @asynccontextmanager
    async def lifespan(application):
        async with httpx.AsyncClient(timeout=90, transport=transport, follow_redirects=False, trust_env=False) as client:
            application.state.engine = Engine(config, client)
            yield
    app = FastAPI(title="ANGE Core Companion", version="0.3.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)
    bearer = HTTPBearer(auto_error=False)
    semaphore = asyncio.Semaphore(1)  # One model request at a time on an 8 GB machine.

    async def authenticated(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
        if not credentials or not secrets.compare_digest(credentials.credentials, config.token):
            raise HTTPException(401, "Authentification requise", headers={"WWW-Authenticate": "Bearer"})

    @app.middleware("http")
    async def bounds(request: Request, call_next):
        # Read the actual body too: Content-Length is untrusted and chunked requests may omit it.
        if request.method in {"POST", "PUT", "PATCH"}:
            body = bytearray()
            async for chunk in request.stream():
                body.extend(chunk)
                if len(body) > 100000:
                    from fastapi.responses import JSONResponse
                    return JSONResponse({"detail": "Requête trop volumineuse"}, status_code=413)
            request._body = bytes(body)
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Cache-Control"] = "no-store"
        response.headers["Referrer-Policy"] = "no-referrer"
        return response

    @app.get("/health")
    def health():
        return {"service": "ANGE Core", "version": "0.3.0"}

    @app.get("/v1/status", dependencies=[Depends(authenticated)])
    def status():
        return {"local_configured": bool(config.ollama_model),
                "cloud_configured": bool(config.cloud_key and config.cloud_model),
                "local_model": config.ollama_model or None, "cloud_model": config.cloud_model or None,
                "tools": False, "automatic_cloud_fallback": False}

    @app.post("/v1/chat", dependencies=[Depends(authenticated)])
    async def chat(body: Chat):
        if semaphore.locked():
            raise HTTPException(429, "ANGE traite déjà une demande. Réessayez après sa réponse.")
        async with semaphore:
            try:
                return await app.state.engine.chat(body.message, body.history, body.mode, body.language, body.cloud_consent)
            except EngineError as exc:
                raise HTTPException(exc.status, str(exc))

    @app.get("/v1/notes", dependencies=[Depends(authenticated)])
    def notes():
        with sqlite3.connect(db_path) as db:
            return [{"id": r[0], "text": r[1], "created": r[2]} for r in db.execute("SELECT id,text,created FROM notes ORDER BY id DESC LIMIT 500")]

    @app.post("/v1/notes", dependencies=[Depends(authenticated)], status_code=201)
    def add_note(body: Note):
        with sqlite3.connect(db_path) as db:
            if db.execute("SELECT count(*) FROM notes").fetchone()[0] >= 500:
                raise HTTPException(409, "Limite de 500 notes atteinte. Exportez ou supprimez des notes.")
            cursor = db.execute("INSERT INTO notes(text,created) VALUES (?,?)", (body.text, time.time()))
            return {"id": cursor.lastrowid, "text": body.text}

    @app.put("/v1/notes/{note_id}", dependencies=[Depends(authenticated)])
    def update_note(note_id: int, body: Note):
        with sqlite3.connect(db_path) as db:
            if not db.execute("UPDATE notes SET text=? WHERE id=?", (body.text, note_id)).rowcount:
                raise HTTPException(404, "Note inexistante")
        return {"id": note_id, "text": body.text}

    @app.delete("/v1/notes/{note_id}", dependencies=[Depends(authenticated)])
    def delete_note(note_id: int):
        with sqlite3.connect(db_path) as db:
            if not db.execute("DELETE FROM notes WHERE id=?", (note_id,)).rowcount:
                raise HTTPException(404, "Note inexistante")
        return {"deleted": note_id}

    @app.delete("/v1/notes", dependencies=[Depends(authenticated)])
    def erase_notes():
        with sqlite3.connect(db_path) as db:
            db.execute("DELETE FROM notes")
        return {"deleted": "all"}

    # Same-origin browser app. No broad CORS configuration or authentication tokens in URLs.
    app.mount("/ui", StaticFiles(directory=Path(__file__).resolve().parents[1] / "windows", html=True), name="ui")
    return app
