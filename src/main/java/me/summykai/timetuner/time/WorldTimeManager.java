package me.summykai.timetuner.time;

import org.bukkit.GameRule;
import org.bukkit.World;
import me.summykai.timetuner.TimeTuner;

public class WorldTimeManager {
    private final TimeTuner plugin;
    private final World world;
    private Time accumulatedTime;
    private double daySpeed;
    private double nightSpeed;
    private boolean paused;

    public WorldTimeManager(TimeTuner plugin, World world) {
        this(plugin, world, plugin.getDaySpeed(), plugin.getNightSpeed());
    }
    
    public WorldTimeManager(TimeTuner plugin, World world, double daySpeed, double nightSpeed) {
        this.plugin = plugin;
        this.world = world;
        this.daySpeed = daySpeed;
        this.nightSpeed = nightSpeed;
        this.accumulatedTime = Time.fromWorldTime(world.getTime());
        this.paused = false;
        initializeWorldRules();
    }

    private void initializeWorldRules() {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        if (plugin.getConfig().getBoolean("overflow-protection", true)) {
            TimeAdjuster.safeTimeUpdate(world, accumulatedTime.getTicks());
        }
    }

    public void tick() {
        if (shouldSkipTick()) return;
        
        syncWithWorldTime();
        Time delta = calculateTimeDelta();
        updateAccumulatedTime(delta);
        updateWorldTime();
    }

    private boolean shouldSkipTick() {
        return paused || world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) == Boolean.TRUE;
    }

    private void syncWithWorldTime() {
        long currentWorldTime = world.getTime();
        long accumulatedTicks = (long) (accumulatedTime.getTotalTime() % Time.DAY_LENGTH);
        
        if (currentWorldTime != accumulatedTicks) {
            accumulatedTime = Time.fromWorldTime(currentWorldTime);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Resynchronized time for " + world.getName() + 
                                    " (External change detected)");
            }
        }
    }

    private Time calculateTimeDelta() {
        double speed = getEffectiveSpeed();
        return new Time(speed * plugin.getTickFrequency());
    }

    private double getEffectiveSpeed() {
        double currentTime = accumulatedTime.getTotalTime() % Time.DAY_LENGTH;
        return currentTime < Time.NIGHT_START ? daySpeed : nightSpeed;
    }

    private void updateAccumulatedTime(Time delta) {
        accumulatedTime = accumulatedTime.add(delta);
    }

    private void updateWorldTime() {
        long newTime = (long) (accumulatedTime.getTotalTime() % Time.DAY_LENGTH);
        
        if (plugin.getConfig().getBoolean("overflow-protection", true)) {
            TimeAdjuster.safeTimeUpdate(world, newTime);
        } else {
            world.setTime(newTime);
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Time " + (paused ? "paused" : "resumed") + 
                                  " in " + world.getName());
        }
    }

    public void reset() {
        accumulatedTime = Time.fromWorldTime(world.getTime());
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Reset time tracking for " + world.getName());
        }
    }

    public void updateSpeeds(double daySpeed, double nightSpeed) {
        this.daySpeed = daySpeed;
        this.nightSpeed = nightSpeed;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Updated speeds for " + world.getName() + 
                                  " - Day: " + daySpeed + ", Night: " + nightSpeed);
        }
    }

    // Getters
    public World getWorld() { return world; }
    public double getDaySpeed() { return daySpeed; }
    public double getNightSpeed() { return nightSpeed; }
    public boolean isPaused() { return paused; }
}