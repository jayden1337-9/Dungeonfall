#!/usr/bin/env python3
"""Dungeonfall: Python web app with server-side JSON saves."""

from datetime import datetime, timezone
from pathlib import Path
from uuid import UUID
import json
import os
import tempfile

from flask import Flask, jsonify, request, send_from_directory

ROOT = Path(__file__).resolve().parent
SAVE_DIR = ROOT / "data" / "saves"
SAVE_DIR.mkdir(parents=True, exist_ok=True)

app = Flask(__name__, static_folder="assets", static_url_path="/assets")
app.config["MAX_CONTENT_LENGTH"] = 128 * 1024


def save_path(player_id: str) -> Path:
    # UUID parsing prevents path traversal and arbitrary file access.
    safe_id = str(UUID(player_id))
    return SAVE_DIR / f"{safe_id}.json"


@app.get("/")
def home():
    return send_from_directory(ROOT, "index.html")


@app.get("/api/health")
def health():
    return jsonify({"ok": True, "app": "Dungeonfall", "storage": "json"})


@app.get("/api/save/<player_id>")
def get_save(player_id):
    try:
        path = save_path(player_id)
    except (ValueError, AttributeError):
        return jsonify({"error": "Invalid player id"}), 400

    if not path.is_file():
        return jsonify({"error": "No saved journey"}), 404

    try:
        with path.open("r", encoding="utf-8") as file:
            data = json.load(file)
        return jsonify(data)
    except (OSError, json.JSONDecodeError):
        app.logger.exception("Could not read save file")
        return jsonify({"error": "Save file could not be read"}), 500


@app.put("/api/save/<player_id>")
def put_save(player_id):
    try:
        path = save_path(player_id)
    except (ValueError, AttributeError):
        return jsonify({"error": "Invalid player id"}), 400

    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Expected a JSON save object"}), 400

    required = ("level", "hp", "maxHp", "xp", "essence", "potions",
                "relics", "keys", "coins", "world", "player",
                "enemies", "chests", "stairs")
    if any(key not in data for key in required):
        return jsonify({"error": "Save is missing required game data"}), 400
    if not isinstance(data["world"], list) or not isinstance(data["player"], dict):
        return jsonify({"error": "Invalid dungeon state"}), 400

    data["updated_at"] = datetime.now(timezone.utc).isoformat()
    temp_name = None
    try:
        # Atomic replace keeps the previous save intact if a write is interrupted.
        with tempfile.NamedTemporaryFile(
            "w", encoding="utf-8", dir=SAVE_DIR, delete=False,
            prefix=f".{path.stem}-", suffix=".tmp"
        ) as file:
            json.dump(data, file, separators=(",", ":"))
            file.flush()
            os.fsync(file.fileno())
            temp_name = file.name
        os.replace(temp_name, path)
        return jsonify({"ok": True, "updated_at": data["updated_at"]})
    except OSError:
        app.logger.exception("Could not write save file")
        if temp_name and os.path.exists(temp_name):
            os.unlink(temp_name)
        return jsonify({"error": "Could not save journey"}), 500


if __name__ == "__main__":
    port = int(os.environ.get("SERVER_PORT") or os.environ.get("PORT") or "8080")
    print(f"Dungeonfall Python app listening on 0.0.0.0:{port}", flush=True)
    app.run(host="0.0.0.0", port=port, threaded=True)
