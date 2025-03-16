package me.summykai.timetuner.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.time.WorldTimeManager;
import me.summykai.timetuner.utils.ErrorHandler;

public class WorldListener implements Listener {
    private final TimeTuner plugin;

    public WorldListener(TimeTuner plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(
                String.format(
                    "World '%s' loaded, checking configuration",
                    world.getName()
                )
            );
        }

        try {
            plugin.initializeWorldManager(world);
        } catch (Exception e) {
            ErrorHandler.logPluginError(
                "Failed to initialize world manager",
                e
            );
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
        
        if (manager != null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(
                    String.format(
                        "World '%s' unloaded, removing manager",
                        world.getName()
                    )
                );
            }
            plugin.getWorldManagers().remove(world.getUID());
        }
    }
}
