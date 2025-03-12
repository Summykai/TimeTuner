package me.summykai.timetuner.utils;

import me.summykai.timetuner.TimeTuner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private final TimeTuner plugin;
    private FileConfiguration messages;
    private final Pattern placeholderPattern = Pattern.compile("\\{([\\w.-]+)\\}");
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    
    public MessageManager(TimeTuner plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Loads or reloads the messages from the messages.yml file
     */
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Gets a raw message string from the messages file
     * 
     * @param path The path to the message
     * @return The message string or a default error message if not found
     */
    public String getRawMessage(String path) {
        String message = messages.getString(path);
        if (message == null) {
            return "§cMissing message: " + path;
        }
        return message;
    }
    
    /**
     * Gets a formatted message with placeholders replaced
     * 
     * @param path The path to the message
     * @param placeholders Map of placeholders and their values
     * @return The formatted message
     */
    public String getFormattedMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = placeholders.getOrDefault(placeholder, matcher.group());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Sends a message to a command sender
     * 
     * @param sender The recipient of the message
     * @param path The path to the message
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }
    
    /**
     * Sends a message with placeholders to a command sender
     * 
     * @param sender The recipient of the message
     * @param path The path to the message
     * @param placeholders Map of placeholders and their values
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = getRawMessage("plugin.prefix");
        String message = getFormattedMessage(path, placeholders);
        
        // Convert legacy format to Adventure components
        Component component = legacySerializer.deserialize(prefix + message);
        sender.sendMessage(component);
    }
    
    /**
     * Creates a placeholder map with the given key-value pairs
     * 
     * @param keyValues Key-value pairs in the format key1, value1, key2, value2, etc.
     * @return A map of placeholders
     */
    public Map<String, String> placeholders(String... keyValues) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                placeholders.put(keyValues[i], keyValues[i + 1]);
            }
        }
        return placeholders;
    }
    
    /**
     * Saves updated messages to the messages.yml file
     */
    public void saveMessages() {
        try {
            File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
}
