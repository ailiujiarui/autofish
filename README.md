# Auto Fish

A configurable client-side automatic fishing mod for Minecraft 26.1.2 and NeoForge. This project ports [troyhayes/autofish](https://github.com/troyhayes/autofish) from Minecraft 1.19.3, retains its singleplayer and multiplayer bite-detection strategies, and supports Aquaculture 2 fishing rods and hooks.

## Requirements

- Minecraft 26.1.2
- NeoForge 26.1.2.71 or newer compatible 26.1.2 release
- Java 25

Aquaculture 2 is optional. Version 2.9.2 is the tested compatibility target for Minecraft 26.1.2.

## Controls

- `F8`: toggle automatic fishing
- `F9`: open the settings screen

Both keys can be changed in Minecraft's Controls screen. Hold a fishing rod, cast once, and enable Auto Fish. The mod reels in and recasts automatically. Persistent mode also casts again when the hook disappears.

The settings screen includes multiplayer sound/motion detection, multi-rod switching, break protection, persistent mode, recast delay, ClearLag chat matching, and fishing while the ESC pause menu is open. The ESC option is enabled by default and only keeps the world running while an eligible fishing flow is active.

## Build

```powershell
.\gradlew.bat build
```

The release JAR is written to `build/libs/autofish-1.2.0.jar`.

## License and attribution

This is a derivative of `troyhayes/autofish`, originally published under GPL-3.0. The port remains licensed under GPL-3.0-only. See `LICENSE` and `NOTICE`.
