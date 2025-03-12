package me.summykai.timetuner;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import me.summykai.timetuner.time.*;
import me.summykai.timetuner.commands.*;
import me.summykai.timetuner.listeners.WorldListener;
import me.summykai.timetuner.utils.*;
import me.summykai.timetuner.utils.ErrorHandler;
import java.util.*;

public final class TimeTuner extends JavaPlugin {
    private final Map<UUID, WorldTimeManager> worldManagers = new HashMap<>();
    private double daySpeed = 1.0;
    private double nightSpeed = 1.0;
    private boolean timePaused = false;
    private boolean manualPaused = false;
    private boolean autoPaused = false;
    private MessageManager messageManager;
    private boolean debugMode = false;
    private int tickFrequency = 1;
    private BukkitTask timeUpdateTask;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            messageManager = new MessageManager(this);
            reloadConfigValues();
            initializeWorldManagers();
            startTimeUpdateTask();
            registerCommands();
            
            // Register world load listener
            getServer().getPluginManager().registerEvents(new WorldListener(this), this);
            
            // Always log configuration values at startup, regardless of debug mode
            getLogger().info("TimeTuner configuration loaded:");
            getLogger().info("Global Day speed: " + daySpeed);
            getLogger().info("Global Night speed: " + nightSpeed);
            getLogger().info("Tick frequency: " + tickFrequency);
            getLogger().info("Managed worlds: " + worldManagers.size());
            
            if (debugMode) {
                getLogger().info("Debug mode enabled");
            } else {
                getLogger().info("TimeTuner v" + getPluginMeta().getVersion() + " enabled!");
            }
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to enable plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (timeUpdateTask != null) {
                timeUpdateTask.cancel();
            }
            worldManagers.clear();
            getLogger().info("TimeTuner disabled.");
        } catch (Exception e) {
            ErrorHandler.logPluginError("Failed to disable plugin", e);
        }
    }

    public void initializeWorldManagers() {
        worldManagers.clear();
        for (World world : getServer().getWorlds()) {
            String worldName = world.getName();
            
            // Check if world is configured and enabled
            boolean enabled = getConfig().getBoolean("worlds." + worldName + ".enabled", false);
            if (!enabled) {
                if (debugMode) {
                    getLogger().info("Skipping disabled world: " + worldName);
                }
                continue;
            }
            
            // Get world-specific speeds from config, fallback to global speeds
            double worldDaySpeed = getConfig().getDouble("worlds." + worldName + ".day-speed", daySpeed);
            double worldNightSpeed = getConfig().getDouble("worlds." + worldName + ".night-speed", nightSpeed);
            
            worldManagers.put(world.getUID(), new WorldTimeManager(this, world, worldDaySpeed, worldNightSpeed));
            if (debugMode) {
                getLogger().info("Initialized time manager for world: " + worldName + 
                                " (day-speed: " + worldDaySpeed + ", night-speed: " + worldNightSpeed + ")");
            }
        }
    }

    private void startTimeUpdateTask() {
        if (timeUpdateTask != null) {
            timeUpdateTask.cancel();
        }
        
        timeUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    boolean shouldPause = getConfig().getBoolean("auto-pause-empty", false) 
                        && getServer().getOnlinePlayers().isEmpty();
                    
                    if (shouldPause != autoPaused) {
                        setAutoPaused(shouldPause);
                        if (debugMode) {
                            getLogger().info("Auto-pause " + (shouldPause ? "activated" : "deactivated") + 
                                              " due to " + (shouldPause ? "empty" : "active") + " server");
                        }
                    }
                    
                    worldManagers.values().forEach(WorldTimeManager::tick);
                } catch (Exception e) {
                    ErrorHandler.logPluginError("Error in time update task", e);
                }
            }
        }.runTaskTimer(this, 1L, tickFrequency);
        
        if (debugMode) {
            getLogger().info("Time update task started with frequency: " + tickFrequency);
        }
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this, messageManager);
        TimeTunerCommandExecutor executor = new TimeTunerCommandExecutor(this, commandManager);
        getCommand("timetuner").setExecutor(executor);
        getCommand("timetuner").setTabCompleter(executor);
    }

    public void reloadConfigValues() {
        reloadConfig();
        messageManager.loadMessages();
        
        // Store old values to detect changes
        double oldDaySpeed = daySpeed;
        double oldNightSpeed = nightSpeed;
        boolean oldDebugMode = debugMode;
        int oldTickFrequency = tickFrequency;
        
        // Load new values with validation
        double newDaySpeed = Math.max(0, getConfig().getDouble("day-speed", 1.0));
        double newNightSpeed = Math.max(0, getConfig().getDouble("night-speed", 1.0));
        
        // Check for NaN values
        if (Double.isNaN(newDaySpeed)) {
            newDaySpeed = 1.0;
            if (debugMode) {
                getLogger().warning("Invalid day-speed value (NaN) in config, using default: 1.0");
            }
        }
        if (Double.isNaN(newNightSpeed)) {
            newNightSpeed = 1.0;
            if (debugMode) {
                getLogger().warning("Invalid night-speed value (NaN) in config, using default: 1.0");
            }
        }
        
        daySpeed = newDaySpeed;
        nightSpeed = newNightSpeed;
        
        debugMode = getConfig().getBoolean("debug-mode", false);
        tickFrequency = getConfig().getInt("tick-frequency", 1);
        
        // Validate values
        validateSpeedValues();
        tickFrequency = Math.max(1, Math.min(20, tickFrequency));
        
        // Check if we need to restart the time update task due to tick frequency change
        if (oldTickFrequency != tickFrequency) {
            startTimeUpdateTask();
            if (debugMode) {
                getLogger().info("Tick frequency changed from " + oldTickFrequency + " to " + tickFrequency);
            }
        }
        
        // Log speed changes if in debug mode
        if (debugMode && (oldDaySpeed != daySpeed || oldNightSpeed != nightSpeed)) {
            getLogger().info("Global speeds changed - Day: " + oldDaySpeed + " → " + daySpeed + 
                           ", Night: " + oldNightSpeed + " → " + nightSpeed);
        }
        
        // Update all world managers with new global or world-specific speeds
        updateAllWorldManagerSpeeds();
        
        if (debugMode && !oldDebugMode) {
            getLogger().info("Debug mode enabled");
            getLogger().info("Day speed: " + daySpeed);
            getLogger().info("Night speed: " + nightSpeed);
            getLogger().info("Tick frequency: " + tickFrequency);
            getLogger().info("Managed worlds: " + worldManagers.size());
        }
    }

    /**
     * Updates the speeds for all world managers based on the current configuration.
     */
    public void updateAllWorldManagerSpeeds() {
        worldManagers.forEach((uuid, manager) -> {
            World world = manager.getWorld();
            String worldName = world.getName();
            
            // Get world-specific speeds from config, fallback to global speeds
            double worldDaySpeed = getConfig().getDouble("worlds." + worldName + ".day-speed", daySpeed);
            double worldNightSpeed = getConfig().getDouble("worlds." + worldName + ".night-speed", nightSpeed);
            
            // Update the manager with the new speeds
            manager.updateSpeeds(worldDaySpeed, worldNightSpeed);
            
            if (debugMode) {
                getLogger().info("Updated speeds for world " + worldName + ": day=" + worldDaySpeed + ", night=" + worldNightSpeed);
            }
        });
    }

    /**
     * Updates the global speed settings and propagates changes to all world managers.
     * 
     * @param newDaySpeed The new global day speed
     * @param newNightSpeed The new global night speed
     */
    public void updateGlobalSpeeds(double newDaySpeed, double newNightSpeed) {
        // Update config in memory
        getConfig().set("day-speed", newDaySpeed);
        getConfig().set("night-speed", newNightSpeed);
        
        // Save to disk
        saveConfig();
        
        // Update in-memory values
        daySpeed = newDaySpeed;
        nightSpeed = newNightSpeed;
        validateSpeedValues();
        
        // Update all world managers that don't have specific overrides
        updateAllWorldManagerSpeeds();
        
        if (debugMode) {
            getLogger().info("Updated global speeds: day=" + daySpeed + ", night=" + nightSpeed);
        }
    }

    public void updateWorldSpeed(String worldName, double daySpeed, double nightSpeed) {
        // Update config in memory
        if (getConfig().isConfigurationSection("worlds." + worldName)) {
            getConfig().set("worlds." + worldName + ".day-speed", daySpeed);
            getConfig().set("worlds." + worldName + ".night-speed", nightSpeed);
        } else {
            getConfig().set("worlds." + worldName + ".day-speed", daySpeed);
            getConfig().set("worlds." + worldName + ".night-speed", nightSpeed);
            getConfig().set("worlds." + worldName + ".enabled", true);
        }
        
        // Save changes to disk
        saveConfig();
        
        // Update the world manager immediately if it exists
        worldManagers.values().stream()
            .filter(manager -> manager.getWorld().getName().equals(worldName))
            .findFirst()
            .ifPresent(manager -> {
                manager.updateSpeeds(daySpeed, nightSpeed);
                
                if (debugMode) {
                    getLogger().info("Updated speeds for world " + worldName + ": day=" + daySpeed + ", night=" + nightSpeed);
                }
            });
    }

    private void validateSpeedValues() {
        daySpeed = Math.max(0, daySpeed);
        nightSpeed = Math.max(0, nightSpeed);
    }

    public double getDaySpeed() { return daySpeed; }
    public double getNightSpeed() { return nightSpeed; }
    public int getTickFrequency() { return tickFrequency; }
    public MessageManager getMessageManager() { return messageManager; }
    public boolean isDebugMode() { return debugMode; }

    public boolean isTimePaused() { return timePaused; }
    
    public void setTimePaused(boolean paused) {
        setManualPaused(paused);
    }
    
    public void setManualPaused(boolean paused) {
        this.manualPaused = paused;
        updatePausedState();
    }
    
    public void setAutoPaused(boolean paused) {
        this.autoPaused = paused;
        updatePausedState();
    }
    
    private void updatePausedState() {
        boolean newPaused = manualPaused || autoPaused;
        if (newPaused != timePaused) {
            timePaused = newPaused;
            worldManagers.values().forEach(manager -> manager.setPaused(timePaused));
        }
    }

    public Map<UUID, WorldTimeManager> getWorldManagers() {
        return Collections.unmodifiableMap(worldManagers);
    }

    public void resetWorldTimes() {
        worldManagers.values().forEach(WorldTimeManager::reset);
    }
    
    /**
     * Removes a world manager for the specified world UUID.
     * Used when a world is unloaded to prevent memory leaks.
     * 
     * @param worldUID The UUID of the world to remove
     */
    public void removeWorldManager(UUID worldUID) {
        worldManagers.remove(worldUID);
        if (debugMode) {
            getLogger().info("Removed world manager for unloaded world with UUID: " + worldUID);
        }
    }
}