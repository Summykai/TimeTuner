package me.summykai.timetuner.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.World;
import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.time.WorldTimeManager;
import me.summykai.timetuner.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimeTunerCommandExecutor implements CommandExecutor, TabCompleter {
    private final TimeTuner plugin;
    private final CommandManager commandManager;
    private final List<String> mainCommands = Arrays.asList("reload", "pause", "speed", "status", "worlds", "reset", "worldspeed", "help");
    private final List<String> commonSpeedValues = Arrays.asList("0.5", "0.75", "1.0", "1.5", "2.0", "3.0");

    public TimeTunerCommandExecutor(TimeTuner plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("timetuner.status")) {
                ErrorHandler.logCommandError(sender, "You don't have permission for this command");
                return true;
            }
            return commandManager.handleStatus(sender);
        }

        String subCommand = args[0].toLowerCase();
        
        // Map the command to the correct permission
        String permission;
        switch (subCommand) {
            case "help":
            case "status":
            case "worlds":
                permission = "timetuner.use";
                break;
            default:
                permission = "timetuner." + subCommand;
                break;
        }
        
        if (!sender.hasPermission(permission)) {
            ErrorHandler.logCommandError(sender, "You don't have permission for this command");
            return true;
        }
        
        switch (subCommand) {
            case "reload":
                return commandManager.handleReload(sender);
            case "pause":
                return commandManager.handlePause(sender);
            case "speed":
                return commandManager.handleSpeed(sender, args);
            case "status":
                return commandManager.handleStatus(sender);
            case "worlds":
                return commandManager.handleWorlds(sender);
            case "reset":
                return commandManager.handleReset(sender);
            case "worldspeed":
                return commandManager.handleWorldSpeed(sender, args);
            case "help":
                return commandManager.handleHelp(sender);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            completions = mainCommands.stream()
                .filter(cmd -> cmd.startsWith(partialCommand))
                .filter(cmd -> {
                    // Use the same permission mapping for tab complete
                    String perm;
                    switch (cmd) {
                        case "help":
                        case "status":
                        case "worlds":
                            perm = "timetuner.use";
                            break;
                        default:
                            perm = "timetuner." + cmd;
                            break;
                    }
                    return sender.hasPermission(perm);
                })
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("worldspeed")) {
            String partialWorldName = args[1].toLowerCase();
            completions = plugin.getServer().getWorlds().stream()
                .map(World::getName)
                .filter(name -> name.toLowerCase().startsWith(partialWorldName))
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("speed")) {
            // Suggest day speed values for arg 1
            completions = suggestSpeedValues(args[1], plugin.getDaySpeed());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("speed")) {
            // Suggest night speed values for arg 2
            completions = suggestSpeedValues(args[2], plugin.getNightSpeed());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("worldspeed")) {
            // For worldspeed, get the world's current day speed if available for arg 2
            String worldName = args[1];
            double currentSpeed = plugin.getDaySpeed(); // Default to global
            
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
                if (manager != null) {
                    currentSpeed = manager.getDaySpeed();
                }
            }
            
            completions = suggestSpeedValues(args[2], currentSpeed);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("worldspeed")) {
            // For worldspeed, get the world's current night speed if available for arg 3
            String worldName = args[1];
            double currentSpeed = plugin.getNightSpeed(); // Default to global
            
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
                if (manager != null) {
                    currentSpeed = manager.getNightSpeed();
                }
            }
            
            completions = suggestSpeedValues(args[3], currentSpeed);
        }
        
        return completions;
    }
    
    /**
     * Helper method to suggest speed values based on input
     * 
     * @param input Current user input
     * @param currentSpeed The current speed setting to include in suggestions
     * @return List of suggested speed values
     */
    private List<String> suggestSpeedValues(String input, double currentSpeed) {
        String currentSpeedStr = String.format("%.2f", currentSpeed).replaceAll("\\.?0+$", "");
        List<String> suggestions = new ArrayList<>(commonSpeedValues);
        if (!suggestions.contains(currentSpeedStr)) {
            suggestions.add(currentSpeedStr);
        }
        
        return suggestions.stream()
            .filter(speed -> speed.startsWith(input))
            .collect(Collectors.toList());
    }
}
