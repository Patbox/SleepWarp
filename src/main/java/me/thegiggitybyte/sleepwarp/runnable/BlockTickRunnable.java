package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

public class BlockTickRunnable implements Runnable {
    private final ServerLevel world;
    
    public BlockTickRunnable(ServerLevel world) {
        this.world = world;
    }
    
    @Override
    public void run() {
        for (TickingBlockEntity tickInvoker : world.blockEntityTickers) {
            try {
                if (!tickInvoker.isRemoved() && world.shouldTickBlocksAt(tickInvoker.getPos())) {
                    tickInvoker.tick();
                }
            } catch (Exception e) {
                if (SleepWarpConfig.log_error_messages)
                    e.fillInStackTrace();
            }
        }
    }
}
