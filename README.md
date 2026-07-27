# Auto Fish

A configurable client-side automatic fishing mod for Minecraft 26.1.2 and Fabric. This branch ports [troyhayes/autofish](https://github.com/troyhayes/autofish) from Minecraft 1.19.3 and retains its singleplayer and multiplayer bite-detection strategies.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.2+26.1.2
- Java 25

Aquaculture 2 2.9.2 is a NeoForge mod and cannot be installed on Fabric. This branch still recognizes `FishingRodItem` subclasses and items using the common fishing-rod tag.

## Controls

- `F8`: toggle automatic fishing
- `F9`: open the settings screen

Both keys can be changed in Minecraft's Controls screen. Hold a fishing rod, cast once, and enable Auto Fish. The mod reels in and recasts automatically. Persistent mode also casts again when the hook disappears.

The settings screen includes multiplayer sound/motion detection, multi-rod switching, break protection, persistent mode, recast delay, ClearLag chat matching, and fishing while the ESC pause menu is open. The ESC option is enabled by default and only keeps the world running while an eligible fishing flow is active.

## Build

```powershell
.\gradlew.bat build
```

The release JAR is written to `build/libs/autofish-fabric-1.2.0.jar`.

## License and attribution

This is a derivative of `troyhayes/autofish`, originally published under GPL-3.0. The port remains licensed under GPL-3.0-only. See `LICENSE` and `NOTICE`.
