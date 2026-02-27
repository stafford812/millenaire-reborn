package com.millenaire.village;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.millenaire.MillenaireMod;

/**
 * VillageManager - Server-side PersistentState for managing all villages in a world.
 * 
 * One VillageManager per ServerWorld (Overworld, Nether, End).
 * All village data stored here, serialized to NBT automatically.
 */
public class VillageManager extends PersistentState {
	
	private static final String NBT_VILLAGES = "villages";
	private static final String NBT_NEXT_ID = "nextVillageId";
	private static final String STORAGE_KEY = MillenaireMod.MOD_ID + "_villages";
	
	private final Map<UUID, VillageData> villages = new HashMap<>();
	private int nextVillageId = 1;
	
	public VillageManager() {
		super();
	}
	
	/**
	 * Get or create VillageManager for a world.
	 */
	public static VillageManager get(ServerWorld world) {
		if (world == null) return null;
		
		return world.getPersistentStateManager().getOrCreate(
			new Type<>(
				VillageManager::new,
				VillageManager::createFromNbt,
				null
			),
			STORAGE_KEY
		);
	}
	
	/**
	 * Create from NBT data.
	 */
	public static VillageManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		VillageManager manager = new VillageManager();
		
		manager.nextVillageId = nbt.getInt(NBT_NEXT_ID);
		
		NbtList villagesList = nbt.getList(NBT_VILLAGES, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < villagesList.size(); i++) {
			NbtCompound villageNbt = villagesList.getCompound(i);
			VillageData village = VillageData.fromNbt(villageNbt);
			if (village != null) {
				manager.villages.put(village.getId(), village);
			}
		}
		
		MillenaireMod.LOGGER.info("Loaded {} villages", manager.villages.size());
		return manager;
	}
	
	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putInt(NBT_NEXT_ID, nextVillageId);
		
		NbtList villagesList = new NbtList();
		for (VillageData village : villages.values()) {
			villagesList.add(village.toNbt());
		}
		nbt.put(NBT_VILLAGES, villagesList);
		
		return nbt;
	}
	
	/**
	 * Process one tick for all villages.
	 */
	public void tick() {
		for (VillageData village : villages.values()) {
			village.tick();
			if (village.isDirty()) {
				markDirty();
				village.clearDirty();
			}
		}
	}
	
	/**
	 * Create a new village.
	 */
	public VillageData createVillage(BlockPos center, String cultureId) {
		if (hasVillageNear(center, MillenaireMod.VILLAGE_MIN_DISTANCE)) {
			return null;
		}
		
		UUID id = UUID.randomUUID();
		String name = cultureId + "_village_" + nextVillageId++;
		
		VillageData village = new VillageData(id, name, center, cultureId);
		villages.put(id, village);
		markDirty();
		
		MillenaireMod.LOGGER.info("Created village '{}' at {}", name, center);
		return village;
	}
	
	/**
	 * Check if any village exists within distance.
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
	 * Get nearest village.
	 */
	public Optional<VillageData> getNearestVillage(BlockPos pos, int maxDistance) {
		VillageData nearest = null;
		double nearestDist = maxDistance * maxDistance;
		
		for (VillageData village : villages.values()) {
			double dist = village.getCenter().getSquaredDistance(pos);
			if (dist < nearestDist) {
				nearestDist = dist;
				nearest = village;
			}
		}
		
		return Optional.ofNullable(nearest);
	}
	
	public Map<UUID, VillageData> getAllVillages() {
		return new HashMap<>(villages);
	}
	
	public int getVillageCount() {
		return villages.size();
	}
}
