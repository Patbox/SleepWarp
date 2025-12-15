package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

public class PrecipitationTickRunnable implements Runnable {
    private final ServerLevel world;
    private final LevelChunk chunk;
    
    public PrecipitationTickRunnable(ServerLevel world, LevelChunk chunk) {
        this.world = world;
        this.chunk = chunk;
    }
    
    @Override
    public void run() {
        var randomPos = world.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        var topBlockPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos);
        var biome = world.getBiome(topBlockPos).value();
        
        if (SleepWarpConfig.tick_ice_freezing && biome.shouldFreeze(world, topBlockPos.below())) {
            world.setBlockAndUpdate(topBlockPos.below(), Blocks.ICE.defaultBlockState());
        }
        
        if (SleepWarpConfig.tick_snow_accumulation) {
            var layerHeight = world.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
            if (layerHeight == 0 || !biome.shouldSnow(world, topBlockPos)) return;
            
            var blockState = world.getBlockState(topBlockPos);
            if (blockState.is(Blocks.SNOW)) {
                int snowLayers = blockState.getValue(SnowLayerBlock.LAYERS);
                if (snowLayers < Math.min(layerHeight, 8)) {
                    var layerBlockState = blockState.setValue(SnowLayerBlock.LAYERS, snowLayers + 1);
                    Block.pushEntitiesUp(blockState, layerBlockState, world, topBlockPos);
                    world.setBlockAndUpdate(topBlockPos, layerBlockState);
                }
            } else {
                world.setBlockAndUpdate(topBlockPos, Blocks.SNOW.defaultBlockState());
            }
        }
        
        var precipitation = biome.getPrecipitationAt(topBlockPos.below(), world.getSeaLevel());
        if (precipitation != Biome.Precipitation.NONE) {
            var blockState = world.getBlockState(topBlockPos.below());
            blockState.getBlock().handlePrecipitation(blockState, world, topBlockPos.below(), precipitation);
        }
    }
}
