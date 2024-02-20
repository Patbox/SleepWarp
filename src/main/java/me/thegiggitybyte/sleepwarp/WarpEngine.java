package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.JsonConfiguration;
import me.thegiggitybyte.sleepwarp.runnable.*;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Handles incrementing time and simulating the world.
 */
public class WarpEngine {
    public static final int DAY_LENGTH_TICKS = 24000;
    private static WarpEngine instance;
    private final Random random;
    
    private WarpEngine() {
        random = new Random();
        
        EntitySleepEvents.ALLOW_SLEEP_TIME.register(this::allowSleepTime);
        ServerTickEvents.END_WORLD_TICK.register(this::onEndTick);
    }
    
    public static void initialize() {
        if (instance != null) throw new AssertionError();
        instance = new WarpEngine();
    }
    
    private ActionResult allowSleepTime(PlayerEntity player, BlockPos sleepingPos, boolean vanillaResult) {
        if (!vanillaResult && (player.getWorld().getTimeOfDay() % DAY_LENGTH_TICKS > 12542))
            return ActionResult.SUCCESS;
        else
            return ActionResult.PASS;
    }
    
    private void onEndTick(ServerWorld world) {
        // Pre-warp checks.
        if (!world.isSleepingEnabled()) return;
        
        var totalPlayers = world.getPlayers().size();
        var sleepingPlayers = world.getPlayers().stream().filter(PlayerEntity::canResetTimeBySleeping).count();
        if (sleepingPlayers == 0) return;
        
        if (JsonConfiguration.getUserInstance().getValue("use_sleep_percentage").getAsBoolean()) {
            var percentRequired = world.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            var minimumSleeping = Math.max(1, (totalPlayers * percentRequired) / 100);
            if (sleepingPlayers < minimumSleeping) return;
        }
        
        // Determine amount of ticks to add to time.
        var maxTicksAdded = Math.max(10, JsonConfiguration.getUserInstance().getValue("max_ticks_added").getAsInt());
        var playerMultiplier = Math.max(0.05, Math.min(1.0, JsonConfiguration.getUserInstance().getValue("player_multiplier").getAsDouble()));
        var worldTime = world.getTimeOfDay() % DAY_LENGTH_TICKS;
        int warpTickCount;
        
        if (worldTime + maxTicksAdded < DAY_LENGTH_TICKS) {
            if (totalPlayers == 1) {
                warpTickCount = maxTicksAdded;
            } else {
                var sleepingRatio = (double) sleepingPlayers / totalPlayers;
                var scaledRatio = sleepingRatio * playerMultiplier;
                var tickMultiplier = scaledRatio / ((scaledRatio * 2) - playerMultiplier - sleepingRatio + 1);
                
                warpTickCount = Math.toIntExact(Math.round(maxTicksAdded * tickMultiplier));
            }
        } else {
            warpTickCount = Math.toIntExact(DAY_LENGTH_TICKS % worldTime);
        }
        
        // Collect valid chunks to tick.
        var chunkStorage = world.getChunkManager().threadedAnvilChunkStorage;
        var chunks = new ArrayList<WorldChunk>();
        
        for (ChunkHolder chunkHolder : chunkStorage.entryIterator()) {
            WorldChunk chunk = chunkHolder.getWorldChunk();
            
            if (chunk != null && world.shouldTick(chunk.getPos()) && chunkStorage.shouldTick(chunk.getPos())) {
                chunks.add(chunk);
            }
        }
        
        // Accelerate time and tick world.
        var doDaylightCycle = world.worldProperties.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE);
        
        for (var tick = 0; tick < warpTickCount; tick++) {
            world.tickWeather();
            world.calculateAmbientDarkness();
            if (JsonConfiguration.getUserInstance().getValue("tick_game_time").getAsBoolean()) {
                world.tickTime();
            } else {
                world.setTimeOfDay(world.getTimeOfDay() + 1L);
            }


            var packet = new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), doDaylightCycle);
            world.getServer().getPlayerManager().sendToDimension(packet, world.getRegistryKey());
            
            Collections.shuffle(chunks);
            for (var chunk : chunks) {
                if (JsonConfiguration.getUserInstance().getValue("tick_random_block").getAsBoolean()) {
                    this.execute(world, new RandomTickRunnable(world, chunk));
                }
                if (world.isRaining()) {
                    if (JsonConfiguration.getUserInstance().getValue("tick_lightning").getAsBoolean() && world.isThundering() && random.nextInt(100000) == 0) {
                        this.execute(world, new LightningTickRunnable(world, chunk));
                    }
                    
                    if (random.nextInt(16) == 0) {
                        this.execute(world, new PrecipitationTickRunnable(world, chunk));
                    }
                }
            }
            
            if (JsonConfiguration.getUserInstance().getValue("tick_block_entities").getAsBoolean()) {
                this.execute(world, new BlockTickRunnable(world));
            }
        }
        
        var canTickAnimals = JsonConfiguration.getUserInstance().getValue("tick_animals").getAsBoolean();
        var canTickMonsters = JsonConfiguration.getUserInstance().getValue("tick_monsters").getAsBoolean();
        if (canTickAnimals | canTickMonsters) {
            this.execute(world, new MobTickRunnable(world, warpTickCount));
        }
        
        worldTime = world.getTimeOfDay() % DAY_LENGTH_TICKS;
        MutableText actionBarText = null;
        
        if (worldTime == 0) {
            if (world.isRaining()) world.resetWeather();
            world.wakeSleepingPlayers();
            
            var currentDay = String.valueOf(world.getTimeOfDay() / DAY_LENGTH_TICKS);
            actionBarText = Text.translatable("text.sleepwarp.day", Text.literal(currentDay).formatted(Formatting.GOLD));
        } else if (worldTime > 0) {
            var remainingTicks = world.isThundering()
                    ? world.worldProperties.getThunderTime()
                    : DAY_LENGTH_TICKS - worldTime;
            
            if (remainingTicks > 0) {
                actionBarText = Text.empty();
                if (totalPlayers > 1) {
                    var requiredPercentage = 1.0 - playerMultiplier;
                    var actualPercentage = (double) sleepingPlayers / totalPlayers;
                    var indicatorColor = actualPercentage >= requiredPercentage ? Formatting.DARK_GREEN : Formatting.RED;
                    var playerNoun = (sleepingPlayers == 1 ? "player" : "players");
                    actionBarText.append(Text.translatable("text.sleepwarp." + playerNoun + "_sleeping", Text.literal("⌛ " + sleepingPlayers + ' ').formatted(indicatorColor)));
                } else {
                    actionBarText.append(Text.literal("⌛").formatted(Formatting.GOLD));
                }
                
                var remainingSeconds = Math.round(((double) remainingTicks / warpTickCount) / 20);
                actionBarText.append(ScreenTexts.space());
                actionBarText.append(Text.translatable("text.sleepwarp.until_" + (world.isThundering() ? "thunderstorm" : "dawn"), Text.literal(String.valueOf(remainingSeconds))));
            }
        }
        
        if (JsonConfiguration.getUserInstance().getValue("action_bar_messages").getAsBoolean()) {
            for (var player : world.getPlayers()) {
                player.sendMessage(actionBarText, true);
            }
        }
    }

    private void execute(ServerWorld world, Runnable runnable) {
        //CompletableFuture.runAsync(runnable);
        world.getServer().execute(runnable);
    }
}