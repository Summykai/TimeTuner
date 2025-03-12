package me.summykai.timetuner.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.time.WorldTimeManager;

public class WorldListener implements Listener {
    private final TimeTuner plugin;

    public WorldListener(TimeTuner plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        boolean enabled = plugin.getConfig().getBoolean("worlds." + worldName + ".enabled", false);
        
        if (enabled && !plugin.getWorldManagers().containsKey(event.getWorld().getUID())) {
            double daySpeed = plugin.getConfig().getDouble("worlds." + worldName + ".day-speed", plugin.getDaySpeed());
            double nightSpeed = plugin.getConfig().getDouble("worlds." + worldName + ".night-speed", plugin.getNightSpeed());
            
            plugin.getWorldManagers().put(event.getWorld().getUID(),
                new WorldTimeManager(plugin, event.getWorld(), daySpeed, nightSpeed));
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Automatically initialized manager for newly loaded world: " + worldName);
            }
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.removeWorldManager(event.getWorld().getUID());
    }
}
