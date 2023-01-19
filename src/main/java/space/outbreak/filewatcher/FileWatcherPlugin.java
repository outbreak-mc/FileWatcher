package space.outbreak.filewatcher;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import space.outbreak.filewatcher.eventconsumer.EventConsumer;
import space.outbreak.filewatcher.eventconsumer.PathEventConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FileWatcherPlugin extends JavaPlugin {
    private FileWatcherRunnable watcherRunnable;
    private BukkitTask task;
    private final List<EventConsumer> triggersEventConsumers = new ArrayList<>();
    private FileConfiguration triggersConfig;

    private EventConsumer configAutoReloadListener;
    private EventConsumer triggersAutoReloadListener;

    private static final String TRIGGERS_FILENAME = "triggers.yml";

    public FileWatcherRunnable getWatcherRunnable() {
        return watcherRunnable;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        triggersConfig = getCustomFileConfig(TRIGGERS_FILENAME);

        reloadWatcher();
        loadTriggers();
        FileWatcherAPI.init(this);

        reloadAutoReloadingTask();

        getCommand("filewatcherreload").setExecutor(new FileWatcherReloadCommand());
    }

    void reloadWatcher() {
        if (watcherRunnable != null)
            watcherRunnable.cancel();
        if (task != null)
            task.cancel();

        watcherRunnable = new FileWatcherRunnable(this);
        task = Bukkit.getScheduler().runTaskAsynchronously(this, watcherRunnable);
        FileWatcherAPI.init(this);
        getLogger().info("File watcher task created");
    }

    void reloadAutoReloadingTask() {
        if (getConfig().getBoolean("auto-reload")) {
            if (configAutoReloadListener == null) {
                File configFile = new File(getDataFolder(), "config.yml");
                configAutoReloadListener = FileWatcherAPI.registerFile(this, configFile, (f) -> reload());
            }
            if (triggersAutoReloadListener == null) {
                File triggersConfigFile = new File(getDataFolder(), TRIGGERS_FILENAME);
                triggersAutoReloadListener = FileWatcherAPI.registerFile(this, triggersConfigFile, (f) -> reload());
            }
        }
        else {
            if (configAutoReloadListener != null) {
                FileWatcherAPI.unregister(configAutoReloadListener);
                configAutoReloadListener = null;
            }
            if (triggersAutoReloadListener != null) {
                FileWatcherAPI.unregister(triggersAutoReloadListener);
                triggersAutoReloadListener = null;
            }
        }
    }

    private FileConfiguration getCustomFileConfig(String name) {
        if (!name.endsWith(".yml"))
            name = name + ".yml";
        File customConfigFile = new File(getDataFolder(), name);
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(customConfigFile);
    }

    void loadTriggers() {
        for (EventConsumer eventConsumer : triggersEventConsumers)
            FileWatcherAPI.unregister(eventConsumer);
        triggersEventConsumers.clear();

        for (String key : triggersConfig.getKeys(false)) {
            ConfigurationSection section = triggersConfig.getConfigurationSection(key);
            if (section == null)
                continue;

            String filepath = section.getString("file");
            String dirpath = section.getString("directory");
            List<String> commands = section.getStringList("commands");

            if (filepath != null)
            {
                File file = new File(filepath);
                if (!file.exists()) {
                    getLogger().severe(ChatColor.RED + TRIGGERS_FILENAME + ": File " + file + " does not exist!");
                    continue;
                }

                triggersEventConsumers.add(FileWatcherAPI.registerFile(this, file, (f) -> {
                    for (String cmd : commands) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                                cmd.replace(
                                "%filename%", f.getName()
                                ).replace(
                                        "%path%", f.getPath()
                                )
                        );
                    }
                }));
            }
            else if (dirpath != null)
            {
                File dir = new File(dirpath);
                if (!dir.exists()) {
                    getLogger().severe(ChatColor.RED + TRIGGERS_FILENAME + ": Directory " + dir + " does not exist!");
                }
                triggersEventConsumers.add(FileWatcherAPI.registerDirectory(this, dir, (f) -> {
                    for (String cmd : commands) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                                cmd.replace(
                                        "%filename%", f.getName()
                                ).replace(
                                        "%path%", f.getPath()
                                )
                        );
                    }
                }));
            }
        }
    }

    void reload() {
        getLogger().info("Reloading...");
        reloadConfig();
        triggersConfig = getCustomFileConfig(TRIGGERS_FILENAME);

        List<EventConsumer> eventConsumers = new ArrayList<>();
        if (watcherRunnable != null) {
            eventConsumers = watcherRunnable.getEventConsumers();
        }

        reloadWatcher();
        getLogger().info("Reconnecting listeners...");

        for (EventConsumer consumer : eventConsumers) {
            watcherRunnable.register(consumer);
            if (consumer instanceof PathEventConsumer)
                getLogger().info(" - " + consumer.getName());
        }

        loadTriggers();

        FileWatcherAPI.init(this);

        reloadAutoReloadingTask();

        getLogger().info("Configuration reloaded");
    }

    @Override
    public void onDisable() {
        if (watcherRunnable != null)
            watcherRunnable.cancel();
        if (task != null)
            task.cancel();
    }
}
