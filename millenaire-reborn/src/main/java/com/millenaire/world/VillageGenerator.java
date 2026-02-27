package com.millenaire.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.biome.Biome;

import java.util.Optional;
import java.util.Random;

import com.millenaire.core.MillenaireReborn;
import com.millenaire.culture.Culture;
import com.millenaire.culture.CultureRegistry;
import com.millenaire.village.VillageData;
import com.millenaire.village.VillageManager;

/**
 * VillageGenerator - Handles village spawning in the world.
 * 
 * Village generation is deterministic based on world seed:
 * - Same seed + same chunk = same village spawn decision
 * - Uses chunk coordinates to create unique seed per location
 * - Respects minimum distance between villages
 * 
 * Generation is checked when chunks are loaded, not during worldgen,
 * to avoid blocking chunk generation.
 */
public final class VillageGenerator {
    
    // Configuration
    private static final int VILLAGE_SPAWN_CHANCE = 150; // 1 in X chunks
    private static final int MIN_Y_LEVEL = 60; // Minimum ground level for village
    private static final int MAX_Y_LEVEL = 200; // Maximum ground level
    private static final int FLATNESS_CHECK_RADIUS = 16; // Blocks to check for flat ground
    private static final int MAX_HEIGHT_VARIANCE = 5; // Maximum height difference allowed
    
    private VillageGenerator() {} // Prevent instantiation
    
    /**
     * Check if a village should spawn in this chunk and create it if so.
     * Called when a chunk is loaded or generated.
     * 
     * @param world The server world
     * @param chunkPos The chunk position
     * @return Created village data, or empty if no village spawned
     */
    public static Optional<VillageData> trySpawnVillage(ServerWorld world, ChunkPos chunkPos) {
        // Only try to spawn in certain chunks based on deterministic hash
        long seed = calculateChunkSeed(world.getSeed(), chunkPos);
        Random random = new Random(seed);
        
        // Check spawn chance
        if (random.nextInt(VILLAGE_SPAWN_CHANCE) != 0) {
            return Optional.empty();
        }
        
        // Get chunk center position
        int centerX = chunkPos.getCenterX();
        int centerZ = chunkPos.getCenterZ();
        
        // Get height at center
        int groundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, centerX, centerZ);
        
        // Check Y level constraints
        if (groundY < MIN_Y_LEVEL || groundY > MAX_Y_LEVEL) {
            return Optional.empty();
        }
        
        BlockPos center = new BlockPos(centerX, groundY, centerZ);
        
        // Check distance from existing villages
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            return Optional.empty();
        }
        
        if (manager.hasVillageNear(center, MillenaireReborn.VILLAGE_MIN_DISTANCE)) {
            return Optional.empty();
        }
        
        // Check terrain flatness
        if (!isTerrainSuitable(world, center)) {
            return Optional.empty();
        }
        
        // Get biome at location
        String biomeId = getBiomeId(world, center);
        
        // Select culture for this biome
        Optional<Culture> cultureOpt = CultureRegistry.selectForBiome(biomeId, seed);
        if (cultureOpt.isEmpty()) {
            MillenaireReborn.LOGGER.debug("No culture available for biome {} at {}", biomeId, center);
            return Optional.empty();
        }
        
        Culture culture = cultureOpt.get();
        
        // Generate village name
        String villageName = culture.generateVillageName(seed);
        
        // Create the village!
        VillageData village = manager.createVillage(center, culture.getId());
        
        if (village != null) {
            MillenaireReborn.LOGGER.info("Generated {} village '{}' at {} in biome {}",
                culture.getDisplayName(), villageName, center, biomeId);
        }
        
        return Optional.ofNullable(village);
    }
    
    /**
     * Calculate deterministic seed for a chunk.
     * Same world seed + chunk pos = same result.
     */
    private static long calculateChunkSeed(long worldSeed, ChunkPos chunkPos) {
        long x = chunkPos.x;
        long z = chunkPos.z;
        // Mix bits to create unique seed per chunk
        return worldSeed ^ (x * 341873128712L) ^ (z * 132897987541L);
    }
    
    /**
     * Check if terrain is suitable for village placement.
     */
    private static boolean isTerrainSuitable(ServerWorld world, BlockPos center) {
        int centerY = center.getY();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        // Sample heights in a grid pattern
        for (int dx = -FLATNESS_CHECK_RADIUS; dx <= FLATNESS_CHECK_RADIUS; dx += 4) {
            for (int dz = -FLATNESS_CHECK_RADIUS; dz <= FLATNESS_CHECK_RADIUS; dz += 4) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                
                // Early exit if too much variance
                if (maxY - minY > MAX_HEIGHT_VARIANCE) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get biome identifier at position.
     */
    private static String getBiomeId(ServerWorld world, BlockPos pos) {
        var biomeEntry = world.getBiome(pos);
        return biomeEntry.getKey()
            .map(key -> key.getValue().toString())
            .orElse("minecraft:plains");
    }
    
    /**
     * Force spawn a village at position (for commands/testing).
     */
    public static Optional<VillageData> forceSpawnVillage(
            ServerWorld world, 
            BlockPos pos, 
            String cultureId) {
        
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            return Optional.empty();
        }
        
        // Validate culture exists
        if (CultureRegistry.get(cultureId).isEmpty()) {
            MillenaireReborn.LOGGER.warn("Unknown culture: {}", cultureId);
            return Optional.empty();
        }
        
        VillageData village = manager.createVillage(pos, cultureId);
        return Optional.ofNullable(village);
    }
}
