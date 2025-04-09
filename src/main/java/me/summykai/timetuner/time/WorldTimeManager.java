package me.summykai.timetuner.time;

import me.summykai.timetuner.TimeTuner;
import org.bukkit.World;

public class WorldTimeManager {
    private final TimeTuner plugin;
    private final World world;
    private double daySpeed;
    private double nightSpeed;
    private Time accumulatedTime;
    private boolean skipping; // Flag indicating a sleep skip is in progress
    private boolean paused;
    private long lastProcessedTime; // Tracks the last *tick* value set in the world
    private boolean isDayCache;
    private long lastTimeCheck;
    private static final long TIME_CHECK_INTERVAL = 50; // ms

    public WorldTimeManager(TimeTuner plugin, World world, double daySpeed, double nightSpeed) {
        this.plugin = plugin;
        this.world = world;
        this.daySpeed = daySpeed;
        this.nightSpeed = nightSpeed;
        this.accumulatedTime = Time.fromWorldTime(world.getTime());
        this.skipping = false; // Initialize as false
        this.paused = false;
        this.lastProcessedTime = world.getTime();
        updateTimeCache(); // Initial cache population
    }

    public void updateTime() {
         if (!shouldUpdateTime()) {
             // If paused or skipping, don't advance time.
             // If auto-pause enabled and world empty, also don't advance.
             // Reset accumulated time to current world time if paused to prevent jump on resume?
             // No, keep accumulated time to maintain precision, just don't add to it.
             return;
         }

        double speed = getEffectiveSpeed();
        if (speed <= 0) {
            // If speed is zero, effectively paused, do nothing.
            return;
        }

        Time delta = new Time(speed * plugin.getTickFrequency());
        accumulatedTime = accumulatedTime.add(delta);

        long newTimeTicks = accumulatedTime.getTicks(); // Get the integer part for world time
        long newTimeModulo = newTimeTicks % Time.DAY_LENGTH;

        // Only update world time if the tick value actually changes
        if (newTimeModulo != lastProcessedTime) {
            updateWorldTime(newTimeModulo); // Pass the calculated time
            lastProcessedTime = newTimeModulo;
            updateTimeCache(); // Update day/night status based on new time

             if (plugin.isDebugMode() && Math.random() < 0.01) { // Log occasionally in debug mode
                 plugin.getLogger().info(() -> String.format(
                     "Updated time in %s: %d (Accumulated: %.2f, Speed: %.2f, Day: %b)",
                     world.getName(), newTimeModulo, accumulatedTime.getTotalTime(), speed, isDayCache
                 ));
             }
        }
    }

    private void updateTimeCache() {
        // Use the processed world time for cache consistency
        long currentTime = lastProcessedTime; // Use the value actually set in the world
        isDayCache = currentTime >= Time.DAY_START && currentTime < Time.NIGHT_START;
        lastTimeCheck = System.currentTimeMillis();

         // Debug logging for cache updates can be very verbose, keep commented unless needed
         /*
         if (plugin.isDebugMode()) {
              plugin.getLogger().info(() -> String.format(
                 "Updated time cache for world '%s' (time: %d, isDay: %b)",
                 world.getName(), currentTime, isDayCache
             ));
         }
         */
    }

    private double getEffectiveSpeed() {
        return isDay() ? daySpeed : nightSpeed;
    }

    private boolean shouldUpdateTime() {
        if (skipping) return false; // Don't update time during sleep skip transition
        if (paused) return false;
        if (plugin.isAutoPauseEmpty() && world.getPlayers().isEmpty()) return false;
        return true;
    }

    public void skipToDay() {
        // Set skipping flag to prevent interference during update
        this.setSkipping(true);

        boolean wasStorming = world.hasStorm();
        boolean wasThundering = world.isThundering();

        // Reset accumulated time and world time to day start (0)
        this.accumulatedTime = new Time(Time.DAY_START);
        long targetTime = Time.DAY_START; // Explicitly day start
        updateWorldTime(targetTime);
        this.lastProcessedTime = targetTime;
        updateTimeCache(); // Update cache immediately

        // Clear Weather if needed
        if (wasStorming) {
            world.setStorm(false);
            if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Cleared storm in world '%s' after sleep skip", world.getName()));
            }
        }
        if (wasThundering) {
            world.setThundering(false);
            world.setThunderDuration(0);
            if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Cleared thunder in world '%s' after sleep skip", world.getName()));
            }
        }

        if (plugin.isDebugMode()) {
             plugin.getLogger().info(() -> String.format("Skipped to day (Time: %d) and potentially cleared weather in world '%s'", targetTime, world.getName()));
        }

        // Reset skipping flag slightly later to allow world state to settle
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> this.setSkipping(false), 2L); // 2 ticks later
    }

    private void updateWorldTime(long newTimeModulo) {
        if (plugin.isOverflowProtection()) {
            // TimeAdjuster handles potential full time overflow if needed elsewhere,
            // Here we just set the time-of-day part.
            TimeAdjuster.safeTimeUpdate(world, newTimeModulo);

             // Normalize accumulated time if it went over a day cycle ONLY if precision mode requires it?
             // Generally, accumulated time should keep growing for precision.
             // Let's only normalize if it becomes excessively large? Or rely on safeTimeUpdate's fullTime handling.
             // Keeping accumulated time precise is better. Overflow protection mainly affects setFullTime.
            // Let's remove the normalization here.

        } else {
            // If overflow protection is disabled, just set the time directly
            world.setTime(newTimeModulo);
        }
        // lastProcessedTime is updated in updateTime() after this call
    }


    public boolean isDay() {
        // Check cache validity
        long now = System.currentTimeMillis();
        if (now - lastTimeCheck > TIME_CHECK_INTERVAL) {
            updateTimeCache(); // Refresh cache if stale
        }
        return isDayCache;
    }

    // --- Getters and Setters ---

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
             plugin.getLogger().info(() -> String.format("Updated speeds for world '%s' (Day: %.2f, Night: %.2f)", world.getName(), daySpeed, nightSpeed));
        }
    }

    public boolean isSkipping() {
        return skipping;
    }

    // Make setter private or package-private if only controlled internally
    private void setSkipping(boolean skipping) {
        this.skipping = skipping;
        if (plugin.isDebugMode()) {
             plugin.getLogger().info(() -> String.format("Set skipping state for world '%s' to %b", world.getName(), skipping));
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            // When resuming, sync accumulated time with current world time
            // to prevent jumps if time was changed externally while paused.
            this.accumulatedTime = Time.fromWorldTime(world.getTime());
            this.lastProcessedTime = world.getTime();
            updateTimeCache();
        }
         if (plugin.isDebugMode()) {
              plugin.getLogger().info(() -> String.format("Set paused state for world '%s' to %b. Synced time: %d", world.getName(), paused, this.lastProcessedTime));
         }
    }
}