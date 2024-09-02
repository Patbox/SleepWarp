package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
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
                if (SleepWarpConfig.log_error_messages)
                    e.fillInStackTrace();
            }
        }
    }
}
