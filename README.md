# Dungeonfall

A self-contained browser dungeon crawler. No build step or package installation is required.

## Run on Wispbyte

This repository includes `server.py`, a small Python web server for hosting Dungeonfall.

1. Upload or clone the repository files into your Wispbyte server folder. Keep `server.py` and `index.html` together.
2. Set the server's startup command to:

   ```bash
   /usr/local/bin/python /home/container/server.py
   ```

3. Start the Wispbyte server and open its assigned website address.

The server binds to `0.0.0.0` and uses Wispbyte's `SERVER_PORT` or `PORT` environment variable. It falls back to port `8080` if neither is set. Visit `/health` on the website address to check that the server is responding.

No extra Python packages or requirements file are needed; the server uses Python's standard library.

## Play locally

Open `index.html` in a modern browser, or serve this folder with any static web server.

## Controls

- **WASD / Arrow keys:** move
- **Space:** cast a short-range spell
- **Q:** use a healing potion
- **E:** interact with nearby objects
- Touch controls are available on mobile.

## Features

- Randomly generated dungeon layouts and floors
- Slimes, wisps and shades
- Chests, coins, relics, essence and potions
- Experience, progression, health and floor descent
- Browser-local save data using localStorage
- Responsive layout and mobile controls
- Procedural canvas visuals; no external image assets required

## Other hosting options

The site can also be hosted with GitHub Pages, Cloudflare Pages, Netlify or any ordinary web server. For GitHub Pages, set the repository Pages source to **GitHub Actions**.

## Assets and attribution

The current build draws its dungeon tiles, characters, effects and interface procedurally in canvas/CSS. It does not redistribute third-party art or depend on hotlinked image files. Google Images is a discovery tool, not an asset licence; only add external art when its licence explicitly permits the intended use, and preserve the required attribution.

## Saves

Progress is saved in the current browser on the current device. Clearing browser site data will clear that local save. This version does not yet have accounts or server-side cross-device saves.
