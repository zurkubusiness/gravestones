# Contributing

Thanks for wanting to help!

## Setup

1. Clone the repo
2. Make sure you have Java 21+ installed
3. Run `./gradlew build` to verify everything works

## Project Structure

```
src/main/java/zurku/gravestones/
├── GravestonePlugin.java      # Main plugin entry point
├── GravestoneManager.java     # Handles placement, persistence, timers
├── GravestoneBlockState.java  # Custom block state with container
├── GravestoneSettings.java    # Settings management
├── DeathListenerSystem.java   # Listens for player deaths
├── BlockStateUtil.java        # Helper for getting block states
├── *Interaction.java          # Custom block interactions
└── commands/                  # Admin commands

src/main/resources/
├── manifest.json              # Plugin manifest
├── Client/                    # Client-side assets (models, textures)
├── Common/                    # Shared assets
└── Server/                    # Server-side item definitions
```

## Making Changes

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make your changes
4. Test by building and running in-game
5. Commit with clear messages
6. Push and open a Pull Request

## Code Style

- Use 4-space indentation
- Keep methods focused and small
- Add comments for non-obvious logic
- Follow existing patterns in the codebase

## Testing

Build and copy to your Hytale mods folder:

```bash
./gradlew build
copy build/libs/GravestonePlugin-*.jar "path/to/Hytale/UserData/Mods/"
```

## Ideas for Contributions

- Bug fixes
- Performance improvements
- New features (with discussion first)
- Documentation improvements
- Code cleanup

## Questions?

Open an issue if you have questions or want to discuss a feature before implementing.
