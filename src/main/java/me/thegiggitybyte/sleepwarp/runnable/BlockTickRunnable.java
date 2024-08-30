package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.JsonConfiguration;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public class BlockTickRunnable implements Runnable {
    private final ServerWorld world;
    
    public BlockTickRunnable(ServerWorld world) {
        this.world = world;
    }
    
    @Override
    public void run() {
        for (BlockEntityTickInvoker tickInvoker : world.blockEntityTickers) {
            try {
                if (!tickInvoker.isRemoved() && world.shouldTickBlockPos(tickInvoker.getPos())) {
                    tickInvoker.tick();
                }
            } catch (Exception e) {
                if (JsonConfiguration.getUserInstance().getValue("log_error_messages").getAsBoolean())
                    e.fillInStackTrace();
            }
        }
    }
}
