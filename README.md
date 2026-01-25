# Zurku Gravestones

A Hytale mod that spawns a gravestone when you die. Your items are stored inside so you can retrieve them.

Compatible with any death penalty setting (none / all / percentage).

## Features

- **Gravestone spawning** – A gravestone appears at your death location containing your items
- **Item retrieval** – Right-click the gravestone to collect your items
- **Despawn timer** – Optionally remove old gravestones after a set time
- **Per-player limit** – Limit how many gravestones each player can have (oldest removed first)
- **Owner protection** – Only the owner can break/collect from their gravestone
- **Custom model** – Includes a custom gravestone model (toggle to vanilla if preferred)

## Commands

| Command | Description |
|---------|-------------|
| `/gs timer <minutes>` | Set despawn timer (0 = never) |
| `/gs limit <count>` | Set max gravestones per player (0 = unlimited) |
| `/gs protection <on\|off>` | Toggle owner-only protection |
| `/gs model` | Toggle between custom and vanilla gravestone model |

## Configuration

Settings are stored in `plugins/Gravestones/settings.json`:

```json
{
  "settingsVersion": 2,
  "useVanillaModel": false,
  "despawnMinutes": 0,
  "maxPerPlayer": 0,
  "ownerProtection": false
}
```

| Setting | Default | Description |
|---------|---------|-------------|
| `useVanillaModel` | `false` | Use vanilla tombstone model instead of custom |
| `despawnMinutes` | `0` | Minutes until gravestones despawn (0 = never) |
| `maxPerPlayer` | `0` | Max gravestones per player (0 = unlimited) |
| `ownerProtection` | `false` | Only owner can break/collect |

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place it in `UserData/Mods/`
3. Start the game

## Building from Source

```bash
./gradlew build
```

Output: `build/libs/GravestonePlugin-*.jar`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT
