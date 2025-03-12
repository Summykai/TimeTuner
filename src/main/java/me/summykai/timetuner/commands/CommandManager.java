package me.summykai.timetuner.commands;

import org.bukkit.World;
import org.bukkit.GameRule;
import org.bukkit.command.CommandSender;
import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.time.WorldTimeManager;
import me.summykai.timetuner.utils.ErrorHandler;
import me.summykai.timetuner.utils.MessageManager;

public class CommandManager {
    private final TimeTuner plugin;
    private final MessageManager messageManager;

    public CommandManager(TimeTuner plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("timetuner.reload")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        try {
            plugin.reloadConfigValues();
            plugin.initializeWorldManagers();
            messageManager.sendMessage(sender, "commands.reload.success");
            return true;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to reload configuration", e);
            ErrorHandler.logCommandError(sender, "Failed to reload configuration");
            return false;
        }
    }

    public boolean handlePause(CommandSender sender) {
        if (!sender.hasPermission("timetuner.pause")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        try {
            boolean newState = !plugin.isTimePaused();
            plugin.setTimePaused(newState);
            String key = newState ? "commands.pause.paused" : "commands.pause.resumed";
            messageManager.sendMessage(sender, key);
            return true;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to toggle pause state", e);
            ErrorHandler.logCommandError(sender, "Failed to toggle pause");
            return false;
        }
    }

    public boolean handleSpeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("timetuner.speed")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        if (args.length < 3) {
            messageManager.sendMessage(sender, "commands.speed.usage");
            return false;
        }

        try {
            double day = Double.parseDouble(args[1]);
            double night = Double.parseDouble(args[2]);
            
            if (day < 0 || night < 0 || !Double.isFinite(day) || !Double.isFinite(night)) {
                messageManager.sendMessage(sender, "errors.negative-value");
                return false;
            }

            // Use the new updateGlobalSpeeds method which handles config updates and runtime component updates
            plugin.updateGlobalSpeeds(day, night);
            
            messageManager.sendMessage(sender, "commands.speed.success", 
                messageManager.placeholders("day", String.valueOf(day), "night", String.valueOf(night)));
            return true;
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "errors.invalid-number");
            return false;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to set speeds", e);
            messageManager.sendMessage(sender, "commands.speed.error");
            return false;
        }
    }

    public boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("timetuner.status")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        try {
            // Log actual values for debugging
            plugin.getLogger().info("Status command - Debug Values:");
            plugin.getLogger().info("Global Day Speed: " + plugin.getDaySpeed());
            plugin.getLogger().info("Global Night Speed: " + plugin.getNightSpeed());
            plugin.getLogger().info("Paused: " + plugin.isTimePaused());
            plugin.getLogger().info("World Manager Count: " + plugin.getWorldManagers().size());
            
            // Log world-specific settings
            plugin.getWorldManagers().values().forEach(manager -> {
                String worldName = manager.getWorld().getName();
                plugin.getLogger().info("World '" + worldName + "' settings - Day: " + 
                    manager.getDaySpeed() + ", Night: " + manager.getNightSpeed());
            });
            
            messageManager.sendMessage(sender, "commands.status.header");
            messageManager.sendMessage(sender, "commands.status.day-speed", 
                messageManager.placeholders("speed", String.valueOf(plugin.getDaySpeed())));
            messageManager.sendMessage(sender, "commands.status.night-speed", 
                messageManager.placeholders("speed", String.valueOf(plugin.getNightSpeed())));
            messageManager.sendMessage(sender, "commands.status.paused", 
                messageManager.placeholders("state", String.valueOf(plugin.isTimePaused())));
            messageManager.sendMessage(sender, "commands.status.world-count", 
                messageManager.placeholders("count", String.valueOf(plugin.getWorldManagers().size())));
            
            // Add world-specific status information
            if (!plugin.getWorldManagers().isEmpty()) {
                messageManager.sendMessage(sender, "commands.status.world-settings-header");
                plugin.getWorldManagers().values().forEach(manager -> {
                    String worldName = manager.getWorld().getName();
                    messageManager.sendMessage(sender, "commands.status.world-settings-item",
                        messageManager.placeholders(
                            "world", worldName,
                            "day", String.valueOf(manager.getDaySpeed()),
                            "night", String.valueOf(manager.getNightSpeed())
                        ));
                });
            }
            return true;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to show status", e);
            messageManager.sendMessage(sender, "commands.status.failure");
            return false;
        }
    }

    public boolean handleWorlds(CommandSender sender) {
        if (!sender.hasPermission("timetuner.worlds")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        try {
            messageManager.sendMessage(sender, "commands.worlds.header");
            plugin.getWorldManagers().values().forEach(manager -> {
                String status = manager.getWorld().getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) ? "&a✔" : "&c✖";
                messageManager.sendMessage(sender, "commands.worlds.world-item", 
                    messageManager.placeholders(
                        "world", manager.getWorld().getName(),
                        "status", status
                    ));
            });
            return true;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to list worlds", e);
            messageManager.sendMessage(sender, "commands.worlds.error");
            return false;
        }
    }

    public boolean handleReset(CommandSender sender) {
        if (!sender.hasPermission("timetuner.reset")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        try {
            plugin.resetWorldTimes();
            messageManager.sendMessage(sender, "commands.reset.success");
            return true;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to reset times", e);
            messageManager.sendMessage(sender, "commands.reset.failure");
            return false;
        }
    }

    public boolean handleWorldSpeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("timetuner.worldspeed")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        
        if (args.length < 4) {
            messageManager.sendMessage(sender, "commands.worldspeed.usage");
            return false;
        }

        try {
            String worldName = args[1];
            
            // Validate that the world exists
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                messageManager.sendMessage(sender, "errors.invalid-world", 
                    messageManager.placeholders("world", worldName));
                return false;
            }
            
            // In Bukkit API, if getWorld() returns a non-null value, the world is already loaded
            // Let's make sure the world is also properly initialized with chunks loaded
            if (!world.isChunkLoaded(0, 0)) {
                messageManager.sendMessage(sender, "errors.world-not-loaded",
                    messageManager.placeholders("world", worldName));
                return false;
            }
            
            double day = Double.parseDouble(args[2]);
            double night = Double.parseDouble(args[3]);
            
            if (day < 0 || night < 0 || !Double.isFinite(day) || !Double.isFinite(night)) {
                messageManager.sendMessage(sender, "errors.negative-value");
                return false;
            }

            // Update config, save to disk, and update world manager immediately
            updateWorldConfig(worldName, day, night);
            
            messageManager.sendMessage(sender, "commands.worldspeed.success", 
                messageManager.placeholders("world", worldName, "day", String.valueOf(day), "night", String.valueOf(night)));
            return true;
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "errors.invalid-number");
            return false;
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to set world speeds", e);
            messageManager.sendMessage(sender, "commands.worldspeed.error");
            return false;
        }
    }
    
    /**
     * Updates a world's configuration values, saves them to disk, and updates the runtime world manager.
     * 
     * @param worldName The name of the world to update
     * @param daySpeed The new day speed value
     * @param nightSpeed The new night speed value
     */
    private void updateWorldConfig(String worldName, double daySpeed, double nightSpeed) {
        // Update config in memory
        plugin.getConfig().set("worlds." + worldName + ".day-speed", daySpeed);
        plugin.getConfig().set("worlds." + worldName + ".night-speed", nightSpeed);
        plugin.getConfig().set("worlds." + worldName + ".enabled", true); // Ensure enabled
        
        // Save to disk
        plugin.saveConfig();
        
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;
        
        // Add new manager if not present
        if (!plugin.getWorldManagers().containsKey(world.getUID())) {
            WorldTimeManager manager = new WorldTimeManager(plugin, world, daySpeed, nightSpeed);
            plugin.getWorldManagers().put(world.getUID(), manager);
        } else {
            // Update existing manager
            plugin.getWorldManagers().get(world.getUID()).updateSpeeds(daySpeed, nightSpeed);
        }
    }

    public boolean handleHelp(CommandSender sender) {
        if (!sender.hasPermission("timetuner.use")) {
            messageManager.sendMessage(sender, "errors.no-permission");
            return false;
        }
        messageManager.sendMessage(sender, "commands.help.header");
        messageManager.sendMessage(sender, "commands.help.reload");
        messageManager.sendMessage(sender, "commands.help.pause");
        messageManager.sendMessage(sender, "commands.help.speed");
        messageManager.sendMessage(sender, "commands.help.status");
        messageManager.sendMessage(sender, "commands.help.worlds");
        messageManager.sendMessage(sender, "commands.help.reset");
        messageManager.sendMessage(sender, "commands.help.worldspeed");
        messageManager.sendMessage(sender, "commands.help.help");
        return true;
    }
}