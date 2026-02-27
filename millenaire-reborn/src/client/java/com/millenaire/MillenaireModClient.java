package com.millenaire;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Millenaire Reborn - Client-side Initializer
 * 
 * Handles client-only features:
 * - GUI rendering
 * - Entity renderers
 * - Particle effects
 */
@Environment(EnvType.CLIENT)
public class MillenaireModClient implements ClientModInitializer {
	
	@Override
	public void onInitializeClient() {
		MillenaireMod.LOGGER.info("Initializing Millenaire Reborn Client");
		
		// TODO: Register entity renderers
		// TODO: Register GUI screens
		
		MillenaireMod.LOGGER.info("Millenaire Reborn Client initialized");
	}
}
