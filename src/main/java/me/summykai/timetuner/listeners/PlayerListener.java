package me.summykai.timetuner.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import me.summykai.timetuner.TimeTuner;
import me.summykai.timetuner.time.Time;
import me.summykai.timetuner.time.WorldTimeManager;

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
        if (!isValidSleepAttempt(event)) {
            return;
        }

        World world = event.getPlayer().getWorld();
        WorldTimeManager manager = plugin.getWorldManagers().get(world.getUID());
        
        if (!isValidWorldState(manager, world)) {
            return;
        }

        if (!isNightTime(world)) {
            return;
        }

        // Add player to sleeping cache
        sleepingPlayers.computeIfAbsent(world.getUID(), k -> new HashSet<>())
                      .add(event.getPlayer().getUniqueId());

        checkAndProcessSleepSkip(event.getPlayer(), world, manager);
    }

    private boolean isValidSleepAttempt(PlayerBedEnterEvent event) {
        if (!plugin.isAllowSleepSkip()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> "Sleep skip disabled in config");
            }
            return false;
        }

        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> String.format(
                    "Invalid bed enter result: %s",
                    event.getBedEnterResult()
                ));
            }
            return false;
        }
        return true;
    }

    private boolean isValidWorldState(WorldTimeManager manager, World world) {
        if (manager == null || manager.isSkipping()) {
            if (plugin.isDebugMode()) {
                String reason = manager == null ? "world not managed" : "already skipping";
                plugin.getLogger().info(() -> String.format(
                    "Skipping bed enter event: %s",
                    reason
                ));
            }
            return false;
        }
        return true;
    }

    private boolean isNightTime(World world) {
        Time currentTime = Time.fromWorldTime(world.getTime());
        Time nightStart = new Time(Time.NIGHT_START);
        Time dayStart = new Time(Time.DAY_START);
        
        if (!currentTime.between(nightStart, dayStart)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> String.format(
                    "Not night time in world %s (time: %d)",
                    world.getName(),
                    world.getTime()
                ));
            }
            return false;
        }
        return true;
    }

    private void checkAndProcessSleepSkip(Player player, World world, WorldTimeManager manager) {
        int onlinePlayers = world.getPlayers().size();
        if (onlinePlayers == 1) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(() -> "Single player detected, skipping night");
            }
            processSleepSkip(world, manager, 1, onlinePlayers);
            return;
        }

        int sleepingCount = sleepingPlayers.getOrDefault(world.getUID(), Collections.emptySet()).size();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Sleep check - World: %s, Sleeping: %d, Online: %d",
                world.getName(),
                sleepingCount,
                onlinePlayers
            ));
        }

        boolean shouldSkip = plugin.isUseRequiredPlayers() 
            ? sleepingCount >= plugin.getRequiredPlayers()
            : (double) sleepingCount / onlinePlayers >= plugin.getSleepPercentage();

        if (shouldSkip) {
            processSleepSkip(world, manager, sleepingCount, onlinePlayers);
        }
    }

    private void processSleepSkip(World world, WorldTimeManager manager, int sleepingCount, int onlinePlayers) {
        manager.skipToDay();
        plugin.getMessageManager().broadcast(world, "sleep.skipped");
        sleepingPlayers.get(world.getUID()).clear();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Night skipped in world '%s' (%d/%d players sleeping)",
                world.getName(),
                sleepingCount,
                onlinePlayers
            ));
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        World world = event.getPlayer().getWorld();
        sleepingPlayers.getOrDefault(world.getUID(), Collections.emptySet())
                      .remove(event.getPlayer().getUniqueId());
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Player '%s' left bed in world '%s'",
                event.getPlayer().getName(),
                world.getName()
            ));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        sleepingPlayers.getOrDefault(world.getUID(), Collections.emptySet())
                      .remove(player.getUniqueId());
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Player '%s' quit while sleeping in world '%s'",
                player.getName(),
                world.getName()
            ));
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        sleepingPlayers.getOrDefault(fromWorld.getUID(), Collections.emptySet())
                      .remove(player.getUniqueId());
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(() -> String.format(
                "Player '%s' changed world from '%s' while sleeping",
                player.getName(),
                fromWorld.getName()
            ));
        }
    }
}