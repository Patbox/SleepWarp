package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import me.thegiggitybyte.sleepwarp.runnable.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.timeline.Timelines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Handles incrementing time and simulating the world.
 */
public class WarpEngine {
    private static WarpEngine instance;
    private final Random random;

    private WarpEngine() {
        random = new Random();
        ServerTickEvents.END_LEVEL_TICK.register(this::onEndTick);
    }

    public static void initialize() {
        if (instance != null) throw new AssertionError();
        instance = new WarpEngine();
    }

    private void onEndTick(ServerLevel world) {
        // Pre-warp checks.
        if (!world.canSleepThroughNights()) return;

        var totalPlayers = world.players().size();
        var sleepingPlayers = world.players().stream().filter(Player::isSleepingLongEnough).count();
        if (sleepingPlayers == 0) return;

        if (SleepWarpConfig.use_sleep_percentage) {
            var percentRequired = world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            var minimumSleeping = Math.max(1, Mth.ceil((totalPlayers * percentRequired) / 100.0F));
            if (sleepingPlayers < minimumSleeping) return;
        }

        var dayTimelineOpt = world.registryAccess().get(Timelines.OVERWORLD_DAY);
        var clockOpt = world.dimensionType().defaultClock();

        if (dayTimelineOpt.isEmpty() || clockOpt.isEmpty()) {
            return;
        }
        var clockManager = world.clockManager();

        var clock = clockOpt.get();
        var dayTimeline = dayTimelineOpt.orElseThrow().value();
        var dayLengthTicks = dayTimeline.periodTicks().orElse(Integer.MAX_VALUE);

        //var sleepTimeEndTime = clockManager.

        // Determine amount of ticks to add to time.
        var maxTicksAdded = Math.max(10, SleepWarpConfig.max_ticks_added);
        var playerMultiplier = Math.max(0.05, Math.min(1.0, SleepWarpConfig.player_multiplier));
        var worldTime = world.getDefaultClockTime() % dayLengthTicks;
        int warpTickCount;

        if (worldTime + maxTicksAdded < dayLengthTicks) {
            if (totalPlayers == 1) {
                warpTickCount = maxTicksAdded;
            } else {
                var sleepingRatio = (double) sleepingPlayers / totalPlayers;
                var scaledRatio = sleepingRatio * playerMultiplier;
                var tickMultiplier = scaledRatio / ((scaledRatio * 2) - playerMultiplier - sleepingRatio + 1);

                warpTickCount = Math.toIntExact(Math.round(maxTicksAdded * tickMultiplier));
            }
        } else {
            warpTickCount = Math.toIntExact(dayLengthTicks % worldTime);
        }

        // Collect valid chunks to tick.
        var chunkStorage = world.getChunkSource().chunkMap;
        var chunks = new ArrayList<LevelChunk>();

        chunkStorage.forEachReadyToSendChunk(chunk -> {
            if (chunk != null && world.anyPlayerCloseEnoughForSpawning(chunk.getPos()) && chunkStorage.anyPlayerCloseEnoughForSpawning(chunk.getPos())) {
                chunks.add(chunk);
            }
        });

        // Accelerate time and tick world.
        clockManager.addTicks(clock, warpTickCount);

        for (var tick = 0; tick < warpTickCount; tick++) {
            world.advanceWeatherCycle();
            world.updateSkyBrightness();
            if (SleepWarpConfig.tick_game_time) {
                world.tickTime();
            }

            Collections.shuffle(chunks);
            for (var chunk : chunks) {
                if (SleepWarpConfig.tick_random_block) {
                    this.execute(world, new RandomTickRunnable(world, chunk));
                }
                if (world.isRaining()) {
                    if (SleepWarpConfig.tick_lightning && world.isThundering() && random.nextInt(100000) == 0) {
                        this.execute(world, new LightningTickRunnable(world, chunk));
                    }

                    if (random.nextInt(16) == 0) {
                        this.execute(world, new PrecipitationTickRunnable(world, chunk));
                    }
                }
            }

            if (SleepWarpConfig.tick_block_entities) {
                this.execute(world, new BlockTickRunnable(world));
            }
        }

        if (SleepWarpConfig.tick_animals | SleepWarpConfig.tick_monsters) {
            this.execute(world, new MobTickRunnable(world, warpTickCount));
        }

        var oldWorldTime = worldTime;
        worldTime = world.getDefaultClockTime() % dayLengthTicks;
        MutableComponent actionBarText = null;

        if (worldTime == 0 || oldWorldTime + maxTicksAdded >= dayLengthTicks) {
            if (world.isRaining()) world.resetWeatherCycle();
            world.wakeUpAllPlayers();

            var currentDay = String.valueOf(dayTimeline.getPeriodCount(clockManager));
            actionBarText = Component.translatable("text.sleepwarp.day", Component.literal(currentDay).withStyle(ChatFormatting.GOLD));
        } else if (worldTime > 0) {
            var remainingTicks = world.isThundering()
                    ? world.getWeatherData().getThunderTime()
                    : dayLengthTicks - worldTime;

            if (remainingTicks > 0) {
                actionBarText = Component.empty();
                if (totalPlayers > 1) {
                    var requiredPercentage = 1.0 - playerMultiplier;
                    var actualPercentage = (double) sleepingPlayers / totalPlayers;
                    var indicatorColor = actualPercentage >= requiredPercentage ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
                    var playerNoun = (sleepingPlayers == 1 ? "player" : "players");
                    actionBarText.append(Component.translatable("text.sleepwarp." + playerNoun + "_sleeping", Component.literal("⌛ " + sleepingPlayers + ' ').withStyle(indicatorColor)));
                } else {
                    actionBarText.append(Component.literal("⌛").withStyle(ChatFormatting.GOLD));
                }

                var remainingSeconds = Math.round(((double) remainingTicks / warpTickCount) / 20);
                actionBarText.append(CommonComponents.space());
                actionBarText.append(Component.translatable("text.sleepwarp.until_" + (world.isThundering() ? "thunderstorm" : "dawn"), Component.literal(String.valueOf(remainingSeconds))));
            }
        }

        if (SleepWarpConfig.action_bar_messages) {
            for (var player : world.players()) {
                player.sendSystemMessage(actionBarText, true);
            }
        }
    }

    private void execute(ServerLevel world, Runnable runnable) {
        //CompletableFuture.runAsync(runnable);
        world.getServer().execute(runnable);
    }
}