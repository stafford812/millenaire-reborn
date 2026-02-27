package com.millenaire.culture;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.millenaire.core.MillenaireReborn;

/**
 * CultureRegistry - Central registry for all village cultures.
 * 
 * Cultures are registered at mod initialization and cannot be modified afterward.
 * Provides methods for selecting cultures based on biome and weighted random.
 */
public final class CultureRegistry {
    
    private static final Map<String, Culture> CULTURES = new HashMap<>();
    private static boolean initialized = false;
    private static int totalWeight = 0;
    
    private CultureRegistry() {} // Prevent instantiation
    
    /**
     * Initialize all built-in cultures.
     * Called once during mod initialization.
     */
    public static void init() {
        if (initialized) {
            MillenaireReborn.LOGGER.warn("CultureRegistry already initialized!");
            return;
        }
        
        MillenaireReborn.LOGGER.info("Initializing CultureRegistry...");
        
        // Register built-in cultures
        registerBuiltinCultures();
        
        initialized = true;
        MillenaireReborn.LOGGER.info("CultureRegistry initialized with {} cultures", CULTURES.size());
    }
    
    /**
     * Register a culture. Must be called during initialization.
     */
    public static void register(Culture culture) {
        if (CULTURES.containsKey(culture.getId())) {
            MillenaireReborn.LOGGER.warn("Culture '{}' already registered, skipping", culture.getId());
            return;
        }
        
        CULTURES.put(culture.getId(), culture);
        totalWeight += culture.getSpawnWeight();
        
        MillenaireReborn.LOGGER.debug("Registered culture: {}", culture);
    }
    
    /**
     * Get a culture by ID.
     */
    public static Optional<Culture> get(String id) {
        return Optional.ofNullable(CULTURES.get(id));
    }
    
    /**
     * Get all registered cultures.
     */
    public static Collection<Culture> getAll() {
        return CULTURES.values();
    }
    
    /**
     * Select a random culture appropriate for the given biome.
     * Uses weighted random selection.
     * 
     * @param biomeId The biome identifier
     * @param seed Random seed for deterministic selection
     * @return Selected culture, or empty if none available
     */
    public static Optional<Culture> selectForBiome(String biomeId, long seed) {
        // Filter cultures that can spawn in this biome
        List<Culture> eligible = new ArrayList<>();
        int eligibleWeight = 0;
        
        for (Culture culture : CULTURES.values()) {
            if (culture.canSpawnInBiome(biomeId)) {
                eligible.add(culture);
                eligibleWeight += culture.getSpawnWeight();
            }
        }
        
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        
        // Weighted random selection
        Random random = new Random(seed);
        int roll = random.nextInt(eligibleWeight);
        int cumulative = 0;
        
        for (Culture culture : eligible) {
            cumulative += culture.getSpawnWeight();
            if (roll < cumulative) {
                return Optional.of(culture);
            }
        }
        
        // Fallback (should never reach)
        return Optional.of(eligible.get(0));
    }
    
    /**
     * Select a random culture using only weighted random (ignore biome).
     */
    public static Optional<Culture> selectRandom(long seed) {
        if (CULTURES.isEmpty()) {
            return Optional.empty();
        }
        
        Random random = new Random(seed);
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        
        for (Culture culture : CULTURES.values()) {
            cumulative += culture.getSpawnWeight();
            if (roll < cumulative) {
                return Optional.of(culture);
            }
        }
        
        return CULTURES.values().stream().findFirst();
    }
    
    // ========== Built-in Cultures ==========
    
    private static void registerBuiltinCultures() {
        // Norman Culture - Medieval European villages
        register(Culture.builder("norman")
            .displayName("Norman")
            .description("Medieval European villages with stone buildings and feudal structure")
            .preferredBiomes(
                "minecraft:plains",
                "minecraft:sunflower_plains",
                "minecraft:forest",
                "minecraft:flower_forest",
                "minecraft:birch_forest",
                "minecraft:meadow"
            )
            .forbiddenBiomes(
                "minecraft:desert",
                "minecraft:badlands",
                "minecraft:jungle",
                "minecraft:snowy_plains"
            )
            .resourceMultiplier("wood", 1.0f)
            .resourceMultiplier("stone", 1.2f)
            .resourceMultiplier("food", 1.0f)
            .resourceMultiplier("gold", 0.8f)
            .buildingTypes(
                "town_hall",
                "house_small",
                "house_medium",
                "house_large",
                "farm",
                "bakery",
                "forge",
                "church",
                "tavern",
                "market",
                "guard_tower"
            )
            .maleNames(
                "Guillaume", "Robert", "Richard", "Henri", "Roger",
                "Geoffroy", "Raoul", "Hugues", "Baudouin", "Thibaut",
                "Pierre", "Jean", "Thomas", "Michel", "Jacques"
            )
            .femaleNames(
                "Mathilde", "Adèle", "Emma", "Béatrice", "Aliénor",
                "Isabelle", "Marie", "Jeanne", "Marguerite", "Catherine",
                "Anne", "Blanche", "Agnès", "Constance", "Éléonore"
            )
            .familyNames(
                "de Hauteville", "FitzRoy", "de Normandie", "le Fort",
                "de Beaumont", "le Brun", "de Mortain", "FitzOsbern",
                "de Gournay", "le Masson", "de Tancarville", "le Roux"
            )
            .villagePrefixes(
                "Saint-", "Mont-", "Beau-", "Château-", "Val-",
                "Font-", "Pont-", "Ville-"
            )
            .villageSuffixes(
                "mont", "ville", "bourg", "court", "val",
                "fontaine", "pierre", "champ"
            )
            .spawnWeight(20)
            .build()
        );
        
        // Japanese Culture - For testing multi-culture support
        register(Culture.builder("japanese")
            .displayName("Japanese")
            .description("Traditional Japanese villages with wooden architecture")
            .preferredBiomes(
                "minecraft:cherry_grove",
                "minecraft:bamboo_jungle",
                "minecraft:forest",
                "minecraft:plains"
            )
            .forbiddenBiomes(
                "minecraft:desert",
                "minecraft:snowy_plains",
                "minecraft:ice_spikes"
            )
            .resourceMultiplier("wood", 1.3f)
            .resourceMultiplier("stone", 0.8f)
            .resourceMultiplier("food", 1.2f)
            .resourceMultiplier("gold", 1.0f)
            .buildingTypes(
                "village_center",
                "house_minka",
                "rice_paddy",
                "shrine",
                "dojo",
                "tea_house",
                "sake_brewery"
            )
            .maleNames(
                "Takeshi", "Hiroshi", "Kenji", "Yuki", "Satoshi",
                "Akira", "Haruto", "Ren", "Sota", "Yuto"
            )
            .femaleNames(
                "Yuki", "Sakura", "Hana", "Akiko", "Michiko",
                "Yoko", "Keiko", "Aiko", "Emi", "Mika"
            )
            .familyNames(
                "Tanaka", "Suzuki", "Yamamoto", "Watanabe", "Sato",
                "Ito", "Nakamura", "Kobayashi", "Yamada", "Kato"
            )
            .villagePrefixes(
                "Shin-", "Nishi-", "Higashi-", "Minami-", "Kita-"
            )
            .villageSuffixes(
                "mura", "cho", "gawa", "yama", "no-sato"
            )
            .spawnWeight(10)
            .build()
        );
    }
}
