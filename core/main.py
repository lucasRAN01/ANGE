import argparse
import os
from pathlib import Path
import secrets
import uvicorn
from app import create_app

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--cert")
    parser.add_argument("--key")
    args = parser.parse_args()
    if args.host not in {"127.0.0.1", "localhost", "::1"} and not (args.cert and args.key):
        parser.error("Une connexion LAN nécessite --cert et --key. Ne pas exposer ce service sur Internet.")
    root = Path(__file__).resolve().parent
    os.chdir(root)
    if not os.getenv("ANGE_ACCESS_TOKEN"):
        token_path = root / ".local" / "access-token.txt"
        token_path.parent.mkdir(exist_ok=True)
        if not token_path.exists():
            token_path.write_text(secrets.token_urlsafe(40), encoding="utf-8")
        os.environ["ANGE_ACCESS_TOKEN"] = token_path.read_text(encoding="utf-8").strip()
        print("Jeton local dans core/.local/access-token.txt. Ne pas partager ce fichier.")
    uvicorn.run(create_app(), host=args.host, port=args.port, ssl_certfile=args.cert, ssl_keyfile=args.key,
                access_log=False, limit_concurrency=16)

if __name__ == "__main__":
    main()
