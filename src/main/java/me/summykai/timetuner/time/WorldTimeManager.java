package me.summykai.timetuner.time;

import org.bukkit.World;
import me.summykai.timetuner.TimeTuner;

public class WorldTimeManager {
    private final TimeTuner plugin;
    private final World world;
    private double daySpeed;
    private double nightSpeed;
    private Time accumulatedTime;
    private boolean skipping;
    private boolean paused;
    private long lastProcessedTime;
    private boolean isDayCache;
    private long lastTimeCheck;
    private static final long TIME_CHECK_INTERVAL = 50; // Check every 50ms

    public WorldTimeManager(TimeTuner plugin, World world, double daySpeed, double nightSpeed) {
        this.plugin = plugin;
        this.world = world;
        this.daySpeed = daySpeed;
        this.nightSpeed = nightSpeed;
        this.accumulatedTime = Time.fromWorldTime(world.getTime());
        this.skipping = false;
        this.paused = false;
        this.lastProcessedTime = world.getTime();
        updateTimeCache();
    }

    public void updateTime() {
        if (!shouldUpdateTime()) {
            return;
        }

        double speed = getEffectiveSpeed();
        if (speed <= 0) {
            return;
        }

        Time delta = new Time(speed * plugin.getTickFrequency());
        accumulatedTime = accumulatedTime.add(delta);

        long newTime = accumulatedTime.getTicks() % Time.DAY_LENGTH;
        if (newTime != lastProcessedTime) {
            updateWorldTime();
            lastProcessedTime = newTime;
            updateTimeCache();

            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> String.format(
                    "Updated time in world %s (old: %d, new: %d, speed: %.2f, isDay: %b)",
                    world.getName(),
                    lastProcessedTime,
                    newTime,
                    speed,
                    isDayCache
                ));
            }
        }
    }

    private void updateTimeCache() {
        long currentTime = accumulatedTime.getTicks() % Time.DAY_LENGTH;
        isDayCache = currentTime >= Time.DAY_START && currentTime < Time.NIGHT_START;
        lastTimeCheck = System.currentTimeMillis();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Updated time cache for world '%s' (time: %d, isDay: %b)",
                world.getName(),
                currentTime,
                isDayCache
            ));
        }
    }

    private double getEffectiveSpeed() {
        return isDay() ? daySpeed : nightSpeed;
    }

    private boolean shouldUpdateTime() {
        if (skipping) {
            return false;
        }

        if (paused) {
            return false;
        }

        if (plugin.isAutoPauseEmpty() && world.getPlayers().isEmpty()) {
            return false;
        }

        return true;
    }

    public void skipToDay() {
        this.accumulatedTime = new Time(0);
        updateWorldTime();
        this.lastProcessedTime = 0;
        updateTimeCache();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Skipped to day in world '%s'",
                world.getName()
            ));
        }
    }

    private void updateWorldTime() {
        long newTime = accumulatedTime.getTicks();
        
        if (plugin.isOverflowProtection()) {
            // Apply overflow protection by using TimeAdjuster
            TimeAdjuster.safeTimeUpdate(world, newTime % Time.DAY_LENGTH);
            
            // If we had overflow, normalize the accumulated time
            if (newTime >= Time.DAY_LENGTH) {
                accumulatedTime = new Time(newTime % Time.DAY_LENGTH);
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info(() -> String.format(
                        "Applied overflow protection in world '%s' (ticks: %d)",
                        world.getName(),
                        newTime
                    ));
                }
            }
        } else {
            // If overflow protection is disabled, just set the time directly
            world.setTime(newTime % Time.DAY_LENGTH);
        }
        
        lastProcessedTime = newTime % Time.DAY_LENGTH;
    }

    public boolean isDay() {
        long now = System.currentTimeMillis();
        if (now - lastTimeCheck > TIME_CHECK_INTERVAL) {
            updateTimeCache();
        }
        return isDayCache;
    }

    public World getWorld() {
        return world;
    }

    public double getDaySpeed() {
        return daySpeed;
    }

    public double getNightSpeed() {
        return nightSpeed;
    }

    public void updateSpeeds(double daySpeed, double nightSpeed) {
        this.daySpeed = daySpeed;
        this.nightSpeed = nightSpeed;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Updated speeds for world '%s' (day: %.2f, night: %.2f)",
                world.getName(),
                daySpeed,
                nightSpeed
            ));
        }
    }

    public boolean isSkipping() {
        return skipping;
    }

    public void setSkipping(boolean skipping) {
        this.skipping = skipping;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Set skipping state for world '%s' to %s",
                world.getName(),
                skipping
            ));
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Set paused state for world '%s' to %s",
                world.getName(),
                paused
            ));
        }
    }
}