package space.outbreak.filewatcher.eventconsumer;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public abstract class EventConsumer {
    public abstract void matchAndRun(Path path, String filename, File changedFile);

    public abstract Path getPath();

    public abstract JavaPlugin getPlugin();

    public abstract UUID getUuid();

    public String getName() {
        return getPlugin().getName() + " @ " + getPath() + " @ " + getUuid();
    }
}
