package me.summykai.timetuner.commands;

import org.bukkit.World;
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

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("timetuner.use")) {
                ErrorHandler.logCommandError(sender, "You don't have permission for this command");
                return true;
            }
            return handleStatus(sender);
        }

        String subCommand = args[0].toLowerCase();
        String permission = getPermission(subCommand);
        
        if (!sender.hasPermission(permission)) {
            ErrorHandler.logCommandError(sender, "You don't have permission for this command");
            return true;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Processing command '%s' from %s",
                subCommand,
                sender.getName()
            ));
        }
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "pause":
                return handlePause(sender, args);
            case "resume":
                return handleResume(sender, args);
            case "speed":
                return handleSpeed(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "help":
                return handleHelp(sender);
            case "status":
                return handleStatus(sender);
            default:
                ErrorHandler.logCommandError(sender, "Unknown command: " + subCommand);
                return false;
        }
    }

    private String getPermission(String command) {
        switch (command) {
            case "help":
            case "status":
                return "timetuner.use";
            case "pause":
            case "resume":
                return "timetuner.pause";
            default:
                return "timetuner." + command;
        }
    }

    public boolean handleStatus(CommandSender sender) {
        messageManager.sendFeedback(sender, "commands.status.header");
        
        for (World world : plugin.getServer().getWorlds()) {
            WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
            if (manager != null) {
                messageManager.sendFeedback(sender, "commands.status.world",
                    "world", world.getName(),
                    "day_speed", String.format("%.2f", manager.getDaySpeed()),
                    "night_speed", String.format("%.2f", manager.getNightSpeed()),
                    "is_day", String.valueOf(manager.isDay()),
                    "paused", String.valueOf(manager.isPaused()),
                    "time", String.valueOf(world.getTime())
                );
            }
        }
        
        return true;
    }

    public boolean handleReload(CommandSender sender) {
        plugin.reloadConfigValues();
        messageManager.sendFeedback(sender, "commands.reload.success");
        return true;
    }

    public boolean handlePause(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[1];
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                ErrorHandler.logCommandError(sender, "World not found: " + worldName);
                return false;
            }
            
            WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
            if (manager == null) {
                ErrorHandler.logCommandError(sender, "World not managed: " + worldName);
                return false;
            }
            
            manager.setPaused(true);
            messageManager.sendFeedback(sender, "commands.pause.world.success", "world", worldName);
        } else {
            // Pause all worlds
            long pausedCount = plugin.getWorldManagers().values().stream()
                .peek(manager -> manager.setPaused(true))
                .count();
            if (pausedCount > 0) {
                messageManager.sendFeedback(sender, "commands.pause.global.success");
            } else {
                messageManager.sendFeedback(sender, "errors.no-managed-worlds");
            }
        }
        return true;
    }

    public boolean handleResume(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[1];
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                ErrorHandler.logCommandError(sender, "World not found: " + worldName);
                return false;
            }
            
            WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
            if (manager == null) {
                ErrorHandler.logCommandError(sender, "World not managed: " + worldName);
                return false;
            }
            
            manager.setPaused(false);
            messageManager.sendFeedback(sender, "commands.resume.world.success", "world", worldName);
        } else {
            // Resume all worlds
            long resumedCount = plugin.getWorldManagers().values().stream()
                .peek(manager -> manager.setPaused(false))
                .count();
            if (resumedCount > 0) {
                messageManager.sendFeedback(sender, "commands.resume.global.success");
            } else {
                messageManager.sendFeedback(sender, "errors.no-managed-worlds");
            }
        }
        return true;
    }

    public boolean handleSpeed(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ErrorHandler.logCommandError(sender, "Usage: /timetuner speed <day|night|both> <speed> [world]");
            return false;
        }

        String type = args[1].toLowerCase();
        if (!type.equals("day") && !type.equals("night") && !type.equals("both")) {
            ErrorHandler.logCommandError(sender, "Invalid speed type. Use: day, night, or both");
            return false;
        }

        double speed;
        try {
            speed = Double.parseDouble(args[2]);
            if (speed < 0 || speed > 20) {
                ErrorHandler.logCommandError(sender, "Speed must be between 0 and 20");
                return false;
            }
        } catch (NumberFormatException e) {
            ErrorHandler.logCommandError(sender, "Invalid speed value");
            return false;
        }

        WorldTimeManager manager = null;
        String worldName = null;

        if (args.length > 3) {
            worldName = args[3];
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                ErrorHandler.logCommandError(sender, "World not found: " + worldName);
                return false;
            }

            manager = plugin.getWorldManagers().get(world.getUID());
            if (manager == null) {
                ErrorHandler.logCommandError(sender, "World not managed: " + worldName);
                return false;
            }
        }

        final double newDaySpeed = type.equals("day") || type.equals("both") ? speed : 
            (manager != null ? manager.getDaySpeed() : plugin.getDaySpeed());
        final double newNightSpeed = type.equals("night") || type.equals("both") ? speed :
            (manager != null ? manager.getNightSpeed() : plugin.getNightSpeed());

        if (manager != null) {
            // World-specific speed update
            manager.updateSpeeds(newDaySpeed, newNightSpeed);
            messageManager.sendFeedback(sender, "commands.speed.success",
                "world", worldName,
                "type", type,
                "speed", String.format("%.2f", speed)
            );
        } else {
            // Global speed update
            plugin.getWorldManagers().values().forEach(mgr -> mgr.updateSpeeds(newDaySpeed, newNightSpeed));
            messageManager.sendFeedback(sender, "commands.speed.success",
                "type", type,
                "speed", String.format("%.2f", speed)
            );
        }

        return true;
    }

    public boolean handleReset(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[1];
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                ErrorHandler.logCommandError(sender, "World not found: " + worldName);
                return false;
            }

            WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
            if (manager == null) {
                ErrorHandler.logCommandError(sender, "World not managed: " + worldName);
                return false;
            }

            manager.skipToDay();
            messageManager.sendFeedback(sender, "commands.reset.success", "world", worldName);
        } else {
            plugin.resetWorldTimes();
            messageManager.sendFeedback(sender, "commands.reset.success");
        }
        return true;
    }

    public boolean handleHelp(CommandSender sender) {
        messageManager.sendFeedback(sender, "commands.help.header");
        messageManager.sendFeedback(sender, "commands.help.reload");
        messageManager.sendFeedback(sender, "commands.help.pause");
        messageManager.sendFeedback(sender, "commands.help.resume");
        messageManager.sendFeedback(sender, "commands.help.speed");
        messageManager.sendFeedback(sender, "commands.help.reset");
        messageManager.sendFeedback(sender, "commands.help.status");
        return true;
    }
}