# Dungeonfall

A self-contained browser dungeon crawler. No build step or package installation is required.

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

## Hosting
The site is static and can be hosted with GitHub Pages, Cloudflare Pages, Netlify or any ordinary web server. For GitHub Pages, set the repository Pages source to **GitHub Actions**.

## Assets and attribution
The current build draws its dungeon tiles, characters, effects and interface procedurally in canvas/CSS. It does not redistribute third-party art or depend on hotlinked image files. Google Images is a discovery tool, not an asset licence; only add external art when its licence explicitly permits the intended use, and preserve the required attribution.

## Saves
Progress is saved in the current browser on the current device. Clearing browser site data will clear that local save. This version does not yet have accounts or server-side cross-device saves.
