package me.summykai.timetuner.utils;

import me.summykai.timetuner.TimeTuner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages localized messages for the TimeTuner plugin
 */
public class MessageManager {
    private static final Component PREFIX = Component.text("[")
        .color(TextColor.color(170, 170, 170))
        .append(Component.text("TimeTuner")
            .color(TextColor.color(85, 255, 255)))
        .append(Component.text("] ")
            .color(TextColor.color(170, 170, 170)));

    private final TimeTuner plugin;
    private FileConfiguration messages;
    private final Pattern placeholderPattern = Pattern.compile("\\{([\\w.-]+)\\}");
    private final LegacyComponentSerializer legacySerializer;
    private final Map<String, Component> componentCache;
    private long lastCacheClean;
    private static final long CACHE_CLEANUP_INTERVAL = 300000; // 5 minutes

    public MessageManager(TimeTuner plugin) {
        this.plugin = plugin;
        this.legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
        this.componentCache = new HashMap<>();
        this.lastCacheClean = System.currentTimeMillis();
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        loadMessages();
        componentCache.clear();
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getFormattedMessage(path, placeholders);
        Component component = getCachedComponent(path, message);
        sender.sendMessage(component);
    }

    public void broadcast(String path) {
        broadcast(path, new HashMap<>());
    }

    public void broadcast(String path, Map<String, String> placeholders) {
        String message = getFormattedMessage(path, placeholders);
        Component component = getCachedComponent(path, message);
        plugin.getServer().broadcast(component);
    }

    public void broadcast(World world, String path) {
        broadcast(world, path, new HashMap<>());
    }

    public void broadcast(World world, String path, Map<String, String> placeholders) {
        String message = getFormattedMessage(path, placeholders);
        Component component = getCachedComponent(path, message);
        world.getPlayers().forEach(player -> player.sendMessage(component));
    }

    private Component getCachedComponent(String path, String message) {
        long now = System.currentTimeMillis();
        if (now - lastCacheClean > CACHE_CLEANUP_INTERVAL) {
            componentCache.clear();
            lastCacheClean = now;
        }

        return componentCache.computeIfAbsent(path + message, 
            k -> PREFIX.append(legacySerializer.deserialize(message)));
    }

    private String getFormattedMessage(String path, Map<String, String> placeholders) {
        String message = messages.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }

        if (!placeholders.isEmpty()) {
            Matcher matcher = placeholderPattern.matcher(message);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement = placeholders.getOrDefault(placeholder, matcher.group());
                matcher.appendReplacement(buffer, replacement);
            }
            matcher.appendTail(buffer);
            message = buffer.toString();
        }

        return message;
    }

    public void saveMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }

    // Command feedback utilities
    public void sendFeedback(CommandSender sender, String key) {
        String message = messages.getString(key);
        if (message != null) {
            Component component = getCachedComponent(key, message);
            sender.sendMessage(component);
        }
    }

    public void sendFeedback(CommandSender sender, String key, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be in pairs");
        }

        String message = messages.getString(key);
        if (message != null) {
            Map<String, String> placeholderMap = new HashMap<>();
            for (int i = 0; i < placeholders.length; i += 2) {
                placeholderMap.put(placeholders[i], placeholders[i + 1]);
            }
            message = getFormattedMessage(key, placeholderMap);
            Component component = getCachedComponent(key + placeholders.toString(), message);
            sender.sendMessage(component);
        }
    }
}
