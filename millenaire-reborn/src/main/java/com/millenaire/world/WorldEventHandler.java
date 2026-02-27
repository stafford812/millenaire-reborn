package com.millenaire.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

import com.millenaire.core.MillenaireReborn;

/**
 * WorldEventHandler - Handles world events related to village generation.
 * 
 * Registers for chunk load events to check for village spawning.
 * Uses a cache to avoid re-checking already processed chunks.
 */
public final class WorldEventHandler {
    
    // Cache of processed chunks to avoid redundant checks
    // Format: "dimension:chunkX:chunkZ"
    private static final Set<String> processedChunks = new HashSet<>();
    
    // Maximum cache size before cleanup
    private static final int MAX_CACHE_SIZE = 10000;
    
    private WorldEventHandler() {}
    
    /**
     * Register all world-related events.
     * Called during mod initialization.
     */
    public static void register() {
        MillenaireReborn.LOGGER.info("Registering world event handlers...");
        
        // Register chunk load event
        ServerChunkEvents.CHUNK_LOAD.register(WorldEventHandler::onChunkLoad);
        
        MillenaireReborn.LOGGER.info("World event handlers registered");
    }
    
    /**
     * Called when a chunk is loaded.
     * Checks if a village should spawn in this chunk.
     */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        // Only process overworld for now
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }
        
        ChunkPos chunkPos = chunk.getPos();
        String cacheKey = getCacheKey(world, chunkPos);
        
        // Skip if already processed
        if (processedChunks.contains(cacheKey)) {
            return;
        }
        
        // Clean cache if too large
        if (processedChunks.size() > MAX_CACHE_SIZE) {
            processedChunks.clear();
        }
        
        // Mark as processed
        processedChunks.add(cacheKey);
        
        // Try to spawn a village
        VillageGenerator.trySpawnVillage(world, chunkPos);
    }
    
    /**
     * Create cache key for chunk.
     */
    private static String getCacheKey(ServerWorld world, ChunkPos pos) {
        return world.getRegistryKey().getValue() + ":" + pos.x + ":" + pos.z;
    }
    
    /**
     * Clear the processed chunks cache.
     * Called when server stops or world changes.
     */
    public static void clearCache() {
        processedChunks.clear();
    }
}
