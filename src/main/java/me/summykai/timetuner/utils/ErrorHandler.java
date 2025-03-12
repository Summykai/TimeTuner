// ErrorHandler.java
package me.summykai.timetuner.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.logging.Level;

public class ErrorHandler {
    private static final String PREFIX = "[TimeTuner] ";

    public static void logSevere(String message) {
        Bukkit.getLogger().log(Level.SEVERE, PREFIX + message);
    }

    public static void logWarning(String message) {
        Bukkit.getLogger().log(Level.WARNING, PREFIX + message);
    }

    public static void logConfigError(String key, Object defaultValue) {
        logWarning("Invalid configuration value for " + key + 
                  ". Using default: " + defaultValue);
    }

    public static void logCommandError(CommandSender sender, String message) {
        sender.sendMessage(Component.text(PREFIX + message).color(NamedTextColor.RED));
        logWarning("Command error: " + message);
    }

    public static void logPluginError(String message, Throwable throwable) {
        Bukkit.getLogger().log(Level.SEVERE, PREFIX + message, throwable);
    }
}