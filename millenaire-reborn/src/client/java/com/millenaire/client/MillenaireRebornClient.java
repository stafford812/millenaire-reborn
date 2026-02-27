package com.millenaire.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.millenaire.core.MillenaireReborn;

/**
 * Millenaire Reborn - Client-side Initializer
 * 
 * Handles client-only features:
 * - GUI rendering
 * - Entity renderers
 * - Particle effects
 * - Client-side keybindings
 * 
 * NOTE: No game logic should be here. All state is server-authoritative.
 */
@Environment(EnvType.CLIENT)
public class MillenaireRebornClient implements ClientModInitializer {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(MillenaireReborn.MOD_ID + "-client");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Millenaire Reborn Client");
        
        // TODO: Register entity renderers
        // EntityRendererRegistry.register(MillenaireEntities.VILLAGER, MillenaireVillagerRenderer::new);
        
        // TODO: Register GUI screens
        // ScreenRegistry.register();
        
        // TODO: Register keybindings
        // KeybindingRegistry.register();
        
        LOGGER.info("Millenaire Reborn Client initialized");
    }
}
