package me.thegiggitybyte.sleepwarp.mixin.common;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canSkipNight(I)Z"))
    private boolean suppressVanillaSleep(SleepManager instance, int percentage) {
        return false;
    }
    
    @Redirect(method = "updateSleepingPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;sendSleepingStatus()V"))
    private void sendWarpStatus(ServerWorld world) {
        if (world.getServer().isSingleplayer() || !world.getServer().isRemote() || world.getPlayers().size() == 1) return;
        if (!SleepWarpConfig.action_bar_messages) return;
        
        long playerCount = 0, inBedCount = 0, sleepingCount = 0;
        
        for (var player : world.getPlayers()) {
            if (player.isSleeping()) {
                if (player.getSleepTimer() >= 100) ++sleepingCount;
                ++inBedCount;
            }
            
            ++playerCount;
        }
        
        Text messageText = null;
        var tallyText = Text.empty()
                .append(Text.literal(String.valueOf(inBedCount)))
                .append("/")
                .append(Text.literal(String.valueOf(playerCount)));
        
        if (inBedCount == 0) {
            messageText = Text.translatable("text.sleepwarp.players_sleeping", tallyText.formatted(Formatting.DARK_GRAY));
        } else if (SleepWarpConfig.use_sleep_percentage) {
            var percentRequired = world.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            var minSleepingCount = Math.max(1, (playerCount * percentRequired) / 100);
            
            if (sleepingCount < minSleepingCount && minSleepingCount - inBedCount > 0) {
                messageText = Text.translatable("text.sleepwarp.players_sleeping.more_required", tallyText.formatted(Formatting.RED), String.valueOf((minSleepingCount - inBedCount)));
            } else {
                messageText = Text.translatable("text.sleepwarp.players_sleeping", tallyText.formatted(Formatting.DARK_GREEN));
            }
        } else if (sleepingCount == 0) {
            messageText = Text.translatable("text.sleepwarp.players_sleeping", tallyText.formatted(Formatting.YELLOW));
        }
        
        if (messageText != null) {
            for (var player : world.getPlayers()) {
                player.sendMessage(messageText, true);
            }
        }
    }
}
