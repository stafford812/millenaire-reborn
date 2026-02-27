package com.millenaire.village;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.millenaire.core.MillenaireReborn;

/**
 * VillageManager - Server-side PersistentState for managing all villages in a world.
 * 
 * Architecture:
 * - One VillageManager per ServerWorld (Overworld, Nether, End, custom dimensions)
 * - All village data stored here, serialized to NBT automatically
 * - Uses markDirty() to signal Minecraft to save state
 * - Thread-safe access via ServerWorld's getPersistentStateManager()
 * 
 * Data Flow:
 * 1. Server starts -> VillageManager loaded from NBT or created fresh
 * 2. Villages tick -> state changes -> markDirty() called
 * 3. Server stops/autosaves -> writeNbt() serializes to disk
 * 4. Server restarts -> readNbt() deserializes from disk
 */
public class VillageManager extends PersistentState {
    
    // NBT Keys - centralized to avoid magic strings
    private static final String NBT_VILLAGES = "villages";
    private static final String NBT_NEXT_ID = "nextVillageId";
    private static final String NBT_WORLD_DIMENSION = "worldDimension";
    
    // Storage key for PersistentState
    private static final String STORAGE_KEY = MillenaireReborn.MOD_ID + "_villages";
    
    // Village storage: UUID -> VillageData
    private final Map<UUID, VillageData> villages = new HashMap<>();
    
    // Spatial index: ChunkPos-like key -> VillageData for fast lookups
    private final Map<Long, VillageData> villagesByChunk = new HashMap<>();
    
    // Auto-incrementing ID for human-readable village names
    private int nextVillageId = 1;
    
    // Reference to the world (transient, not serialized)
    private transient ServerWorld world;
    private String worldDimension;
    
    // Tick counter for performance management
    private int tickCounter = 0;
    
    /**
     * Private constructor - use get() factory method
     */
    private VillageManager() {
        super();
    }
    
    /**
     * Factory method to get or create VillageManager for a world.
     * Uses Minecraft's PersistentStateManager for lifecycle management.
     * 
     * @param world The ServerWorld to get manager for
     * @return VillageManager instance (never null for valid world)
     */
    public static VillageManager get(ServerWorld world) {
        if (world == null) {
            MillenaireReborn.LOGGER.error("Cannot get VillageManager for null world");
            return null;
        }
        
        return world.getPersistentStateManager().getOrCreate(
            new Type<>(
                VillageManager::new,
                VillageManager::readNbt,
                null // DataFixTypes - null for new mods
            ),
            STORAGE_KEY
        );
    }
    
    /**
     * Set world reference after loading from NBT.
     * Called by PersistentStateManager.
     */
    public void setWorld(ServerWorld world) {
        this.world = world;
        this.worldDimension = world.getRegistryKey().getValue().toString();
        
        // Rebuild spatial index after loading
        rebuildSpatialIndex();
    }
    
    /**
     * Deserialize VillageManager from NBT.
     * Called when world loads.
     */
    public static VillageManager readNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        VillageManager manager = new VillageManager();
        
        manager.nextVillageId = nbt.getInt(NBT_NEXT_ID);
        manager.worldDimension = nbt.getString(NBT_WORLD_DIMENSION);
        
        // Load villages
        NbtList villagesList = nbt.getList(NBT_VILLAGES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < villagesList.size(); i++) {
            NbtCompound villageNbt = villagesList.getCompound(i);
            VillageData village = VillageData.fromNbt(villageNbt);
            if (village != null) {
                manager.villages.put(village.getId(), village);
            }
        }
        
        MillenaireReborn.LOGGER.info("Loaded {} villages for dimension {}", 
            manager.villages.size(), manager.worldDimension);
        
        return manager;
    }
    
    /**
     * Serialize VillageManager to NBT.
     * Called on world save/autosave.
     */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        nbt.putInt(NBT_NEXT_ID, nextVillageId);
        nbt.putString(NBT_WORLD_DIMENSION, worldDimension != null ? worldDimension : "");
        
        // Save villages
        NbtList villagesList = new NbtList();
        for (VillageData village : villages.values()) {
            villagesList.add(village.toNbt());
        }
        nbt.put(NBT_VILLAGES, villagesList);
        
        MillenaireReborn.LOGGER.debug("Saved {} villages for dimension {}", 
            villages.size(), worldDimension);
        
        return nbt;
    }
    
    /**
     * Process one tick for all villages.
     * Called from server tick event.
     */
    public void tick() {
        tickCounter++;
        
        for (VillageData village : villages.values()) {
            village.tick(world);
            
            // Check if village needs to mark manager dirty
            if (village.isDirty()) {
                markDirty();
                village.clearDirty();
            }
        }
    }
    
    /**
     * Create a new village at the specified position.
     * 
     * @param center Village center position
     * @param cultureId Culture identifier (e.g., "norman", "japanese")
     * @return Created village, or null if position invalid
     */
    public VillageData createVillage(BlockPos center, String cultureId) {
        // Check minimum distance from existing villages
        if (hasVillageNear(center, MillenaireReborn.VILLAGE_MIN_DISTANCE)) {
            MillenaireReborn.LOGGER.warn("Cannot create village at {} - too close to existing village", center);
            return null;
        }
        
        UUID id = UUID.randomUUID();
        String name = generateVillageName(cultureId);
        
        VillageData village = new VillageData(id, name, center, cultureId);
        villages.put(id, village);
        
        // Update spatial index
        updateSpatialIndex(village);
        
        markDirty();
        
        MillenaireReborn.LOGGER.info("Created village '{}' ({}) at {} in {}", 
            name, cultureId, center, worldDimension);
        
        return village;
    }
    
    /**
     * Remove a village by ID.
     */
    public boolean removeVillage(UUID id) {
        VillageData village = villages.remove(id);
        if (village != null) {
            removeSpatialIndex(village);
            markDirty();
            MillenaireReborn.LOGGER.info("Removed village '{}' ({})", village.getName(), id);
            return true;
        }
        return false;
    }
    
    /**
     * Get village by UUID.
     */
    public Optional<VillageData> getVillage(UUID id) {
        return Optional.ofNullable(villages.get(id));
    }
    
    /**
     * Get village containing the specified position.
     */
    public Optional<VillageData> getVillageAt(BlockPos pos) {
        // First check spatial index for quick lookup
        long chunkKey = getChunkKey(pos);
        VillageData cached = villagesByChunk.get(chunkKey);
        if (cached != null && cached.containsPosition(pos)) {
            return Optional.of(cached);
        }
        
        // Fallback to linear search
        for (VillageData village : villages.values()) {
            if (village.containsPosition(pos)) {
                return Optional.of(village);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Find nearest village to a position.
     */
    public Optional<VillageData> getNearestVillage(BlockPos pos, int maxDistance) {
        VillageData nearest = null;
        double nearestDist = maxDistance * maxDistance; // Use squared distance
        
        for (VillageData village : villages.values()) {
            double dist = village.getCenter().getSquaredDistance(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = village;
            }
        }
        
        return Optional.ofNullable(nearest);
    }
    
    /**
     * Check if any village exists within distance of position.
     */
    public boolean hasVillageNear(BlockPos pos, int distance) {
        int distSq = distance * distance;
        for (VillageData village : villages.values()) {
            if (village.getCenter().getSquaredDistance(pos) < distSq) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all villages in this world.
     */
    public Map<UUID, VillageData> getAllVillages() {
        return new HashMap<>(villages); // Return copy for safety
    }
    
    /**
     * Get total village count.
     */
    public int getVillageCount() {
        return villages.size();
    }
    
    // ========== Private Helper Methods ==========
    
    private String generateVillageName(String cultureId) {
        // TODO: Generate culture-specific names from name lists
        return String.format("%s_village_%d", cultureId, nextVillageId++);
    }
    
    private long getChunkKey(BlockPos pos) {
        return ((long)(pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
    }
    
    private void rebuildSpatialIndex() {
        villagesByChunk.clear();
        for (VillageData village : villages.values()) {
            updateSpatialIndex(village);
        }
    }
    
    private void updateSpatialIndex(VillageData village) {
        long key = getChunkKey(village.getCenter());
        villagesByChunk.put(key, village);
    }
    
    private void removeSpatialIndex(VillageData village) {
        long key = getChunkKey(village.getCenter());
        villagesByChunk.remove(key);
    }
}
