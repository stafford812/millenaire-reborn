package com.millenaire.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.millenaire.command.MillenaireCommands;
import com.millenaire.culture.CultureRegistry;
import com.millenaire.village.VillageManager;
import com.millenaire.world.WorldEventHandler;

/**
 * Millenaire Reborn - Main Mod Initializer
 * 
 * Server-centric mod implementing autonomous villages with:
 * - Own economy and resource management
 * - NPCs with unique professions
 * - Cultural diversity (Norman, Japanese, etc.)
 * - Progressive building system
 * 
 * All village data is stored server-side using PersistentState.
 * Client receives only necessary sync data for rendering.
 * 
 * @version 1.0.0-alpha.1
 * @since Minecraft 1.21.11
 */
public class MillenaireReborn implements ModInitializer {
    
    public static final String MOD_ID = "millenaire";
    public static final String MOD_VERSION = "1.0.0-alpha.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // ========== Configuration Constants ==========
    
    /** Update villages every second (20 ticks) */
    public static final int VILLAGE_TICK_INTERVAL = 20;
    
    /** Minimum blocks between village centers */
    public static final int VILLAGE_MIN_DISTANCE = 256;
    
    /** Search radius for village detection */
    public static final int VILLAGE_SEARCH_RADIUS = 128;
    
    @Override
    public void onInitialize() {
        LOGGER.info("===========================================");
        LOGGER.info("  Millenaire Reborn v{}", MOD_VERSION);
        LOGGER.info("  Minecraft 1.21.11 - Fabric");
        LOGGER.info("===========================================");
        
        // Initialize subsystems in order
        initializeCultures();
        initializeCommands();
        initializeWorldEvents();
        initializeServerEvents();
        
        LOGGER.info("Millenaire Reborn initialized successfully!");
    }
    
    /**
     * Initialize culture registry with all available cultures.
     */
    private void initializeCultures() {
        LOGGER.info("Loading cultures...");
        CultureRegistry.init();
    }
    
    /**
     * Register server commands.
     */
    private void initializeCommands() {
        LOGGER.info("Registering commands...");
        MillenaireCommands.register();
    }
    
    /**
     * Register world generation events.
     */
    private void initializeWorldEvents() {
        LOGGER.info("Registering world events...");
        WorldEventHandler.register();
    }
    
    /**
     * Register server lifecycle and tick events.
     */
    private void initializeServerEvents() {
        LOGGER.info("Registering server events...");
        
        // Server starting - prepare global state
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Server starting - preparing Millenaire...");
        });
        
        // Server started - ready for play
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started - Millenaire village system active");
            LOGGER.info("Use /millenaire commands to manage villages");
        });
        
        // Server stopping - cleanup
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping - saving village data...");
            WorldEventHandler.clearCache();
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
}
