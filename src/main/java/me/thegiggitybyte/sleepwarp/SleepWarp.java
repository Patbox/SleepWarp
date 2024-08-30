package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.fabricmc.api.ModInitializer;

public class SleepWarp implements ModInitializer {
    @Override
    public void onInitialize() {
        SleepWarpConfig.init("sleepwarp", SleepWarpConfig.class);
        Commands.register();
        WarpEngine.initialize();
    }
}
