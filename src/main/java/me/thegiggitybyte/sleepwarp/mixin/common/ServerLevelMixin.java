package me.thegiggitybyte.sleepwarp.mixin.common;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

    protected ServerLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
    private boolean suppressVanillaSleep(SleepStatus instance, int percentage) {
        return false;
    }
    
    @Redirect(method = "updateSleepingPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;announceSleepStatus()V"))
    private void sendWarpStatus(ServerLevel world) {
        if (world.getServer().isSingleplayer() || !world.getServer().isPublished() || world.players().size() == 1) return;
        if (!SleepWarpConfig.action_bar_messages) return;
        
        long playerCount = 0, inBedCount = 0, sleepingCount = 0;
        
        for (var player : world.players()) {
            if (player.isSleeping()) {
                if (player.getSleepTimer() >= 100) ++sleepingCount;
                ++inBedCount;
            }
            
            ++playerCount;
        }
        
        Component messageText = null;
        var tallyText = Component.empty()
                .append(Component.literal(String.valueOf(inBedCount)))
                .append("/")
                .append(Component.literal(String.valueOf(playerCount)));
        
        if (inBedCount == 0) {
            messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.DARK_GRAY));
        } else if (SleepWarpConfig.use_sleep_percentage) {
            var percentRequired = world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            var minSleepingCount = Math.max(1, Mth.ceil((playerCount * percentRequired) / 100.0F));
            
            if (sleepingCount < minSleepingCount && minSleepingCount - inBedCount > 0) {
                messageText = Component.translatable("text.sleepwarp.players_sleeping.more_required", tallyText.withStyle(ChatFormatting.RED), String.valueOf((minSleepingCount - inBedCount)));
            } else {
                messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.DARK_GREEN));
            }
        } else if (sleepingCount == 0) {
            messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.YELLOW));
        }
        
        if (messageText != null) {
            for (var player : world.players()) {
                player.displayClientMessage(messageText, true);
            }
        }
    }
}
