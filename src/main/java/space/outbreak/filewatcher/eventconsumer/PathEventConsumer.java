package space.outbreak.filewatcher.eventconsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@AllArgsConstructor
public class PathEventConsumer extends EventConsumer {
    @Getter private final JavaPlugin plugin;
    @Getter private final Path path;
    @Getter private final Consumer<File> runnable;
    @Getter private final UUID uuid = UUID.randomUUID();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathEventConsumer)) return false;
        PathEventConsumer that = (PathEventConsumer) o;
        return getPlugin().equals(that.getPlugin()) && getPath().equals(that.getPath()) && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPlugin(), getPath(), uuid);
    }

    @Override
    public void matchAndRun(Path path, String filename, File changedFile) {
        if (getPath().equals(path))
            getRunnable().accept(changedFile);
    }
}
