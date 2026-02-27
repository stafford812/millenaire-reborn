# Millenaire Reborn

A server-centric Minecraft mod implementing autonomous villages with their own economy, culture, NPCs, and construction.

## Version
- Minecraft: 1.21.11 (Fabric)
- Java: 21
- Fabric Loader: 0.18.1+
- Fabric API: 0.139.4+

## Features (Planned)

### Phase 1: Core Infrastructure ✓
- [x] Project setup with Fabric 1.21.11
- [x] VillageManager (PersistentState)
- [x] VillageData serialization (NBT)
- [ ] World generation integration

### Phase 2: World Integration
- [ ] Village spawn detection
- [ ] Terrain analysis
- [ ] Deterministic generation based on seed

### Phase 3: NPC System
- [ ] Entity registration
- [ ] Professions system
- [ ] AI tasks and goals
- [ ] Village membership

### Phase 4: Economy System
- [ ] Resource types
- [ ] Production/consumption cycles
- [ ] Supply/demand balancing
- [ ] Player trading

### Phase 5: Building System
- [ ] Building types registry
- [ ] Upgrade levels
- [ ] Construction over time
- [ ] Resource requirements

### Phase 6: Simulation Layer
- [ ] ServerTickEvents processing
- [ ] Task scheduling
- [ ] State updates
- [ ] Resource distribution

### Phase 7: Interaction Layer
- [ ] GUI screens
- [ ] Trading interface
- [ ] Quest system
- [ ] Reputation mechanics

## Building

```bash
./gradlew build
```

Output JAR will be in `build/libs/`

## Development

```bash
# Generate sources for IDE
./gradlew genSources

# Run client
./gradlew runClient

# Run dedicated server
./gradlew runServer
```

## Architecture

```
com.millenaire
├── core/              # Mod initialization
├── village/           # Village data and management
├── npc/               # NPC entities and AI
├── building/          # Building system
├── culture/           # Culture definitions
├── economy/           # Economy system
└── client/            # Client-side rendering
```

### Key Design Principles

1. **Server-Authoritative**: All game state lives on the server
2. **PersistentState**: Villages saved via Minecraft's native system
3. **NBT Serialization**: All data structures serialize to NBT
4. **markDirty()**: Changes trigger save on next autosave
5. **Culture-Agnostic Core**: Easy to add new cultures

## License

CC0-1.0
