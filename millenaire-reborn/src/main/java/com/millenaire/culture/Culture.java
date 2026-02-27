package com.millenaire.culture;

import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.RegistryKey;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.millenaire.core.MillenaireReborn;

/**
 * Culture - Defines a village culture with its unique characteristics.
 * 
 * Each culture has:
 * - Unique building styles and types
 * - Specific NPC professions
 * - Preferred biomes for spawning
 * - Starting resources and bonuses
 * - Name generation rules
 */
public class Culture {
    
    private final String id;
    private final String displayName;
    private final String description;
    
    // Biome preferences for village spawning
    private final List<String> preferredBiomes;
    private final List<String> forbiddenBiomes;
    
    // Starting resources multipliers
    private final Map<String, Float> resourceMultipliers;
    
    // Building types available to this culture
    private final List<String> buildingTypes;
    
    // NPC names for this culture
    private final List<String> maleNames;
    private final List<String> femaleNames;
    private final List<String> familyNames;
    
    // Village naming
    private final List<String> villagePrefixes;
    private final List<String> villageSuffixes;
    
    // Spawn weight (higher = more common)
    private final int spawnWeight;
    
    private Culture(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.preferredBiomes = new ArrayList<>(builder.preferredBiomes);
        this.forbiddenBiomes = new ArrayList<>(builder.forbiddenBiomes);
        this.resourceMultipliers = new HashMap<>(builder.resourceMultipliers);
        this.buildingTypes = new ArrayList<>(builder.buildingTypes);
        this.maleNames = new ArrayList<>(builder.maleNames);
        this.femaleNames = new ArrayList<>(builder.femaleNames);
        this.familyNames = new ArrayList<>(builder.familyNames);
        this.villagePrefixes = new ArrayList<>(builder.villagePrefixes);
        this.villageSuffixes = new ArrayList<>(builder.villageSuffixes);
        this.spawnWeight = builder.spawnWeight;
    }
    
    /**
     * Check if this culture can spawn in the given biome.
     */
    public boolean canSpawnInBiome(String biomeId) {
        // Check forbidden first
        if (forbiddenBiomes.contains(biomeId)) {
            return false;
        }
        // If preferred list is empty, allow all non-forbidden
        if (preferredBiomes.isEmpty()) {
            return true;
        }
        // Otherwise must be in preferred list
        return preferredBiomes.contains(biomeId);
    }
    
    /**
     * Generate a random village name for this culture.
     */
    public String generateVillageName(long seed) {
        if (villagePrefixes.isEmpty() && villageSuffixes.isEmpty()) {
            return id + "_village_" + (seed % 1000);
        }
        
        java.util.Random random = new java.util.Random(seed);
        StringBuilder name = new StringBuilder();
        
        if (!villagePrefixes.isEmpty()) {
            name.append(villagePrefixes.get(random.nextInt(villagePrefixes.size())));
        }
        if (!villageSuffixes.isEmpty()) {
            name.append(villageSuffixes.get(random.nextInt(villageSuffixes.size())));
        }
        
        return name.toString();
    }
    
    /**
     * Generate a random NPC name for this culture.
     */
    public String generateNpcName(long seed, boolean male) {
        java.util.Random random = new java.util.Random(seed);
        List<String> firstNames = male ? maleNames : femaleNames;
        
        if (firstNames.isEmpty()) {
            return (male ? "Man" : "Woman") + "_" + (seed % 100);
        }
        
        String firstName = firstNames.get(random.nextInt(firstNames.size()));
        
        if (!familyNames.isEmpty()) {
            String familyName = familyNames.get(random.nextInt(familyNames.size()));
            return firstName + " " + familyName;
        }
        
        return firstName;
    }
    
    /**
     * Get resource multiplier for this culture.
     */
    public float getResourceMultiplier(String resourceId) {
        return resourceMultipliers.getOrDefault(resourceId, 1.0f);
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getPreferredBiomes() { return new ArrayList<>(preferredBiomes); }
    public List<String> getBuildingTypes() { return new ArrayList<>(buildingTypes); }
    public int getSpawnWeight() { return spawnWeight; }
    
    @Override
    public String toString() {
        return String.format("Culture[%s, weight=%d, biomes=%d]", 
            id, spawnWeight, preferredBiomes.size());
    }
    
    // ========== Builder Pattern ==========
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private List<String> preferredBiomes = new ArrayList<>();
        private List<String> forbiddenBiomes = new ArrayList<>();
        private Map<String, Float> resourceMultipliers = new HashMap<>();
        private List<String> buildingTypes = new ArrayList<>();
        private List<String> maleNames = new ArrayList<>();
        private List<String> femaleNames = new ArrayList<>();
        private List<String> familyNames = new ArrayList<>();
        private List<String> villagePrefixes = new ArrayList<>();
        private List<String> villageSuffixes = new ArrayList<>();
        private int spawnWeight = 10;
        
        public Builder(String id) {
            this.id = id;
            this.displayName = id;
        }
        
        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }
        
        public Builder description(String desc) {
            this.description = desc;
            return this;
        }
        
        public Builder preferredBiomes(String... biomes) {
            this.preferredBiomes.addAll(List.of(biomes));
            return this;
        }
        
        public Builder forbiddenBiomes(String... biomes) {
            this.forbiddenBiomes.addAll(List.of(biomes));
            return this;
        }
        
        public Builder resourceMultiplier(String resource, float multiplier) {
            this.resourceMultipliers.put(resource, multiplier);
            return this;
        }
        
        public Builder buildingTypes(String... types) {
            this.buildingTypes.addAll(List.of(types));
            return this;
        }
        
        public Builder maleNames(String... names) {
            this.maleNames.addAll(List.of(names));
            return this;
        }
        
        public Builder femaleNames(String... names) {
            this.femaleNames.addAll(List.of(names));
            return this;
        }
        
        public Builder familyNames(String... names) {
            this.familyNames.addAll(List.of(names));
            return this;
        }
        
        public Builder villagePrefixes(String... prefixes) {
            this.villagePrefixes.addAll(List.of(prefixes));
            return this;
        }
        
        public Builder villageSuffixes(String... suffixes) {
            this.villageSuffixes.addAll(List.of(suffixes));
            return this;
        }
        
        public Builder spawnWeight(int weight) {
            this.spawnWeight = weight;
            return this;
        }
        
        public Culture build() {
            return new Culture(this);
        }
    }
}
