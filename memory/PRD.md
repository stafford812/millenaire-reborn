# Millenaire Reborn - PRD (Product Requirements Document)

## Original Problem Statement
Перенос мода Millenaire с Minecraft 1.12.2 Forge на Minecraft 1.21.11 Fabric. 
Мод реализует масштабируемую серверную систему автономных деревень с собственной 
экономикой, NPC, культурой и строительством.

## Target Versions
- **Minecraft**: 1.21.11 (unobfuscated)
- **Java**: 21
- **Fabric Loader**: 0.18.1+
- **Fabric API**: 0.139.4+1.21.11
- **Loom**: 1.14.0-alpha.31

## Core Requirements (Static)
1. Server-centric architecture - all data on server
2. PersistentState for village storage
3. NBT serialization for all data
4. markDirty() for change tracking
5. No Forge dependencies
6. No client-only code in server logic
7. Extensible culture system

## Architecture
```
com.millenaire
├── core/              # Mod initialization
├── village/           # Village data and management  
├── npc/               # NPC entities and AI (TODO)
├── building/          # Building system (TODO)
├── culture/           # Culture definitions (TODO)
├── economy/           # Economy system (TODO)
└── client/            # Client-side rendering
```

## What's Been Implemented (Feb 27, 2026)

### Phase 1: Core Infrastructure ✓
- [x] Corrected Gradle build system for 1.21.11
- [x] fabric-loom-no-remap plugin (for unobfuscated MC)
- [x] Proper dependency configuration
- [x] MillenaireReborn.java - main initializer with server events
- [x] MillenaireRebornClient.java - client initializer
- [x] VillageManager.java - PersistentState for village storage
- [x] VillageData.java - complete village state with NBT serialization

### Phase 2: World Integration & Cultures ✓
- [x] Culture.java - culture definition with Builder pattern
- [x] CultureRegistry.java - central registry with biome-weighted selection
- [x] VillageGenerator.java - deterministic chunk-based village spawning
- [x] WorldEventHandler.java - chunk load event handling
- [x] MillenaireCommands.java - admin commands for testing

### Built-in Cultures:
1. **Norman** (weight: 20) - Medieval European, plains/forest biomes
2. **Japanese** (weight: 10) - Traditional Japanese, cherry/bamboo biomes

### Commands:
- `/millenaire spawn <culture>` - Force spawn village at player location
- `/millenaire list` - List all villages
- `/millenaire info` - Show nearest village details
- `/millenaire cultures` - List available cultures
- `/millenaire nearest` - Find nearest village
- `/millenaire tp <index>` - Teleport to village

### Key Features in VillageManager:
- Per-world storage via PersistentStateManager
- Spatial index for fast lookups
- Village creation with distance checks
- Auto-incrementing village IDs

### Key Features in VillageData:
- UUID identification
- Resource management (add/consume/check)
- NPC population tracking
- Building registration
- Player reputation system
- Level-up mechanics
- Complete NBT serialization

### Key Features in VillageGenerator:
- Deterministic spawning based on world seed
- Biome-aware culture selection
- Terrain flatness validation
- Minimum distance enforcement

## Prioritized Backlog

### P0 - Critical
- [ ] Test build on local machine with Java 21
- [ ] Village world generation integration
- [ ] Basic NPC entity registration

### P1 - High Priority  
- [ ] Culture registry system
- [ ] Building type definitions
- [ ] NPC profession system
- [ ] Resource production chains

### P2 - Medium Priority
- [ ] GUI screens for trading
- [ ] Quest system
- [ ] Advanced building construction
- [ ] Multi-culture support

### P3 - Low Priority
- [ ] Particle effects
- [ ] Sound integration
- [ ] Achievements
- [ ] Configuration options

## User Personas
1. **Server Admin** - runs dedicated server, needs stable village system
2. **Single Player** - explores world, interacts with villages
3. **Mod Developer** - extends mod with new cultures

## Issues Fixed from Original Repo
1. Wrong Gradle plugin: `fabric-loom-remap` → `fabric-loom-no-remap`
2. Missing client class referenced in fabric.mod.json
3. Missing mixin JSON files
4. Incorrect package structure (`com.example` → `com.millenaire`)

## Next Steps
1. User tests build locally: `./gradlew build`
2. Run in development: `./gradlew runClient`
3. Implement Phase 2: World generation integration
