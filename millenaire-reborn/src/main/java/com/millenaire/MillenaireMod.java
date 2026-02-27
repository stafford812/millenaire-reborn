package com.millenaire;

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
 */
public class MillenaireMod implements ModInitializer {
	
	public static final String MOD_ID = "millenaire";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	// Configuration constants
	public static final int VILLAGE_TICK_INTERVAL = 20; // Update every second
	public static final int VILLAGE_MIN_DISTANCE = 256; // Min blocks between villages
	
	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Millenaire Reborn for Minecraft 1.21.1");
		
		// Server lifecycle events
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Millenaire: Server started, village system ready");
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Millenaire: Server stopping, saving village data...");
		});
		
		// Server tick - update village simulations
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % VILLAGE_TICK_INTERVAL == 0) {
				server.getWorlds().forEach(world -> {
					VillageManager manager = VillageManager.get(world);
					if (manager != null) {
						manager.tick();
					}
				});
			}
		});
		
		LOGGER.info("Millenaire Reborn initialized successfully");
	}
}
