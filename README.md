# Visual Redstone

Visual Redstone adds the **Redstone Visualizer**, a hand-held tool that shows which blocks carry a redstone signal. Hold it in either hand and look at a block: if a redstone signal reaches it (or it is powered redstone dust), the block is highlighted with a bright green box. The box has a translucent, gently pulsing fill, a thick dark border with a bright green border on top so it stands out against any background, and it is drawn through other blocks so it is never hidden. Stronger signals show a brighter green.

Craft the visualizer from a redstone torch, a compass, redstone dust, a clock and a stick:

```
    torch
compass redstone clock
    stick
```

This repository holds every version of the mod, one folder per Minecraft version:

| Folder | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| [1.7.10](1.7.10) | 1.7.10 | Forge (GTNH buildscript) | 8 to play, 21 to build |
| [1.12.2](1.12.2) | 1.12.2 | Forge | 8 |
| [1.16.5](1.16.5) | 1.16.5 | Forge | 8 |
| [1.18.2](1.18.2) | 1.18.2 | Forge | 17 |
| [1.20.1](1.20.1) | 1.20.1 | Forge | 17 |
| [1.20.1-fabric](1.20.1-fabric) | 1.20.1 | Fabric (needs Fabric API) | 17 to play, 21 to build |
| [26.3](26.3) | 26.3 | NeoForge | 25 |

Each Minecraft folder is its own Gradle project with its own wrapper. Open the folder for the version you want in your IDE (not the repository root), and run `./gradlew build` inside it. The jar lands in that folder's `build/libs` as `visualredstone-<mod version>-<Minecraft version>.jar` (with `-fabric` on the end for Fabric). The Fabric build uses the model, texture, language file and recipe from the Forge folder of the same Minecraft version, so it needs that folder next to it.

## Builds and releases

[`.github/workflows/build.yml`](.github/workflows/build.yml) builds each version on its own Java and checks that it loads on a server.

- Pull requests build only the versions they change.
- Every push to the default branch (`main`) builds all versions and replaces the **Development build** pre-release with the newest jar for each one.
- Pushing a tag such as `v1.2.0` builds every version as mod version 1.2.0 and publishes a release with all the jars. Each jar is also uploaded to CurseForge when the `CURSEFORGE_TOKEN` secret and the `CURSEFORGE_PROJECT_ID` variable are set in the repository settings.

## History

The mod started on Minecraft 1.7.10. A port to 1.16.4 was begun but never finished: its sources were still the 1.7.10 code. Every version here is written against its own Minecraft and loader, following the layout of [More Dyes](https://github.com/Naverene/moredyes).
