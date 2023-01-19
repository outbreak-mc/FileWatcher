package space.outbreak.filewatcher;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class FileWatcherReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        FileWatcherAPI.reload();
        sender.sendMessage("FileWatcher reloaded");
        return true;
    }
}
