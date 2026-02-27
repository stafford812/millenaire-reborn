package com.millenaire.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.millenaire.village.VillageManager;

/**
 * Millenaire Reborn - Main Mod Initializer
 * 
 * Server-centric mod implementing autonomous villages with:
 * - Own economy and resource management
 * - NPCs with unique professions
 * - Cultural diversity
 * - Progressive building system
 * 
 * All village data is stored server-side using PersistentState.
 * Client receives only necessary sync data for rendering.
 */
public class MillenaireReborn implements ModInitializer {
    
    public static final String MOD_ID = "millenaire";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Global configuration constants
    public static final int VILLAGE_TICK_INTERVAL = 20; // Update villages every second (20 ticks)
    public static final int VILLAGE_MIN_DISTANCE = 256; // Minimum blocks between village centers
    public static final int VILLAGE_SEARCH_RADIUS = 128; // Search radius for village detection
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Millenaire Reborn for Minecraft 1.21.11");
        
        // Register server lifecycle events
        registerServerEvents();
        
        // Initialize subsystems
        initializeSubsystems();
        
        LOGGER.info("Millenaire Reborn initialized successfully");
    }
    
    /**
     * Register server lifecycle events for village management.
     * VillageManager is attached to each ServerWorld via PersistentState.
     */
    private void registerServerEvents() {
        // Server starting - prepare global registries
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Millenaire: Server starting, preparing registries...");
        });
        
        // Server started - VillageManager initializes per-world on first access
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Millenaire: Server started, village system ready");
            // VillageManager.get(world) will be called lazily when worlds are accessed
        });
        
        // Server stopping - cleanup
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Millenaire: Server stopping, saving village data...");
            // PersistentState automatically saves on server stop
        });
        
        // Server tick - update village simulations
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Only process every VILLAGE_TICK_INTERVAL ticks for performance
            if (server.getTickCount() % VILLAGE_TICK_INTERVAL == 0) {
                server.getWorlds().forEach(world -> {
                    VillageManager manager = VillageManager.get(world);
                    if (manager != null) {
                        manager.tick();
                    }
                });
            }
        });
    }
    
    /**
     * Initialize mod subsystems (cultures, buildings, NPCs, etc.)
     * Called once during mod initialization.
     */
    private void initializeSubsystems() {
        LOGGER.info("Millenaire: Initializing subsystems...");
        
        // TODO: Register cultures
        // CultureRegistry.init();
        
        // TODO: Register building types
        // BuildingRegistry.init();
        
        // TODO: Register NPC types
        // NPCRegistry.init();
        
        // TODO: Register items and blocks
        // MillenaireItems.init();
        // MillenaireBlocks.init();
        
        LOGGER.info("Millenaire: Subsystems initialized");
    }
}
