package com.millenaire.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Collection;

import com.millenaire.core.MillenaireReborn;
import com.millenaire.culture.Culture;
import com.millenaire.culture.CultureRegistry;
import com.millenaire.village.VillageData;
import com.millenaire.village.VillageManager;
import com.millenaire.world.VillageGenerator;

/**
 * MillenaireCommands - Server commands for testing and administration.
 * 
 * Commands:
 * - /millenaire spawn <culture> - Spawn a village at player location
 * - /millenaire list - List all villages in current world
 * - /millenaire info - Show info about nearest village
 * - /millenaire cultures - List available cultures
 * - /millenaire tp <village_name> - Teleport to a village
 */
public final class MillenaireCommands {
    
    private MillenaireCommands() {}
    
    /**
     * Register all commands.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(MillenaireCommands::registerCommands);
        MillenaireReborn.LOGGER.info("Millenaire commands registered");
    }
    
    private static void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        
        dispatcher.register(
            CommandManager.literal("millenaire")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2
                
                // /millenaire spawn <culture>
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.argument("culture", StringArgumentType.word())
                        .executes(MillenaireCommands::spawnVillage)
                    )
                )
                
                // /millenaire list
                .then(CommandManager.literal("list")
                    .executes(MillenaireCommands::listVillages)
                )
                
                // /millenaire info
                .then(CommandManager.literal("info")
                    .executes(MillenaireCommands::villageInfo)
                )
                
                // /millenaire cultures
                .then(CommandManager.literal("cultures")
                    .executes(MillenaireCommands::listCultures)
                )
                
                // /millenaire nearest
                .then(CommandManager.literal("nearest")
                    .executes(MillenaireCommands::nearestVillage)
                )
                
                // /millenaire tp <index>
                .then(CommandManager.literal("tp")
                    .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                        .executes(MillenaireCommands::teleportToVillage)
                    )
                )
        );
    }
    
    /**
     * /millenaire spawn <culture>
     */
    private static int spawnVillage(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String cultureId = StringArgumentType.getString(context, "culture");
        
        // Validate culture
        Optional<Culture> cultureOpt = CultureRegistry.get(cultureId);
        if (cultureOpt.isEmpty()) {
            source.sendError(Text.literal("Unknown culture: " + cultureId));
            source.sendFeedback(() -> Text.literal("Available cultures: " + 
                String.join(", ", CultureRegistry.getAll().stream().map(Culture::getId).toList())
            ), false);
            return 0;
        }
        
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        
        Optional<VillageData> villageOpt = VillageGenerator.forceSpawnVillage(world, pos, cultureId);
        
        if (villageOpt.isPresent()) {
            VillageData village = villageOpt.get();
            source.sendFeedback(() -> Text.literal(
                String.format("Created %s village '%s' at %s", 
                    cultureOpt.get().getDisplayName(),
                    village.getName(),
                    pos.toShortString())
            ), true);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to create village (too close to existing village?)"));
            return 0;
        }
    }
    
    /**
     * /millenaire list
     */
    private static int listVillages(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            source.sendError(Text.literal("VillageManager not available"));
            return 0;
        }
        
        var villages = manager.getAllVillages();
        
        if (villages.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No villages in this world"), false);
            return 1;
        }
        
        source.sendFeedback(() -> Text.literal(
            String.format("=== Villages (%d) ===", villages.size())
        ), false);
        
        int index = 0;
        for (VillageData village : villages.values()) {
            final int idx = index++;
            source.sendFeedback(() -> Text.literal(
                String.format("[%d] %s (%s) - Level %d, Pop: %d, at %s",
                    idx,
                    village.getName(),
                    village.getCultureId(),
                    village.getLevel(),
                    village.getPopulation(),
                    village.getCenter().toShortString())
            ), false);
        }
        
        return 1;
    }
    
    /**
     * /millenaire info
     */
    private static int villageInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            source.sendError(Text.literal("VillageManager not available"));
            return 0;
        }
        
        // Check if player is in a village
        Optional<VillageData> villageOpt = manager.getVillageAt(playerPos);
        
        if (villageOpt.isEmpty()) {
            // Try to find nearest
            villageOpt = manager.getNearestVillage(playerPos, 500);
            if (villageOpt.isEmpty()) {
                source.sendFeedback(() -> Text.literal("No village nearby"), false);
                return 1;
            }
        }
        
        VillageData village = villageOpt.get();
        
        source.sendFeedback(() -> Text.literal("=== Village Info ==="), false);
        source.sendFeedback(() -> Text.literal("Name: " + village.getName()), false);
        source.sendFeedback(() -> Text.literal("Culture: " + village.getCultureId()), false);
        source.sendFeedback(() -> Text.literal("Level: " + village.getLevel()), false);
        source.sendFeedback(() -> Text.literal("Population: " + village.getPopulation()), false);
        source.sendFeedback(() -> Text.literal("Buildings: " + village.getBuildingCount()), false);
        source.sendFeedback(() -> Text.literal("Radius: " + village.getRadius()), false);
        source.sendFeedback(() -> Text.literal("Center: " + village.getCenter().toShortString()), false);
        
        // Resources
        source.sendFeedback(() -> Text.literal("--- Resources ---"), false);
        village.getResources().forEach((res, amt) -> {
            source.sendFeedback(() -> Text.literal("  " + res + ": " + amt), false);
        });
        
        return 1;
    }
    
    /**
     * /millenaire cultures
     */
    private static int listCultures(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Collection<Culture> cultures = CultureRegistry.getAll();
        
        source.sendFeedback(() -> Text.literal(
            String.format("=== Available Cultures (%d) ===", cultures.size())
        ), false);
        
        for (Culture culture : cultures) {
            source.sendFeedback(() -> Text.literal(
                String.format("- %s (%s): %s",
                    culture.getId(),
                    culture.getDisplayName(),
                    culture.getDescription())
            ), false);
        }
        
        return 1;
    }
    
    /**
     * /millenaire nearest
     */
    private static int nearestVillage(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            source.sendError(Text.literal("VillageManager not available"));
            return 0;
        }
        
        Optional<VillageData> villageOpt = manager.getNearestVillage(playerPos, Integer.MAX_VALUE);
        
        if (villageOpt.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No villages exist in this world"), false);
            return 1;
        }
        
        VillageData village = villageOpt.get();
        double distance = Math.sqrt(village.getCenter().getSquaredDistance(playerPos));
        
        source.sendFeedback(() -> Text.literal(
            String.format("Nearest village: %s (%s) at %s (%.0f blocks away)",
                village.getName(),
                village.getCultureId(),
                village.getCenter().toShortString(),
                distance)
        ), false);
        
        return 1;
    }
    
    /**
     * /millenaire tp <index>
     */
    private static int teleportToVillage(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "index");
        ServerWorld world = source.getWorld();
        
        VillageManager manager = VillageManager.get(world);
        if (manager == null) {
            source.sendError(Text.literal("VillageManager not available"));
            return 0;
        }
        
        var villages = manager.getAllVillages().values().toArray(new VillageData[0]);
        
        if (index >= villages.length) {
            source.sendError(Text.literal("Invalid village index. Use /millenaire list to see available villages."));
            return 0;
        }
        
        VillageData village = villages[index];
        BlockPos pos = village.getCenter();
        
        // Teleport
        if (source.getEntity() != null) {
            source.getEntity().teleport(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 
                java.util.Set.of(), source.getEntity().getYaw(), source.getEntity().getPitch(), true);
            source.sendFeedback(() -> Text.literal(
                "Teleported to village: " + village.getName()
            ), true);
            return 1;
        }
        
        source.sendError(Text.literal("Cannot teleport - no entity"));
        return 0;
    }
}
