package me.thegiggitybyte.sleepwarp.runnable;

import java.util.Random;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public class LightningTickRunnable implements Runnable {
    private final ServerLevel world;
    private final LevelChunk chunk;
    
    public LightningTickRunnable(ServerLevel world, LevelChunk chunk) {
        this.world = world;
        this.chunk = chunk;
    }
    
    @Override
    public void run() {
        var chunkPos = chunk.getPos();
        var randomPos = world.getBlockRandomPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), 15);
        var blockPos = world.findLightningTargetAround(randomPos);
        
        var canSpawnMobs = world.getGameRules().get(GameRules.SPAWN_MOBS);
        var localDifficulty = world.getCurrentDifficultyAt(blockPos).getEffectiveDifficulty() * 0.01;
        boolean skeletonHorseSpawn = canSpawnMobs && (new Random().nextDouble() < localDifficulty) && !world.getBlockState(blockPos.below()).is(Blocks.LIGHTNING_ROD);
        
        if (skeletonHorseSpawn) {
            SkeletonHorse skeletonHorseEntity = EntityType.SKELETON_HORSE.create(world, EntitySpawnReason.NATURAL);
            if (skeletonHorseEntity != null) {
                skeletonHorseEntity.setTrap(true);
                skeletonHorseEntity.setAge(0);
                skeletonHorseEntity.setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                world.addFreshEntity(skeletonHorseEntity);
            }
        }
        
        LightningBolt lightningEntity = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.NATURAL);
        if (lightningEntity != null) {
            lightningEntity.snapTo(Vec3.atBottomCenterOf(blockPos));
            lightningEntity.setVisualOnly(skeletonHorseSpawn);
            world.addFreshEntity(lightningEntity);
        }
    }
}
