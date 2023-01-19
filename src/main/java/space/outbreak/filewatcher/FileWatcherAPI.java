package space.outbreak.filewatcher;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import space.outbreak.filewatcher.eventconsumer.EventConsumer;
import space.outbreak.filewatcher.eventconsumer.FileEventConsumer;
import space.outbreak.filewatcher.eventconsumer.PathEventConsumer;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class FileWatcherAPI {
    private static FileWatcherPlugin plugin;
    private static boolean logging;
    private static Logger logger;
    private static FileWatcherRunnable runnable;

    static void init(FileWatcherPlugin plugin) {
        FileWatcherAPI.plugin = plugin;
        FileWatcherAPI.logging = plugin.getConfig().getBoolean("logging");
        FileWatcherAPI.logger = plugin.getLogger();
        FileWatcherAPI.runnable = plugin.getWatcherRunnable();
    }

    public static FileEventConsumer registerFile(JavaPlugin plugin, File file, Consumer<File> callback) {
        FileEventConsumer eventConsumer = new FileEventConsumer(plugin, file, callback);
        if (logging)
            logger.info("Listener registered: " + eventConsumer.getName());
        return runnable.registerFile(eventConsumer);
    }

    public static PathEventConsumer registerDirectory(JavaPlugin plugin, File dir, Consumer<File> callback) {
        PathEventConsumer eventConsumer = new PathEventConsumer(plugin, dir.toPath(), callback);
        if (logging)
            logger.info("Listener registered: " + eventConsumer.getName());
        return runnable.registerDirectory(eventConsumer);
    }

    public static boolean unregister(EventConsumer eventConsumer) {
        boolean success = runnable.unregister(eventConsumer);
        if (logging) {
            if (success)
                logger.info("Listener unregistered: " + eventConsumer.getName());
            else
                logger.severe(ChatColor.RED + "Failed to unregister listener " + eventConsumer.getName());
        }
        return success;
    }

    public static void reload() {
        plugin.reload();
    }
}
