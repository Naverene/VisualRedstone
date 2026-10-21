# Visual Redstone

Visual Redstone adds the **Redstone Visualizer**, a hand-held tool that shows how your redstone is wired. Everything below shows only while you hold it (in either hand on 1.12.2 and newer, in your hand on 1.7.10) or wear it in an accessory slot:

- **Highlight:** look at a block that carries a redstone signal (or powered redstone dust) and it gets a bright green box with a translucent, gently pulsing fill and a thick dark border under a bright one, so it stands out against any background. Stronger signals show a brighter green.
- **Power path:** every powered block wired to the one you look at gets a thinner blue box, so you can follow the signal from its source to everything it powers.
- **Signal strength:** redstone dust shows its power level (0 to 15) and every powered block nearby shows the strongest signal reaching it.
- **Repeaters and comparators:** repeaters show their delay in redstone ticks (and, except on 1.7.10, whether they are locked), comparators show whether they compare or subtract.
- **Quasi-connectivity hints:** pistons, dispensers and droppers that are powered only through the block above them but have not noticed yet get an orange box and "QC: fires on next update". Extended pistons that have lost their power without updating show "QC: retracts on next update".

Numbers and labels are drawn through other blocks so they are never hidden, and so are the boxes on every version except 26.3, where they follow the game's own block outline.

### Wearing it

With an accessory mod installed, the visualizer goes in a slot and works exactly as if you were holding it, leaving both hands free. None of these mods is required.

| Version | Mod | Slot |
| --- | --- | --- |
| 1.7.10 | Baubles or Baubles Expanded | Amulet |
| 1.12.2 | Baubles | Charm |
| 1.16.5, 1.18.2, 1.20.1 Forge, 26.3 NeoForge | Curios API | Charm (added for players if no other mod adds it) |
| 1.20.1 Fabric | Trinkets | Necklace (added for players if no other mod adds it) |

### Settings

The client config sets the range (how many blocks around you get numbers and hints, 1 to 16, default 8), the outline thickness, the highlight, path and hint colors (as `#RRGGBB`), and switches each overlay on or off. It lives in the game's `config` folder:

| Version | File |
| --- | --- |
| 1.7.10, 1.12.2 | `vr-client.cfg` (1.12.2 also from the Mods screen) |
| 1.16.5, 1.18.2, 1.20.1 Forge | `vr-client.toml` |
| 1.20.1 Fabric | `vr-client.properties` |
| 26.3 NeoForge | `vr-client.toml` |

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

Each Minecraft folder is its own Gradle project with its own wrapper. Open the folder for the version you want in your IDE (not the repository root), and run `./gradlew build` inside it. The jar lands in that folder's `build/libs` as `visualredstone-<mod version>-<Minecraft version>-<loader>.jar`, where the loader is `forge`, `neoforge` or `fabric`. The Fabric build uses the model, texture, language file and recipe from the Forge folder of the same Minecraft version, so it needs that folder next to it.

## Builds and releases

Builds run on your own machine, so they do not use GitHub Actions minutes. From the repository root:

```
./build-all.sh                  # build every version
./build-all.sh 1.20.1 26.3      # build only these folders
./build-all.sh --check          # also start a server with each jar to catch load crashes (slow)
./build-all.sh --version 1.2.0  # build as mod version 1.2.0
```

The jars are collected in `dist/`. Each version builds on its own Java (see the table above), so set `JAVA_HOME_8`, `JAVA_HOME_17`, `JAVA_HOME_21` and `JAVA_HOME_25` to a JDK of each version; a version whose variable is unset uses your default `JAVA_HOME`. On Windows, run it from Git Bash.

Releases still go through GitHub: pushing a tag such as `v1.2.0` runs [`.github/workflows/build.yml`](.github/workflows/build.yml), which builds every version as mod version 1.2.0, checks each one loads on a server, publishes a GitHub release with all the jars, and uploads each jar to CurseForge when the `CURSEFORGE_TOKEN` secret is set. Pull requests and branch pushes do not run anything on GitHub. The workflow can also be started by hand from the Actions tab to build every version without publishing.

## History

The mod started on Minecraft 1.7.10. A port to 1.16.4 was begun but never finished: its sources were still the 1.7.10 code. Every version here is written against its own Minecraft and loader, following the layout of [More Dyes](https://github.com/Naverene/moredyes).
