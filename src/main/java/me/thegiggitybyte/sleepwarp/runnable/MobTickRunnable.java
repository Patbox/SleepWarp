package me.thegiggitybyte.sleepwarp.runnable;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MobTickRunnable implements Runnable {
    private final ServerLevel world;
    private final int tickCount;
    
    public MobTickRunnable(ServerLevel world, int tickCount) {
        this.world = world;
        this.tickCount = tickCount;
    }
    
    @Override
    public void run() {
        var animals = new ArrayList<Mob>();
        var monsters = new ArrayList<Mob>();
        
        world.entityTickList.forEach(entity -> {
            if (entity.isRemoved()) return;
            
            if (SleepWarpConfig.tick_animals && entity instanceof Animal animal)
                animals.add(animal);
            else if (SleepWarpConfig.tick_monsters && entity instanceof Monster monster)
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
    
    private void tickMobs(List<Mob> entities) {
        Collections.shuffle(entities);
        
        for (Mob entity : entities) {
            world.getServer().submit(() -> {
                if (entity.isRemoved() || !world.isPositionEntityTicking(entity.blockPosition())) return;
                
                Entity entityVehicle = entity.getVehicle();
                if (entityVehicle != null && (entityVehicle.isRemoved() || !entityVehicle.hasPassenger(entity))) {
                    entity.stopRiding();
                }
                
                world.tickNonPassenger(entity);
            });
        }
    }
}
