/*
 * Copyright (C) 2026 Noelle <noelletrnrghs81@gmail.com>
 *
 * -------------------------------------------------------------------------------
 * EXCLUSION OF THIRD-PARTY REFERENCES & IDENTIFIERS:
 * Identifiers and references to "sras:grand_theatre", "sras:eop_body", and
 * "sras:embryo_of_philosophy" originate from the external project
 * "Star Rail: Apocalyptic Shadow" (https://www.curseforge.com/minecraft/mc-mods/star-rail-apocalyptic-shadow),
 * all rights reserved to LuoShu. These specific identifiers and external references
 * are strictly excluded from the terms of this GPLv2 license and remain the
 * property of their respective copyright holder.
 * -------------------------------------------------------------------------------
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package one_in_10000_embryo_of_philosophy;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(one_in_10000_embryo_of_philosophy.MOD_ID)
@SuppressWarnings("deprecation")
public class one_in_10000_embryo_of_philosophy {
    public static final String MOD_ID = "one_in_10000_embryo_of_philosophy";

    private static final Map<UUID, Integer> EMBRYO_KILL_COUNTS = new ConcurrentHashMap<>();

    private static boolean GRAND_THEATRE_RNG_PAUSED = false;
    private static UUID PENDING_GT_PLAYER_UUID = null;
    private static final Map<UUID, Integer> PENDING_DIM_TRANSITION_TICKS = new ConcurrentHashMap<>();

    public one_in_10000_embryo_of_philosophy() {
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parentScreen) -> new ModConfigScreen(parentScreen))
        );
        MinecraftForge.EVENT_BUS.register(GameEventHandler.class);
    }

    public static class GameEventHandler {

        private static int tickCounter = 0;

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            if (!PENDING_DIM_TRANSITION_TICKS.isEmpty() && event.getServer() != null) {
                for (UUID playerUUID : new ArrayList<>(PENDING_DIM_TRANSITION_TICKS.keySet())) {
                    int remainingTicks = PENDING_DIM_TRANSITION_TICKS.get(playerUUID) - 1;
                    if (remainingTicks <= 0) {
                        PENDING_DIM_TRANSITION_TICKS.remove(playerUUID);
                        ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
                        if (player != null && player.isAlive()) {
                            ServerLevel level = player.serverLevel();
                            triggerSpawnLogic(level, player, "Grand Theatre Exit Trigger");

                            if (ModConfig.DEBUG.get()) {
                                player.sendSystemMessage(Component.literal("§e[Debug] §aExecuted delayed Grand Theatre spawn after dimension transition"));
                            }
                        }
                        PENDING_GT_PLAYER_UUID = null;
                        GRAND_THEATRE_RNG_PAUSED = false;
                    } else {
                        PENDING_DIM_TRANSITION_TICKS.put(playerUUID, remainingTicks);
                    }
                }
            }

            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;

                if (!ModConfig.EMBRYO_OF_PHILOSOPHY.get()) return;

                MinecraftServer server = event.getServer();
                if (server == null) return;

                for (ServerLevel level : server.getAllLevels()) {
                    List<ServerPlayer> playersInDim = level.players();
                    if (playersInDim.isEmpty()) continue;

                    String dimName = level.dimension().location().toString();

                    if (dimName.equals("sras:grand_theatre") && GRAND_THEATRE_RNG_PAUSED) {
                        continue;
                    }

                    ServerPlayer selectedPlayer = playersInDim.get(level.getRandom().nextInt(playersInDim.size()));

                    if (ModConfig.DEBUG.get()) {
                        if (level.getRandom().nextInt(1000) == 0) {
                            selectedPlayer.sendSystemMessage(Component.literal("§e[Debug] §cshitpost rng checked"));
                        }
                    }

                    if (level.getRandom().nextInt(10000) == 0) {
                        if (dimName.equals("sras:grand_theatre")) {
                            GRAND_THEATRE_RNG_PAUSED = true;
                            PENDING_GT_PLAYER_UUID = selectedPlayer.getUUID();

                            if (ModConfig.DEBUG.get()) {
                                selectedPlayer.sendSystemMessage(Component.literal("§e[Debug] §aSuccessful check but not now :3"));
                            }
                        } else {
                            triggerSpawnLogic(level, selectedPlayer, "RNG");
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                String fromDim = event.getFrom().location().toString();

                if (fromDim.equals("sras:grand_theatre") && player.getUUID().equals(PENDING_GT_PLAYER_UUID)) {
                    PENDING_DIM_TRANSITION_TICKS.put(player.getUUID(), 10);

                    if (ModConfig.DEBUG.get()) {
                        System.out.println("[Debug] Tagged player " + player.getScoreboardName() + " left sras:grand_theatre. Triggering delayed spawn in " + event.getTo().location());
                    }
                }
            }
        }

        private static void triggerSpawnLogic(ServerLevel level, ServerPlayer player, String cause) {
            Vec3 look = player.getLookAngle();

            double targetX = player.getX() + (look.x * 50.0);
            double targetY = player.getY() + (look.y * 50.0);
            double targetZ = player.getZ() + (look.z * 50.0);

            if (ModConfig.DEBUG.get()) {
                player.sendSystemMessage(Component.literal("§e[Debug] §aCheck successful (" + cause + ")"));
            }

            ResourceLocation bodyId = ResourceLocation.tryBuild("sras", "eop_body");
            if (bodyId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(bodyId)) {
                EntityType<?> bodyType = BuiltInRegistries.ENTITY_TYPE.get(bodyId);
                Entity bodyEntity = bodyType.create(level);
                if (bodyEntity != null) {
                    bodyEntity.moveTo(targetX, targetY, targetZ, player.getYRot(), player.getXRot());
                    level.addFreshEntity(bodyEntity);
                }
            }

            ResourceLocation embryoId = ResourceLocation.tryBuild("sras", "embryo_of_philosophy");
            if (embryoId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(embryoId)) {
                EntityType<?> embryoType = BuiltInRegistries.ENTITY_TYPE.get(embryoId);
                Entity embryoEntity = embryoType.create(level);
                if (embryoEntity != null) {
                    embryoEntity.moveTo(targetX, targetY + 5.0, targetZ, player.getYRot(), player.getXRot());
                    level.addFreshEntity(embryoEntity);

                    EMBRYO_KILL_COUNTS.put(embryoEntity.getUUID(), 0);

                    CommandSourceStack commandSource = player.createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();
                    player.getServer().getCommands().performPrefixedCommand(
                        commandSource,
                        "kill " + embryoEntity.getUUID().toString()
                    );
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof Player) {
                Entity killer = event.getSource().getEntity();
                if (killer == null) {
                    killer = event.getSource().getDirectEntity();
                }

                if (killer != null && EMBRYO_KILL_COUNTS.containsKey(killer.getUUID())) {
                    int currentKills = EMBRYO_KILL_COUNTS.get(killer.getUUID()) + 1;
                    EMBRYO_KILL_COUNTS.put(killer.getUUID(), currentKills);

                    if (ModConfig.DEBUG.get()) {
                        System.out.println("[Debug] Tracked Embryo " + killer.getUUID() + " kill count: " + currentKills + "/3");
                    }

                    if (currentKills >= 3) {
                        killer.discard();
                        EMBRYO_KILL_COUNTS.remove(killer.getUUID());
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

            java.util.function.Predicate<CommandSourceStack> canExecute = source -> 
                ModConfig.DEBUG.get() || source.hasPermission(2);

            dispatcher.register(
                Commands.literal("killtest")
                    .requires(canExecute)
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerLevel level = source.getLevel();

                        List<Entity> matches = new ArrayList<>();

                        for (Entity entity : level.getAllEntities()) {
                            ResourceLocation registryName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                            if (registryName != null 
                                && registryName.toString().equals("sras:embryo_of_philosophy")
                                && !entity.isRemoved()) {
                                matches.add(entity);
                            }
                        }

                        if (matches.isEmpty()) {
                            source.sendFailure(Component.literal("No 'sras:embryo_of_philosophy' entities found in this dimension."));
                            return 0;
                        }

                        Entity latestEntity = matches.stream()
                            .max(Comparator.comparingInt(Entity::getId))
                            .orElse(matches.get(matches.size() - 1));

                        latestEntity.discard();
                        EMBRYO_KILL_COUNTS.remove(latestEntity.getUUID());

                        source.sendSuccess(
                            () -> Component.literal("§aSuccessfully sent latest 'sras:embryo_of_philosophy' (ID: " + latestEntity.getId() + ") into the shadowrealm :3"),
                            true
                        );

                        return 1;
                    })
            );

            dispatcher.register(
                Commands.literal("testtestthing")
                    .requires(canExecute)
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerLevel level = source.getLevel();
                        List<ServerPlayer> playersInDim = level.players();

                        if (playersInDim.isEmpty()) {
                            source.sendFailure(Component.literal("No players present in this dimension."));
                            return 0;
                        }

                        ServerPlayer selectedPlayer = playersInDim.get(level.getRandom().nextInt(playersInDim.size()));
                        triggerSpawnLogic(level, selectedPlayer, "Command: /testtestthing");

                        source.sendSuccess(
                            () -> Component.literal("§aSuccessfully triggered Embryo of Philosophy spawn check for player " + selectedPlayer.getScoreboardName() + ":3"),
                            true
                        );

                        return 1;
                    })
            );
        }
    }
}
