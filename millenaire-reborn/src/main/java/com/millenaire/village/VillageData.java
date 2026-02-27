package com.millenaire.village;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.millenaire.core.MillenaireReborn;

/**
 * VillageData - Complete state of a single village.
 * 
 * Stores:
 * - Identity: UUID, name, culture
 * - Location: center position, territory radius
 * - Economy: resources, production, consumption
 * - Population: NPC UUIDs, profession counts
 * - Buildings: building positions and types
 * - Progress: development level, reputation with players
 * 
 * All data is serializable to NBT for persistence.
 * Uses dirty flag to signal VillageManager for saving.
 */
public class VillageData {
    
    // NBT Keys
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_CENTER = "center";
    private static final String NBT_CULTURE = "culture";
    private static final String NBT_RADIUS = "radius";
    private static final String NBT_LEVEL = "level";
    private static final String NBT_RESOURCES = "resources";
    private static final String NBT_NPCS = "npcs";
    private static final String NBT_BUILDINGS = "buildings";
    private static final String NBT_CREATED_TIME = "createdTime";
    private static final String NBT_PLAYER_REPUTATION = "playerReputation";
    
    // Default values
    private static final int DEFAULT_RADIUS = 64;
    private static final int DEFAULT_LEVEL = 1;
    private static final int MAX_LEVEL = 10;
    
    // Identity
    private final UUID id;
    private String name;
    private final String cultureId;
    
    // Location
    private final BlockPos center;
    private int radius;
    
    // Development
    private int level;
    private long createdTime;
    
    // Economy - resource ID -> amount
    private final Map<String, Integer> resources = new HashMap<>();
    
    // Population - NPC UUIDs
    private final Set<UUID> npcIds = new HashSet<>();
    
    // Buildings - position hash -> building type
    private final Map<Long, String> buildings = new HashMap<>();
    
    // Player relations - player UUID -> reputation
    private final Map<UUID, Integer> playerReputation = new HashMap<>();
    
    // State management (transient, not serialized)
    private transient boolean dirty = false;
    private transient int ticksSinceLastUpdate = 0;
    
    /**
     * Create a new village.
     */
    public VillageData(UUID id, String name, BlockPos center, String cultureId) {
        this.id = id;
        this.name = name;
        this.center = center;
        this.cultureId = cultureId;
        this.radius = DEFAULT_RADIUS;
        this.level = DEFAULT_LEVEL;
        this.createdTime = System.currentTimeMillis();
        
        // Initialize starting resources based on culture
        initializeResources();
    }
    
    /**
     * Deserialize from NBT.
     */
    public static VillageData fromNbt(NbtCompound nbt) {
        try {
            UUID id = nbt.getUuid(NBT_ID);
            String name = nbt.getString(NBT_NAME);
            String culture = nbt.getString(NBT_CULTURE);
            
            // Read center position
            int[] centerArr = nbt.getIntArray(NBT_CENTER);
            if (centerArr.length != 3) {
                MillenaireReborn.LOGGER.error("Invalid village center data for {}", id);
                return null;
            }
            BlockPos center = new BlockPos(centerArr[0], centerArr[1], centerArr[2]);
            
            VillageData village = new VillageData(id, name, center, culture);
            village.radius = nbt.getInt(NBT_RADIUS);
            village.level = nbt.getInt(NBT_LEVEL);
            village.createdTime = nbt.getLong(NBT_CREATED_TIME);
            
            // Load resources
            NbtCompound resourcesNbt = nbt.getCompound(NBT_RESOURCES);
            for (String key : resourcesNbt.getKeys()) {
                village.resources.put(key, resourcesNbt.getInt(key));
            }
            
            // Load NPC IDs
            if (nbt.contains(NBT_NPCS, NbtElement.INT_ARRAY_TYPE)) {
                long[] npcArray = nbt.getLongArray(NBT_NPCS);
                for (int i = 0; i < npcArray.length; i += 2) {
                    if (i + 1 < npcArray.length) {
                        village.npcIds.add(new UUID(npcArray[i], npcArray[i + 1]));
                    }
                }
            }
            
            // Load buildings
            NbtCompound buildingsNbt = nbt.getCompound(NBT_BUILDINGS);
            for (String key : buildingsNbt.getKeys()) {
                try {
                    village.buildings.put(Long.parseLong(key), buildingsNbt.getString(key));
                } catch (NumberFormatException e) {
                    MillenaireReborn.LOGGER.warn("Invalid building position key: {}", key);
                }
            }
            
            // Load player reputation
            NbtCompound repNbt = nbt.getCompound(NBT_PLAYER_REPUTATION);
            for (String key : repNbt.getKeys()) {
                try {
                    village.playerReputation.put(UUID.fromString(key), repNbt.getInt(key));
                } catch (IllegalArgumentException e) {
                    MillenaireReborn.LOGGER.warn("Invalid player UUID in reputation: {}", key);
                }
            }
            
            return village;
            
        } catch (Exception e) {
            MillenaireReborn.LOGGER.error("Failed to load village from NBT", e);
            return null;
        }
    }
    
    /**
     * Serialize to NBT.
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putUuid(NBT_ID, id);
        nbt.putString(NBT_NAME, name);
        nbt.putString(NBT_CULTURE, cultureId);
        nbt.putIntArray(NBT_CENTER, new int[] { center.getX(), center.getY(), center.getZ() });
        nbt.putInt(NBT_RADIUS, radius);
        nbt.putInt(NBT_LEVEL, level);
        nbt.putLong(NBT_CREATED_TIME, createdTime);
        
        // Save resources
        NbtCompound resourcesNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            resourcesNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put(NBT_RESOURCES, resourcesNbt);
        
        // Save NPC IDs as long array (UUID = 2 longs)
        long[] npcArray = new long[npcIds.size() * 2];
        int idx = 0;
        for (UUID npcId : npcIds) {
            npcArray[idx++] = npcId.getMostSignificantBits();
            npcArray[idx++] = npcId.getLeastSignificantBits();
        }
        nbt.putLongArray(NBT_NPCS, npcArray);
        
        // Save buildings
        NbtCompound buildingsNbt = new NbtCompound();
        for (Map.Entry<Long, String> entry : buildings.entrySet()) {
            buildingsNbt.putString(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(NBT_BUILDINGS, buildingsNbt);
        
        // Save player reputation
        NbtCompound repNbt = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : playerReputation.entrySet()) {
            repNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(NBT_PLAYER_REPUTATION, repNbt);
        
        return nbt;
    }
    
    /**
     * Process one tick of village simulation.
     */
    public void tick(ServerWorld world) {
        ticksSinceLastUpdate++;
        
        // Slow tick - resource production/consumption (every 100 ticks = 5 seconds)
        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;
            processResourceCycle();
            checkLevelUp();
        }
        
        // TODO: NPC task assignment
        // TODO: Building progress
        // TODO: Trade opportunity generation
    }
    
    /**
     * Check if position is within village territory.
     */
    public boolean containsPosition(BlockPos pos) {
        double distSq = center.getSquaredDistance(pos);
        return distSq <= (radius * radius);
    }
    
    // ========== Resource Management ==========
    
    /**
     * Initialize starting resources for new village.
     */
    private void initializeResources() {
        // Basic starting resources for all cultures
        resources.put("wood", 100);
        resources.put("stone", 50);
        resources.put("food", 200);
        resources.put("gold", 20);
        
        // TODO: Culture-specific starting resources
    }
    
    /**
     * Process resource production and consumption.
     */
    private void processResourceCycle() {
        // Food consumption based on population
        int foodConsumption = Math.max(1, npcIds.size());
        consumeResource("food", foodConsumption);
        
        // Basic resource generation based on level
        addResource("wood", level);
        addResource("stone", level / 2);
        addResource("food", level * 2);
        
        // TODO: Building-based production
        // TODO: NPC profession-based production
    }
    
    /**
     * Add resource to village.
     */
    public void addResource(String resourceId, int amount) {
        if (amount <= 0) return;
        resources.merge(resourceId, amount, Integer::sum);
        markDirty();
    }
    
    /**
     * Consume resource from village.
     * @return Actual amount consumed (may be less if insufficient)
     */
    public int consumeResource(String resourceId, int amount) {
        int current = resources.getOrDefault(resourceId, 0);
        int consumed = Math.min(current, amount);
        if (consumed > 0) {
            resources.put(resourceId, current - consumed);
            markDirty();
        }
        return consumed;
    }
    
    /**
     * Check if village has at least specified amount of resource.
     */
    public boolean hasResource(String resourceId, int amount) {
        return resources.getOrDefault(resourceId, 0) >= amount;
    }
    
    /**
     * Get amount of a resource.
     */
    public int getResourceAmount(String resourceId) {
        return resources.getOrDefault(resourceId, 0);
    }
    
    // ========== Development ==========
    
    /**
     * Check and process level up conditions.
     */
    private void checkLevelUp() {
        if (level >= MAX_LEVEL) return;
        
        // Level up requirements (simplified)
        int requiredPopulation = level * 2;
        int requiredBuildings = level;
        int requiredGold = level * 50;
        
        if (npcIds.size() >= requiredPopulation 
            && buildings.size() >= requiredBuildings
            && hasResource("gold", requiredGold)) {
            
            consumeResource("gold", requiredGold);
            level++;
            radius += 16; // Expand territory
            markDirty();
            
            MillenaireReborn.LOGGER.info("Village '{}' leveled up to level {}", name, level);
        }
    }
    
    // ========== NPC Management ==========
    
    /**
     * Register an NPC as belonging to this village.
     */
    public void addNpc(UUID npcId) {
        if (npcIds.add(npcId)) {
            markDirty();
        }
    }
    
    /**
     * Remove NPC from village.
     */
    public void removeNpc(UUID npcId) {
        if (npcIds.remove(npcId)) {
            markDirty();
        }
    }
    
    /**
     * Get all NPCs in this village.
     */
    public Set<UUID> getNpcIds() {
        return new HashSet<>(npcIds);
    }
    
    // ========== Building Management ==========
    
    /**
     * Register a building at position.
     */
    public void addBuilding(BlockPos pos, String buildingType) {
        long key = posToLong(pos);
        buildings.put(key, buildingType);
        markDirty();
    }
    
    /**
     * Remove building at position.
     */
    public void removeBuilding(BlockPos pos) {
        long key = posToLong(pos);
        if (buildings.remove(key) != null) {
            markDirty();
        }
    }
    
    /**
     * Get building type at position.
     */
    public String getBuildingAt(BlockPos pos) {
        return buildings.get(posToLong(pos));
    }
    
    private long posToLong(BlockPos pos) {
        return ((long)pos.getX() << 40) | ((long)(pos.getY() & 0xFFF) << 28) | (pos.getZ() & 0xFFFFFFF);
    }
    
    // ========== Player Reputation ==========
    
    /**
     * Get player's reputation with this village.
     */
    public int getReputation(UUID playerId) {
        return playerReputation.getOrDefault(playerId, 0);
    }
    
    /**
     * Modify player's reputation.
     */
    public void modifyReputation(UUID playerId, int delta) {
        int current = playerReputation.getOrDefault(playerId, 0);
        int newValue = Math.max(-1000, Math.min(1000, current + delta));
        playerReputation.put(playerId, newValue);
        markDirty();
    }
    
    // ========== Getters ==========
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public BlockPos getCenter() { return center; }
    public String getCultureId() { return cultureId; }
    public int getRadius() { return radius; }
    public int getLevel() { return level; }
    public long getCreatedTime() { return createdTime; }
    public int getPopulation() { return npcIds.size(); }
    public int getBuildingCount() { return buildings.size(); }
    public Map<String, Integer> getResources() { return new HashMap<>(resources); }
    
    // ========== Dirty Flag Management ==========
    
    private void markDirty() {
        this.dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void clearDirty() {
        this.dirty = false;
    }
    
    @Override
    public String toString() {
        return String.format("Village[%s, culture=%s, level=%d, pop=%d, pos=%s]",
            name, cultureId, level, npcIds.size(), center);
    }
}
