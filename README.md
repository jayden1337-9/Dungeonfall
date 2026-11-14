# Dungeonfall

Dungeonfall is a Python-hosted browser RPG with a dynamic API and server-side JSON saves. The browser handles the real-time canvas rendering and controls; the Python backend serves the game, stores full dungeon saves, and exposes the game API.

## Run on Wispbyte

Keep these files together in the Wispbyte server folder:

- `server.py`
- `index.html`
- `requirements.txt`
- `assets/pixel-sprites.svg`

Set the startup command to:

```bash
/usr/local/bin/python -m pip install -U --target /home/container/.python-packages -r /home/container/requirements.txt && /usr/local/bin/python /home/container/server.py
```

The Python app binds to `0.0.0.0` and uses Wispbyte's `SERVER_PORT` or `PORT` environment variable (falling back to 8080). The health endpoint is `/api/health`.

## Server-side saves

Each browser profile receives a random player UUID. The UUID is kept in browser storage only; the actual game state is stored by the Python server as a JSON file under `data/saves/`. The save includes player stats, the current dungeon map, enemies, chests, and stairs. Saves are written with an atomic file replacement so an interrupted write does not truncate the previous save.

The game autosaves while playing and also saves when descending floors or when the journey ends. Use the SAVE button for an immediate save. This prototype does not yet include accounts, password login, or a public leaderboard.

## Controls

- **WASD / Arrow keys:** move
- **Space:** cast a spell
- **Q:** use a healing potion
- **E:** interact with nearby objects
- Touch controls are available on mobile.

## Pixel art

The current repository includes an original pixel-art SVG spritesheet for the player, dungeon tiles, monsters, stairs, chests, and inventory icons.

An additional CC0 reference asset source found during research is the [16x16 Puny Dungeon Tileset by Shade](https://opengameart.org/content/16x16-puny-dungeon-tileset). Its page identifies the art as CC0. The spritesheet currently in this repository is original project artwork rather than a copied download from that pack.

## Local development

Install the dependency and run:

```bash
python -m pip install -r requirements.txt
python server.py
```

Then open `http://127.0.0.1:8080`.

