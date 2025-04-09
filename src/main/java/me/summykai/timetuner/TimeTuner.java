package me.summykai.timetuner;

import me.summykai.timetuner.commands.CommandManager;
import me.summykai.timetuner.commands.TimeTunerCommandExecutor;
import me.summykai.timetuner.listeners.PlayerListener;
import me.summykai.timetuner.time.WorldTimeManager;
import me.summykai.timetuner.utils.MessageManager;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimeTuner extends JavaPlugin {
    private final Map<UUID, WorldTimeManager> worldManagers;
    private final Map<String, WorldConfig> worldConfigs;
    private double daySpeed;
    private double nightSpeed;
    private boolean debugMode;
    private boolean allowSleepSkip;
    private double sleepPercentage;
    private boolean useRequiredPlayers;
    private int requiredPlayers;
    private int tickFrequency;
    private boolean autoPauseEmpty;
    private long lastConfigReload;
    private static final long CONFIG_RELOAD_COOLDOWN = 1000; // 1 second cooldown

    private MessageManager messageManager;
    private CommandManager commandManager;

    public TimeTuner() {
        this.worldManagers = new ConcurrentHashMap<>();
        this.worldConfigs = new ConcurrentHashMap<>();
        this.lastConfigReload = 0;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messageManager = new MessageManager(this);
        commandManager = new CommandManager(this, messageManager);

        TimeTunerCommandExecutor executor = new TimeTunerCommandExecutor(this, commandManager);
        getCommand("timetuner").setExecutor(executor);
        getCommand("timetuner").setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        loadConfigValues();
        initializeWorldManagers();

        // Start time update task
        new BukkitRunnable() {
            @Override
            public void run() {
                worldManagers.values().forEach(WorldTimeManager::updateTime);
            }
        }.runTaskTimer(this, 0L, tickFrequency);
    }

    public void loadConfigValues() {
        reloadConfig();
        
        // Load global speed settings
        ConfigurationSection globalSpeedsSection = getConfig().getConfigurationSection("global-speeds");
        if (globalSpeedsSection != null) {
            daySpeed = globalSpeedsSection.getDouble("day-speed", 0.5);
            nightSpeed = globalSpeedsSection.getDouble("night-speed", 1.0);
        } else {
            // Fallback for backward compatibility
            daySpeed = getConfig().getDouble("day-speed", 0.5);
            nightSpeed = getConfig().getDouble("night-speed", 1.0);
        }
        
        // Load sleep settings
        ConfigurationSection sleepSection = getConfig().getConfigurationSection("sleep");
        if (sleepSection != null) {
            allowSleepSkip = sleepSection.getBoolean("allow-skip", true);
            sleepPercentage = sleepSection.getDouble("percentage", 0.50);
            useRequiredPlayers = sleepSection.getBoolean("use-required-players", false);
            requiredPlayers = sleepSection.getInt("required-players", 3);
        } else {
            // Fallback for backward compatibility
            allowSleepSkip = getConfig().getBoolean("allow-sleep-skip", true);
            sleepPercentage = getConfig().getDouble("sleep-percentage", 0.50);
            useRequiredPlayers = getConfig().getBoolean("use-required-players", false);
            requiredPlayers = getConfig().getInt("required-players", 3);
        }
        
        // Load advanced settings
        ConfigurationSection advancedSection = getConfig().getConfigurationSection("advanced");
        if (advancedSection != null) {
            tickFrequency = Math.max(1, advancedSection.getInt("tick-frequency", 1));
            debugMode = advancedSection.getBoolean("debug-mode", false);
            autoPauseEmpty = advancedSection.getBoolean("auto-pause-empty", false);
        } else {
            // Fallback for backward compatibility
            tickFrequency = Math.max(1, getConfig().getInt("tick-frequency", 1));
            debugMode = getConfig().getBoolean("debug-mode", false);
            autoPauseEmpty = getConfig().getBoolean("auto-pause-empty", false);
        }

        // Load world-specific configurations
        ConfigurationSection worldsSection = getConfig().getConfigurationSection("worlds");
        worldConfigs.clear();

        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldConfig config = new WorldConfig(
                        worldSection.getDouble("day-speed", daySpeed),
                        worldSection.getDouble("night-speed", nightSpeed),
                        worldSection.getBoolean("enabled", true),
                        worldSection.getBoolean("allow-bed-explosions", false),
                        worldSection.getBoolean("allow-thunderstorm-sleep", true)
                    );
                    worldConfigs.put(worldName.toLowerCase(), config);
                }
            }
        }

        if (debugMode) {
            getLogger().info(() -> String.format(
                "Loaded configuration - Day Speed: %.2f, Night Speed: %.2f, Sleep Skip: %b",
                daySpeed, nightSpeed, allowSleepSkip
            ));
        }
    }

    public void reloadConfigValues() {
        long now = System.currentTimeMillis();
        if (now - lastConfigReload < CONFIG_RELOAD_COOLDOWN) {
            if (debugMode) {
                getLogger().info("Config reload skipped due to cooldown");
            }
            return;
        }

        // Preserve paused states
        Map<UUID, Boolean> pausedStates = new HashMap<>();
        worldManagers.forEach((id, manager) -> pausedStates.put(id, manager.isPaused()));

        loadConfigValues();
        messageManager.reloadMessages();

        // Update existing world managers with new config values
        worldManagers.forEach((id, manager) -> {
            World world = manager.getWorld();
            WorldConfig config = getWorldConfig(world);
            manager.updateSpeeds(config.getDaySpeed(), config.getNightSpeed());
            
            // Reapply paused states
            if (pausedStates.containsKey(id)) {
                manager.setPaused(pausedStates.get(id));
            }
        });

        lastConfigReload = now;
    }

    public void initializeWorldManagers() {
        // Remove managers for unloaded worlds
        worldManagers.entrySet().removeIf(entry -> getServer().getWorld(entry.getKey()) == null);

        // Initialize or update managers for loaded worlds
        for (World world : getServer().getWorlds()) {
            initializeWorldManager(world);
        }

        if (debugMode) {
            getLogger().info(() -> String.format(
                "Initialized %d world managers with global defaults - Day: %.2f, Night: %.2f",
                worldManagers.size(), daySpeed, nightSpeed
            ));
        }
    }

    public void initializeWorldManager(World world) {
        if (world == null) {
            return;
        }

        UUID worldId = world.getUID();
        WorldTimeManager existingManager = worldManagers.get(worldId);

        WorldConfig config = getWorldConfig(world);
        if (!config.isEnabled()) {
            if (existingManager != null) {
                worldManagers.remove(worldId);
                if (debugMode) {
                    getLogger().info(() -> "Removed manager for disabled world: " + world.getName());
                }
            }
            return;
        }

        if (existingManager != null) {
            existingManager.updateSpeeds(config.getDaySpeed(), config.getNightSpeed());
            if (debugMode) {
                getLogger().info(() -> String.format(
                    "Updated manager for world %s - Day: %.2f, Night: %.2f",
                    world.getName(), config.getDaySpeed(), config.getNightSpeed()
                ));
            }
        } else {
            // Disable vanilla daylight cycle before creating our manager
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            
            // Initialize with configured speeds
            WorldTimeManager manager = new WorldTimeManager(this, world, config.getDaySpeed(), config.getNightSpeed());
            worldManagers.put(worldId, manager);
            
            if (debugMode) {
                getLogger().info(() -> String.format(
                    "Created manager for world %s - Day: %.2f, Night: %.2f, Initial time: %d",
                    world.getName(), config.getDaySpeed(), config.getNightSpeed(), world.getTime()
                ));
            }
        }
    }

    public WorldConfig getWorldConfig(World world) {
        return worldConfigs.getOrDefault(
            world.getName().toLowerCase(),
            new WorldConfig(daySpeed, nightSpeed, true, false, true)
        );
    }

    public void resetWorldTimes() {
        worldManagers.values().forEach(WorldTimeManager::skipToDay);
    }

    public Map<UUID, WorldTimeManager> getWorldManagers() {
        return worldManagers;
    }

    public double getDaySpeed() {
        return daySpeed;
    }

    public double getNightSpeed() {
        return nightSpeed;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isAllowSleepSkip() {
        return allowSleepSkip;
    }

    public double getSleepPercentage() {
        return sleepPercentage;
    }

    public boolean isUseRequiredPlayers() {
        return useRequiredPlayers;
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public boolean isAutoPauseEmpty() {
        return autoPauseEmpty;
    }

    public boolean isOverflowProtection() {
        // Check safety section first, then fall back to old format
        ConfigurationSection safetySection = getConfig().getConfigurationSection("safety");
        if (safetySection != null) {
            return safetySection.getBoolean("overflow-protection", true);
        }
        return getConfig().getBoolean("overflow-protection", true);
    }

    public int getTickFrequency() {
        return tickFrequency;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public void updateGlobalSpeeds(double newDaySpeed, double newNightSpeed) {
        this.daySpeed = newDaySpeed;
        this.nightSpeed = newNightSpeed;
        worldManagers.values().forEach(manager -> 
            manager.updateSpeeds(newDaySpeed, newNightSpeed)
        );
    }

    public static final class WorldConfig {
        private final double daySpeed;
        private final double nightSpeed;
        private final boolean enabled;
        private final boolean allowBedExplosions;
        private final boolean allowThunderstormSleep;

        private WorldConfig(double daySpeed, double nightSpeed, boolean enabled, boolean allowBedExplosions, boolean allowThunderstormSleep) {
            this.daySpeed = daySpeed;
            this.nightSpeed = nightSpeed;
            this.enabled = enabled;
            this.allowBedExplosions = allowBedExplosions;
            this.allowThunderstormSleep = allowThunderstormSleep;
        }

        private double getDaySpeed() {
            return daySpeed;
        }

        private double getNightSpeed() {
            return nightSpeed;
        }

        private boolean isEnabled() {
            return enabled;
        }

        public boolean isAllowBedExplosions() {
            return allowBedExplosions;
        }

        public boolean isAllowThunderstormSleep() {
            return allowThunderstormSleep;
        }
    }
}