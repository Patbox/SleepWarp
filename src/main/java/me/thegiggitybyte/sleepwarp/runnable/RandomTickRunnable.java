package me.thegiggitybyte.sleepwarp.runnable;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;

public class RandomTickRunnable implements Runnable {
    private final ServerLevel world;
    private final LevelChunk chunk;
    private RandomSource random;
    
    public  RandomTickRunnable(ServerLevel world, LevelChunk chunk) {
        this.world = world;
        this.chunk = chunk;
        random = RandomSource.create();
    }
    
    @Override
    public void run() {
        var startX = chunk.getPos().getMinBlockX();
        var startZ = chunk.getPos().getMinBlockZ();
        var chunkSections = chunk.getSections();
        
        for (var sectionIndex = 0; sectionIndex < chunkSections.length; ++sectionIndex) {
            var chunkSection = chunkSections[sectionIndex];
            if (!chunkSection.isRandomlyTicking()) continue;
            
            var sectionCoordinate = chunk.getSectionYFromSectionIndex(sectionIndex);
            var startY = SectionPos.sectionToBlockCoord(sectionCoordinate);
            
            for(int i = 0; i < world.getGameRules().get(GameRules.RANDOM_TICK_SPEED); ++i) {
                var blockPos = world.getBlockRandomPos(startX, startY, startZ, 15);
                var blockState = chunkSection.getBlockState(blockPos.getX() - startX , blockPos.getY() - startY, blockPos.getZ() - startZ);
                var fluidState = blockState.getFluidState();
                
                if (blockState.isRandomlyTicking()) blockState.randomTick(world, blockPos, random);
                if (fluidState.isRandomlyTicking()) fluidState.randomTick(world, blockPos, random);
            }
        }
    }
}
