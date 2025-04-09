package me.summykai.timetuner.listeners;

import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.TimeTuner.WorldConfig;
import me.summykai.timetuner.time.Time;
import me.summykai.timetuner.time.WorldTimeManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final TimeTuner plugin;
    private final Map<UUID, Set<UUID>> sleepingPlayers;

    public PlayerListener(TimeTuner plugin) {
        this.plugin = plugin;
        this.sleepingPlayers = new HashMap<>();
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        World world = event.getPlayer().getWorld();
        WorldConfig worldConfig = plugin.getWorldConfig(world);

        if (!isValidSleepAttempt(event, worldConfig)) {
            return;
        }

        WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());

        if (!isValidWorldState(manager, world)) {
            return;
        }

        boolean isNight = isNightTime(world);
        boolean isThunderstorm = world.isThundering();
        boolean canSleepDueToThunder = isThunderstorm && worldConfig.isAllowThunderstormSleep();

        if (!isNight && !canSleepDueToThunder) {
            if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format(
                    "Bed enter denied for %s in %s: Not night (%b) and not valid thunderstorm condition (isThundering: %b, allowThunderSleep: %b)",
                    event.getPlayer().getName(), world.getName(), isNight, isThunderstorm, worldConfig.isAllowThunderstormSleep()
                ));
            }
            return;
        }
         if (plugin.isDebugMode() && canSleepDueToThunder && !isNight) {
             plugin.getLogger().info(() -> String.format(
                "Player %s entering bed in %s due to thunderstorm (Time: %d)",
                 event.getPlayer().getName(), world.getName(), world.getTime()
             ));
         }

        // Add player to sleeping cache
        sleepingPlayers.computeIfAbsent(world.getUID(), k -> new HashSet<>())
                .add(event.getPlayer().getUniqueId());

        // Schedule the check slightly later
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            WorldTimeManager currentManager = plugin.getWorldManagers().get(world.getUID());
            Player player = event.getPlayer();
            if (player.isOnline() && player.getWorld().equals(world) && player.isSleeping()) {
               checkAndProcessSleepSkip(player, world, currentManager);
            } else {
               // Player left or got out of bed before check
               sleepingPlayers.getOrDefault(world.getUID(), Collections.emptySet()).remove(player.getUniqueId());
            }
        }, 1L); // 1 tick later
    }

    private boolean isValidSleepAttempt(PlayerBedEnterEvent event, WorldConfig worldConfig) {
        if (!plugin.isAllowSleepSkip()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> "Sleep skip disabled globally in config");
            }
            return false;
        }

        PlayerBedEnterEvent.BedEnterResult result = event.getBedEnterResult();

        if (result == PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_HERE ||
            result == PlayerBedEnterEvent.BedEnterResult.NOT_SAFE ||
            result == PlayerBedEnterEvent.BedEnterResult.OBSTRUCTED ||
            result == PlayerBedEnterEvent.BedEnterResult.TOO_FAR_AWAY) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> String.format("Invalid bed enter result for %s: %s", event.getPlayer().getName(), result));
            }
            return false;
        }

        // Prevent interaction if bed explosions are disallowed in config for relevant dimensions
        if (result == PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_HERE &&
            !worldConfig.isAllowBedExplosions() &&
            event.getPlayer().getWorld().getEnvironment() != World.Environment.NORMAL) {
             if (plugin.isDebugMode()) {
               plugin.getLogger().info(() -> String.format("Preventing bed interaction for %s in %s due to disallowed explosion.", event.getPlayer().getName(), event.getPlayer().getWorld().getName()));
            }
            return false;
        }

        return true;
    }

    private boolean isValidWorldState(WorldTimeManager manager, World world) {
        if (manager == null || manager.isSkipping()) {
            if (plugin.isDebugMode()) {
                String reason = (manager == null) ? "world not managed" : "already skipping";
                 plugin.getLogger().info(() -> String.format("Skipping bed enter event processing: %s in world %s", reason, world.getName()));
            }
            return false;
        }
        return true;
    }

    private boolean isNightTime(World world) {
        long time = world.getTime();
        // Vanilla night start for sleep is ~12541, end is ~23458
        // Let's use the plugin's defined constants for consistency if they exist,
        // otherwise use vanilla approximate values. Using 12000 as NIGHT_START might be slightly early.
        // Let's stick to the plugin's Time constants for now.
        return time >= Time.NIGHT_START || time < Time.DAY_START; // Checks if time is in the wrap-around night period
    }

    private void checkAndProcessSleepSkip(Player player, World world, WorldTimeManager manager) {
         if (manager == null || manager.isSkipping()) {
             if (plugin.isDebugMode() && manager != null && manager.isSkipping()) {
                  plugin.getLogger().info(() -> String.format("Skipping sleep check for %s in %s: Already skipping", player.getName(), world.getName()));
             } else if (plugin.isDebugMode() && manager == null) {
                 plugin.getLogger().info(() -> String.format("Skipping sleep check for %s in %s: World not managed", player.getName(), world.getName()));
             }
             return;
        }

        WorldConfig worldConfig = plugin.getWorldConfig(world);
        boolean isNight = isNightTime(world);
        boolean canSkipDueToThunder = world.isThundering() && worldConfig.isAllowThunderstormSleep();

        if (!isNight && !canSkipDueToThunder) {
            if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Sleep skip check aborted for %s in %s: Conditions no longer met (Night: %b, Thunder: %b, AllowThunder: %b)", player.getName(), world.getName(), isNight, world.isThundering(), worldConfig.isAllowThunderstormSleep()));
            }
            return;
        }

        int onlinePlayers = (int) world.getPlayers().stream().filter(p -> !p.isSleepingIgnored()).count();
        Set<UUID> currentSleeping = sleepingPlayers.getOrDefault(world.getUID(), Collections.emptySet());
        int sleepingCount = currentSleeping.size();

         if (onlinePlayers <= 0) {
              if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Sleep check aborted for %s in %s: No valid online players", player.getName(), world.getName()));
             }
             return;
         }

        if (onlinePlayers == 1 && sleepingCount == 1) {
            if (plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> "Single player detected and sleeping, skipping night/storm");
            }
            processSleepSkip(world, manager, 1, onlinePlayers);
            return;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
               "Sleep check - World: %s, Sleeping: %d, Online (valid): %d, Required %s: %.2f/%d",
               world.getName(), sleepingCount, onlinePlayers,
               plugin.isUseRequiredPlayers() ? "players" : "percentage",
               plugin.getSleepPercentage(), plugin.getRequiredPlayers()
            ));
        }

        boolean shouldSkip = false;
        if (plugin.isUseRequiredPlayers()) {
            int required = Math.min(plugin.getRequiredPlayers(), onlinePlayers);
            shouldSkip = sleepingCount >= required;
        } else {
            shouldSkip = onlinePlayers > 0 && (double) sleepingCount / onlinePlayers >= plugin.getSleepPercentage();
        }

        if (shouldSkip) {
            processSleepSkip(world, manager, sleepingCount, onlinePlayers);
        } else if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Sleep skip condition not met for world %s (%d/%d)", world.getName(), sleepingCount, onlinePlayers
            ));
        }
    }

    private void processSleepSkip(World world, WorldTimeManager manager, int sleepingCount, int onlinePlayers) {
        manager.skipToDay(); // This now handles weather clearing
        plugin.getMessageManager().broadcast(world, "sleep.skipped");
        sleepingPlayers.computeIfPresent(world.getUID(), (k, v) -> {
            v.clear(); // Clear the set for this world
            return v;
        });

        if (plugin.isDebugMode()) {
             plugin.getLogger().info(() -> String.format("Night/storm skipped in world '%s' (%d/%d players sleeping). Sleeping cache cleared.", world.getName(), sleepingCount, onlinePlayers));
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        World world = event.getPlayer().getWorld();
        sleepingPlayers.computeIfPresent(world.getUID(), (k, v) -> {
            v.remove(event.getPlayer().getUniqueId());
            return v;
        });
         if (plugin.isDebugMode()) {
             plugin.getLogger().info(() -> String.format("Player '%s' left bed in world '%s'", event.getPlayer().getName(), world.getName()));
         }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld(); // World they were in when they quit
        sleepingPlayers.computeIfPresent(world.getUID(), (k, v) -> {
             boolean removed = v.remove(player.getUniqueId());
             if (removed && plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Player '%s' quit while potentially sleeping in world '%s'; removed from cache.", player.getName(), world.getName()));
             }
            return v;
        });
        // No need to trigger a sleep check here, as the player leaving might *cause* the condition to be met/unmet later.
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        sleepingPlayers.computeIfPresent(fromWorld.getUID(), (k, v) -> {
             boolean removed = v.remove(player.getUniqueId());
             if (removed && plugin.isDebugMode()) {
                 plugin.getLogger().info(() -> String.format("Player '%s' changed world from '%s' while potentially sleeping; removed from cache.", player.getName(), fromWorld.getName()));
             }
            return v;
        });
        // Similar to quit, changing world might affect sleep counts, but no immediate check needed here.
    }
}