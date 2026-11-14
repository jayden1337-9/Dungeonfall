#!/usr/bin/env python3
"""Small production-friendly static file server for Dungeonfall on Wispbyte."""

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
import os

ROOT = Path(__file__).resolve().parent


class DungeonfallHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def do_GET(self):
        if urlsplit(self.path).path == "/health":
            body = b"Dungeonfall is online"
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)
            return
        super().do_GET()

    def log_message(self, fmt, *args):
        print("[Dungeonfall] " + (fmt % args), flush=True)


def main():
    # Wispbyte/Pterodactyl environments may expose either variable.
    port = int(os.environ.get("SERVER_PORT") or os.environ.get("PORT") or "8080")
    server = ThreadingHTTPServer(("0.0.0.0", port), DungeonfallHandler)
    print(f"Dungeonfall web server listening on 0.0.0.0:{port}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Dungeonfall server stopping", flush=True)
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
