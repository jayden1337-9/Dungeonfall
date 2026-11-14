# MediaVault

MediaVault is a native Android media browser and personal media library.

## Current features
- Built-in Android WebView browser
- Best-effort detection of common direct media URLs requested by pages
- Direct HTTP(S) media downloads to app-specific device storage
- Basic VOD HLS playlist downloading for unencrypted MPEG-TS segment playlists
- HLS and common file playback through AndroidX Media3
- Persistent JSON library metadata
- GitHub Actions debug APK build

## Build an APK without Android Studio
1. Open the repository's **Actions** tab.
2. Select **Build MediaVault APK**.
3. Run the workflow on the `main` branch.
4. Open the completed run and download the `MediaVault-debug` artifact.
5. Extract the ZIP and install `app-debug.apk` on Android.

Every push that changes the Android app or build files also starts the workflow.

## Storage
Downloaded files are kept in the app's external files directory under `MediaVault/`. The library index is stored as JSON in the app's private files directory. Android may remove app-specific files if the app is uninstalled; export/copy important media before uninstalling.

## Current limitations
- Detection depends on WebView exposing a normal HTTP(S) request. It cannot see every source, including `blob:` URLs, and some redirects or service-worker paths may not be visible.
- HLS offline saving currently supports completed, unencrypted VOD playlists with ordinary media segments. Live streams, encrypted streams, byte-range playlists, and fMP4 initialization maps are not downloaded.
- Downloads are for media you own, have permission to save, or that is openly licensed. MediaVault does not bypass DRM, subscriptions, or access controls.

## Legacy Dungeonfall files
The former Wispbyte Dungeonfall files are retained in the repository for now so the existing server folder is not unexpectedly broken. MediaVault itself is the Android project in `app/`.
