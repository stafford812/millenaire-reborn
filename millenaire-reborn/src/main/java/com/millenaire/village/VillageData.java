package com.millenaire.village;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.millenaire.MillenaireMod;

/**
 * VillageData - Complete state of a single village.
 */
public class VillageData {
	
	private static final String NBT_ID = "id";
	private static final String NBT_NAME = "name";
	private static final String NBT_CENTER = "center";
	private static final String NBT_CULTURE = "culture";
	private static final String NBT_RADIUS = "radius";
	private static final String NBT_LEVEL = "level";
	private static final String NBT_RESOURCES = "resources";
	
	private final UUID id;
	private String name;
	private final String cultureId;
	private final BlockPos center;
	private int radius = 64;
	private int level = 1;
	
	private final Map<String, Integer> resources = new HashMap<>();
	private final Set<UUID> npcIds = new HashSet<>();
	
	private transient boolean dirty = false;
	private transient int tickCounter = 0;
	
	public VillageData(UUID id, String name, BlockPos center, String cultureId) {
		this.id = id;
		this.name = name;
		this.center = center;
		this.cultureId = cultureId;
		initializeResources();
	}
	
	private void initializeResources() {
		resources.put("wood", 100);
		resources.put("stone", 50);
		resources.put("food", 200);
		resources.put("gold", 20);
	}
	
	public static VillageData fromNbt(NbtCompound nbt) {
		try {
			UUID id = nbt.getUuid(NBT_ID);
			String name = nbt.getString(NBT_NAME);
			String culture = nbt.getString(NBT_CULTURE);
			
			int[] centerArr = nbt.getIntArray(NBT_CENTER);
			if (centerArr.length != 3) return null;
			BlockPos center = new BlockPos(centerArr[0], centerArr[1], centerArr[2]);
			
			VillageData village = new VillageData(id, name, center, culture);
			village.radius = nbt.getInt(NBT_RADIUS);
			village.level = nbt.getInt(NBT_LEVEL);
			
			NbtCompound resourcesNbt = nbt.getCompound(NBT_RESOURCES);
			for (String key : resourcesNbt.getKeys()) {
				village.resources.put(key, resourcesNbt.getInt(key));
			}
			
			return village;
		} catch (Exception e) {
			MillenaireMod.LOGGER.error("Failed to load village from NBT", e);
			return null;
		}
	}
	
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		
		nbt.putUuid(NBT_ID, id);
		nbt.putString(NBT_NAME, name);
		nbt.putString(NBT_CULTURE, cultureId);
		nbt.putIntArray(NBT_CENTER, new int[] { center.getX(), center.getY(), center.getZ() });
		nbt.putInt(NBT_RADIUS, radius);
		nbt.putInt(NBT_LEVEL, level);
		
		NbtCompound resourcesNbt = new NbtCompound();
		for (Map.Entry<String, Integer> entry : resources.entrySet()) {
			resourcesNbt.putInt(entry.getKey(), entry.getValue());
		}
		nbt.put(NBT_RESOURCES, resourcesNbt);
		
		return nbt;
	}
	
	public void tick() {
		tickCounter++;
		if (tickCounter >= 100) { // Every 5 seconds
			tickCounter = 0;
			processResourceCycle();
		}
	}
	
	private void processResourceCycle() {
		// Basic resource generation
		addResource("wood", level);
		addResource("food", level * 2);
		
		// Food consumption
		consumeResource("food", Math.max(1, npcIds.size()));
	}
	
	public void addResource(String resourceId, int amount) {
		if (amount <= 0) return;
		resources.merge(resourceId, amount, Integer::sum);
		markDirty();
	}
	
	public int consumeResource(String resourceId, int amount) {
		int current = resources.getOrDefault(resourceId, 0);
		int consumed = Math.min(current, amount);
		if (consumed > 0) {
			resources.put(resourceId, current - consumed);
			markDirty();
		}
		return consumed;
	}
	
	public boolean containsPosition(BlockPos pos) {
		return center.getSquaredDistance(pos) <= (radius * radius);
	}
	
	private void markDirty() { this.dirty = true; }
	public boolean isDirty() { return dirty; }
	public void clearDirty() { this.dirty = false; }
	
	// Getters
	public UUID getId() { return id; }
	public String getName() { return name; }
	public BlockPos getCenter() { return center; }
	public String getCultureId() { return cultureId; }
	public int getRadius() { return radius; }
	public int getLevel() { return level; }
	public int getPopulation() { return npcIds.size(); }
	public Map<String, Integer> getResources() { return new HashMap<>(resources); }
}
