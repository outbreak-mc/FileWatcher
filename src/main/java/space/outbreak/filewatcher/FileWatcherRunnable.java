package space.outbreak.filewatcher;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import space.outbreak.filewatcher.eventconsumer.EventConsumer;
import space.outbreak.filewatcher.eventconsumer.FileEventConsumer;
import space.outbreak.filewatcher.eventconsumer.PathEventConsumer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class FileWatcherRunnable implements Runnable {
    @Getter List<EventConsumer> eventConsumers = new ArrayList<>();
    Map<WatchKey, Path> keyToPathMap = new HashMap<>();

    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final FileWatcherPlugin plugin;

    private final int pollMillis;
    private final int antiDuplicateSleepMillis;

    private final @NotNull WatchService watcher;

    FileWatcherRunnable(FileWatcherPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        pollMillis = config.getInt("poll-timeout-millis", 2000);
        antiDuplicateSleepMillis = config.getInt("anti-duplicate-sleep-millis", 10);

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void cancel() throws IllegalStateException {
        stop.set(true);
        try {
            watcher.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isCancelled() throws IllegalStateException {
        return stop.get();
    }

    private synchronized void onChange(WatchKey key, String filename) {
        Path dirChanged = keyToPathMap.get(key);

        if (dirChanged != null) {
            File file = new File(dirChanged.toFile(), filename);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (EventConsumer eventConsumer : eventConsumers) {
                    eventConsumer.matchAndRun(dirChanged, filename, file);
                }
            });
        }
    }

    private void registerPath(Path path) {
        try {
            if (keyToPathMap.containsValue(path))
                return;
            WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            keyToPathMap.put(key, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void cleanUp() {
        keyToPathMap.entrySet().removeIf(entry -> {
            boolean used = false;
            for (EventConsumer eventConsumer : eventConsumers) {
                if (eventConsumer.getPath().equals(entry.getValue())) {
                    used = true;
                    break;
                }
            }
            if (!used)
                entry.getKey().cancel();
            return !used;
        });
    }

    void register(EventConsumer eventConsumer) {
        if (eventConsumer instanceof FileEventConsumer) {
            registerFile((FileEventConsumer) eventConsumer);
        } else if (eventConsumer instanceof PathEventConsumer) {
            registerDirectory((PathEventConsumer) eventConsumer);
        }
    }

    FileEventConsumer registerFile(FileEventConsumer eventConsumer) {
        registerPath(eventConsumer.getPath());
        eventConsumers.add(eventConsumer);
        return eventConsumer;
    }

    PathEventConsumer registerDirectory(PathEventConsumer eventConsumer) {
        registerPath(eventConsumer.getPath());
        eventConsumers.add(eventConsumer);
        return eventConsumer;
    }

    boolean unregister(EventConsumer eventConsumer) {
        boolean result = eventConsumers.remove(eventConsumer);
        cleanUp();
        return result;
    }

    private synchronized void startWatching() {
        stop.set(false);
        try {
            while (!isCancelled()) {
                WatchKey key;
                try { key = watcher.poll(pollMillis, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { return; }
                catch (ClosedWatchServiceException e) { stop.set(true); return; }

                if (key == null) { continue; }
                //noinspection BusyWait
                Thread.sleep(antiDuplicateSleepMillis);

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        onChange(key, filename.toString());
                    }
                    boolean valid = key.reset();
                    if (!valid) { break; }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        startWatching();
    }
}
