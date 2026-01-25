# Zurku Gravestones

A Hytale mod that spawns gravestones when players die. Items are stored in the gravestone so you can retrieve them.

Works with any death penalty setting (none/all/percentage).

## Features

- Gravestone spawns at death location with your items
- Custom or vanilla tombstone model
- Configurable despawn timer
- Per-player gravestone limit
- Owner protection (optional)
- Persistence across server restarts

## Commands

| Command | Description |
|---------|-------------|
| `/gravestone` | Show all settings and commands |
| `/gsmodel` | Toggle between custom/vanilla model |
| `/gstimer <minutes>` | Set despawn timer (0 = disabled) |
| `/gslimit <count>` | Set max gravestones per player (0 = unlimited) |
| `/gsprotection` | Toggle owner-only access |

All commands require OP permissions.

## Install

Drop the jar in `UserData/Mods/` folder.

## Building

Requires Java 21+

```bash
./gradlew build
```

Output: `build/libs/GravestonePlugin-x.x.x.jar`

## Contributing

Pull requests welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT
