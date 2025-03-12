// TimeAdjuster.java
package me.summykai.timetuner.time;

import org.bukkit.World;

public class TimeAdjuster {
    private static final long OVERFLOW_RESET = 1728000; // 72 days
    
    public static void safeTimeUpdate(World world, long newTime) {
        long currentFullTime = world.getFullTime();
        if (currentFullTime > OVERFLOW_RESET) {
            world.setFullTime(currentFullTime % OVERFLOW_RESET);
        }
        world.setTime(newTime);
    }
}