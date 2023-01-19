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
public class FileEventConsumer extends EventConsumer {
    @Getter private final JavaPlugin plugin;
    @Getter private final File file;
    @Getter private final Consumer<File> runnable;
    @Getter private final UUID uuid = UUID.randomUUID();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEventConsumer)) return false;
        FileEventConsumer that = (FileEventConsumer) o;
        return getPlugin().equals(that.getPlugin()) && getFile().equals(that.getFile()) && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPlugin(), getFile(), uuid);
    }

    @Override
    public void matchAndRun(Path path, String filename, File changedFile) {
        if (getFile().equals(changedFile))
            getRunnable().accept(changedFile);
    }

    @Override
    public Path getPath() {
        return file.toPath().getParent();
    }
}
