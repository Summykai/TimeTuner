package me.summykai.timetuner.commands;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import me.summykai.timetuner.TimeTuner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeTunerCommandExecutor implements CommandExecutor, TabCompleter {
    private static final List<String> MAIN_COMMANDS = Arrays.asList(
        "reload", "pause", "resume", "speed", "status", "reset", "help"
    );
    private static final List<String> SPEED_ARGS = Arrays.asList(
        "day", "night", "both"
    );
    private static final List<String> DEFAULT_SPEEDS = Arrays.asList(
        "0", "0.5", "1", "2"
    );
    private static final int MAX_SPEED = 20;
    private static final long CACHE_DURATION = 5000; // 5 seconds

    private final TimeTuner plugin;
    private final CommandManager commandManager;
    private final Map<String, List<String>> completionCache;
    private long lastCacheUpdate;

    public TimeTunerCommandExecutor(TimeTuner plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.completionCache = new HashMap<>();
        this.lastCacheUpdate = 0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandManager.handleCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return MAIN_COMMANDS;
        }

        String partial = args[args.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions = MAIN_COMMANDS.stream()
                .filter(cmd -> hasPermission(sender, getPermissionForTabComplete(cmd)))
                .filter(cmd -> cmd.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            switch (cmd) {
                case "speed":
                    completions = SPEED_ARGS.stream()
                        .filter(arg -> arg.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                    break;
                case "pause":
                case "resume":
                    if (hasPermission(sender, "timetuner.pause")) {
                        completions = getWorldCompletions(partial);
                    }
                    break;
                case "reset":
                    if (hasPermission(sender, "timetuner.reset")) {
                        completions = getWorldCompletions(partial);
                    }
                    break;
                default:
                    break;
            }
        } else if (args.length == 3) {
            String cmd = args[0].toLowerCase();
            String subCmd = args[1].toLowerCase();

            if ("speed".equals(cmd) && hasPermission(sender, "timetuner.speed")) {
                if (SPEED_ARGS.contains(subCmd)) {
                    completions = getSpeedCompletions(partial);
                } else {
                    completions = getWorldCompletions(partial);
                }
            }
        }

        return completions;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        boolean hasPerm = sender.hasPermission(permission);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(String.format(
                "Permission check: %s for %s: %b",
                sender.getName(),
                permission,
                hasPerm
            ));
        }
        return hasPerm;
    }

    private String getPermissionForTabComplete(String command) {
        switch (command.toLowerCase()) {
            case "help":
            case "status":
                return "timetuner.use";
            case "pause":
            case "resume":
                return "timetuner.pause";
            default:
                return "timetuner." + command.toLowerCase();
        }
    }

    private List<String> getWorldCompletions(String partial) {
        long now = System.currentTimeMillis();
        String cacheKey = "worlds_" + partial.toLowerCase();

        // Check cache first
        if (now - lastCacheUpdate < CACHE_DURATION) {
            List<String> cached = completionCache.get(cacheKey);
            if (cached != null) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Using cached world completions for '" + partial + "'");
                }
                return cached;
            }
        }

        // Cache expired or missing, rebuild
        List<String> worlds = plugin.getServer().getWorlds().stream()
            .map(World::getName)
            .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
            .collect(Collectors.toList());

        // Update cache
        completionCache.put(cacheKey, worlds);
        lastCacheUpdate = now;

        if (plugin.isDebugMode()) {
            plugin.getLogger().info(String.format(
                "Updated world completions cache for '%s' with %d entries",
                partial, worlds.size()
            ));
        }

        return worlds;
    }

    private List<String> getSpeedCompletions(String partial) {
        if (partial.isEmpty()) {
            return DEFAULT_SPEEDS;
        }

        try {
            double value = Double.parseDouble(partial);
            if (value < 0) {
                return Collections.emptyList();
            }
            if (value > MAX_SPEED) {
                return Collections.singletonList(String.valueOf(MAX_SPEED));
            }
            return Collections.singletonList(partial);
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }
}
