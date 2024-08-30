package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MobTickRunnable implements Runnable {
    private final ServerWorld world;
    private final int tickCount;
    
    public MobTickRunnable(ServerWorld world, int tickCount) {
        this.world = world;
        this.tickCount = tickCount;
    }
    
    @Override
    public void run() {
        var animals = new ArrayList<MobEntity>();
        var monsters = new ArrayList<MobEntity>();
        
        world.entityList.forEach(entity -> {
            if (entity.isRemoved()) return;
            
            if (SleepWarpConfig.tick_animals && entity instanceof AnimalEntity animal)
                animals.add(animal);
            else if (SleepWarpConfig.tick_monsters && entity instanceof HostileEntity monster)
                monsters.add(monster);
        });
        
        if (SleepWarpConfig.tick_animals) {
            for (var tick = 0; tick < tickCount * SleepWarpConfig.animal_tick_multiplier; tick++) {
                tickMobs(animals);
            }
        }
        
        if (SleepWarpConfig.tick_monsters) {
            for (var tick = 0; tick < tickCount * SleepWarpConfig.monster_tick_multiplier; tick++) {
                tickMobs(monsters);
            }
        }
    }
    
    private void tickMobs(List<MobEntity> entities) {
        Collections.shuffle(entities);
        
        for (MobEntity entity : entities) {
            world.getServer().submit(() -> {
                if (entity.isRemoved() || world.shouldCancelSpawn(entity) | !world.shouldTickEntity(entity.getBlockPos())) return;
                
                Entity entityVehicle = entity.getVehicle();
                if (entityVehicle != null && (entityVehicle.isRemoved() || !entityVehicle.hasPassenger(entity))) {
                    entity.stopRiding();
                }
                
                world.tickEntity(entity);
            });
        }
    }
}
