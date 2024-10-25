package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

public class PrecipitationTickRunnable implements Runnable {
    private final ServerWorld world;
    private final WorldChunk chunk;
    
    public PrecipitationTickRunnable(ServerWorld world, WorldChunk chunk) {
        this.world = world;
        this.chunk = chunk;
    }
    
    @Override
    public void run() {
        var randomPos = world.getRandomPosInChunk(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(), 15);
        var topBlockPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, randomPos);
        var biome = world.getBiome(topBlockPos).value();
        
        if (SleepWarpConfig.tick_ice_freezing && biome.canSetIce(world, topBlockPos.down())) {
            world.setBlockState(topBlockPos.down(), Blocks.ICE.getDefaultState());
        }
        
        if (SleepWarpConfig.tick_snow_accumulation) {
            var layerHeight = world.getGameRules().getInt(GameRules.SNOW_ACCUMULATION_HEIGHT);
            if (layerHeight == 0 || !biome.canSetSnow(world, topBlockPos)) return;
            
            var blockState = world.getBlockState(topBlockPos);
            if (blockState.isOf(Blocks.SNOW)) {
                int snowLayers = blockState.get(SnowBlock.LAYERS);
                if (snowLayers < Math.min(layerHeight, 8)) {
                    var layerBlockState = blockState.with(SnowBlock.LAYERS, snowLayers + 1);
                    Block.pushEntitiesUpBeforeBlockChange(blockState, layerBlockState, world, topBlockPos);
                    world.setBlockState(topBlockPos, layerBlockState);
                }
            } else {
                world.setBlockState(topBlockPos, Blocks.SNOW.getDefaultState());
            }
        }
        
        var precipitation = biome.getPrecipitation(topBlockPos.down(), world.getSeaLevel());
        if (precipitation != Biome.Precipitation.NONE) {
            var blockState = world.getBlockState(topBlockPos.down());
            blockState.getBlock().precipitationTick(blockState, world, topBlockPos.down(), precipitation);
        }
    }
}
