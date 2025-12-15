package me.thegiggitybyte.sleepwarp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class Commands {
    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var sleepWarpCommand = dispatcher.register(literal("sleepwarp").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_MODERATORS)));
            dispatcher.register(literal("sleep").redirect(sleepWarpCommand));
            
            var statusCommand = literal("status").executes(Commands::executeStatusCommand).build();
            sleepWarpCommand.addChild(statusCommand);
        });
    }
    
    private static int executeStatusCommand(CommandContext<CommandSourceStack> ctx) {
        var players = ctx.getSource().getLevel().players();
        var sleepingCount = 0;
        
        var playerText = Component.empty();
        for (var player : players) {
            playerText.append(Component.literal("[").withStyle(ChatFormatting.GRAY));
            
            if (player.isSleeping() && player.getSleepTimer() >= 100) {
                playerText.append(Component.literal("✔ ").append(player.getDisplayName()).withStyle(ChatFormatting.DARK_GREEN));
                ++sleepingCount;
            } else
                playerText.append(Component.literal("✖ ").append(player.getDisplayName()).withStyle(ChatFormatting.RED));
            
            playerText.append(Component.literal("]").withStyle(ChatFormatting.GRAY)).append(" ");
        }
        
        var messageText = Component.empty()
                .append(Component.literal(String.valueOf(sleepingCount)).withStyle(ChatFormatting.GRAY))
                .append(" players sleeping: ")
                .append(playerText);
        
        ctx.getSource().sendSuccess(() -> messageText, false);
        return Command.SINGLE_SUCCESS;
    }
}
