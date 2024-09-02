package me.thegiggitybyte.sleepwarp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var sleepWarpCommand = dispatcher.register(literal("sleepwarp").requires(source -> source.hasPermissionLevel(1)));
            dispatcher.register(literal("sleep").redirect(sleepWarpCommand));
            
            var statusCommand = literal("status").executes(Commands::executeStatusCommand).build();
            sleepWarpCommand.addChild(statusCommand);
        });
    }
    
    private static int executeStatusCommand(CommandContext<ServerCommandSource> ctx) {
        var players = ctx.getSource().getWorld().getPlayers();
        var sleepingCount = 0;
        
        var playerText = Text.empty();
        for (var player : players) {
            playerText.append(Text.literal("[").formatted(Formatting.GRAY));
            
            if (player.isSleeping() && player.getSleepTimer() >= 100) {
                playerText.append(Text.literal("✔ ").append(player.getDisplayName()).formatted(Formatting.DARK_GREEN));
                ++sleepingCount;
            } else
                playerText.append(Text.literal("✖ ").append(player.getDisplayName()).formatted(Formatting.RED));
            
            playerText.append(Text.literal("]").formatted(Formatting.GRAY)).append(" ");
        }
        
        var messageText = Text.empty()
                .append(Text.literal(String.valueOf(sleepingCount)).formatted(Formatting.GRAY))
                .append(" players sleeping: ")
                .append(playerText);
        
        ctx.getSource().sendFeedback(() -> messageText, false);
        return Command.SINGLE_SUCCESS;
    }
}
