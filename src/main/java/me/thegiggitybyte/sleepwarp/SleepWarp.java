package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.JsonConfiguration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class SleepWarp implements ModInitializer {
    @Override
    public void onInitialize() {
        JsonConfiguration.getUserInstance();
        Commands.register();
        WarpEngine.initialize();
    }
}
