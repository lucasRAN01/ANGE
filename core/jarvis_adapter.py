"""ANGE adapters for the pinned MIT Jarvis source. No tool auto-discovery or cloud imports.

Memory operations are adapted from tools/memoire.py, Copyright (c) 2026 sosoj92.
The original core/memoire.py, util.py and confidentialite.py are loaded unchanged.
See third_party/jarvis/LICENSE and JARVIS_ORIGIN.json.
"""
import importlib.util
from pathlib import Path
import uuid

UPSTREAM = Path(__file__).resolve().parents[1] / "third_party" / "jarvis"
REVISION = "15f3f87e0bfd3fe68ec38deafbfeab22636c3993"

def _load(name):
    # Only these reviewed stdlib-only modules may be loaded; no upstream entry point.
    if name not in {"memoire", "util", "confidentialite"}:
        raise ValueError("Module Jarvis non autorisé")
    spec = importlib.util.spec_from_file_location("ange_jarvis_" + name + uuid.uuid4().hex, UPSTREAM / "core" / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

util = _load("util")
confidentialite = _load("confidentialite")

class JarvisMemory:
    """Explicit, bounded memory operations over the original atomic JSON persistence.

    Each service gets its own upstream module and lock/path bindings, so test or
    app instances never reassign another instance's memory file. One server worker.
    """
    def __init__(self, directory):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=True)
        self.store = _load("memoire")
        self.store.FICHIER = self.directory / "memory.json"
        self.store.ANCIEN = self.directory / "memoire.json"

    def snapshot(self):
        with self.store.VERROU:
            raw = self.store.charger()
            # The upstream normalizer guarantees container types; bound entry types too.
            return {**{k: {str(key): value for key, value in raw[k].items() if isinstance(value, str)}
                       for k in ("preferences", "people", "projects")},
                    "facts": [v for v in raw["facts"] if isinstance(v, str)]}

    def remember(self, category, content, key=""):
        if category not in {"preferences", "people", "projects", "facts"}:
            raise ValueError("Catégorie invalide")
        content, key = content.strip(), key.strip()
        if not content or len(content) > 4000 or len(key) > 80:
            raise ValueError("Souvenir vide ou trop long")
        with self.store.VERROU:
            data = self.snapshot()
            if category == "facts":
                if content not in data["facts"]: data["facts"].append(content)
            else:
                data[category][key or content[:40]] = content
            if sum(len(v) for v in data.values()) > 500:
                raise ValueError("Limite de 500 souvenirs atteinte")
            self.store.sauver(data)
        return data

    def recall(self, query=""):
        need = util.sans_accents(query).strip()
        data = self.snapshot(); found = []
        for category in ("preferences", "people", "projects"):
            for key, value in data[category].items():
                if not need or need in util.sans_accents(f"{key} {value}"):
                    found.append({"category": category, "key": key, "text": value})
        for fact in data["facts"]:
            if not need or need in util.sans_accents(fact): found.append({"category": "facts", "key": "", "text": fact})
        return found

    def forget(self, subject):
        need = util.sans_accents(subject).strip()
        if not need: raise ValueError("Sujet d’oubli requis")
        with self.store.VERROU:
            data = self.snapshot(); removed = 0
            for category in ("preferences", "people", "projects"):
                for key in list(data[category]):
                    if need in util.sans_accents(f"{key} {data[category][key]}"):
                        del data[category][key]; removed += 1
            before = len(data["facts"])
            data["facts"] = [v for v in data["facts"] if need not in util.sans_accents(v)]
            removed += before - len(data["facts"])
            if removed:
                self.store.sauvegarder(); self.store.sauver(data)
            return removed

    def erase(self):
        with self.store.VERROU:
            for path in (self.store.FICHIER, self.store.ANCIEN): path.unlink(missing_ok=True)
            for path in (self.directory / "logs").glob("memory.json.*"):
                if path.is_file(): path.unlink()
