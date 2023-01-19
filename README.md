# FileWatcher

Executes the commands when the files change. Also has an API.

## Configuration

### Example triggers.yml

triggers.yml allows you to configure the execution of console commands when files change.

- `%filename%` will be replaced with the name of the file
- `%path%` will be replaced with the path of the file

```yaml
example-file-listener:
  file: "plugins/FileWatcher/file.txt"
  commands:
    - "say File %filename% (%path%) changed"

example-directory-listener:
  directory: "plugins/FileWatcher"
  commands:
    - "say File %filename% (%path%) changed"
```

### Default config.yml

```yaml
# timeout for WatchService
poll-timeout-millis: 2000
# When text editor saves a file, the file's metadata is updated as well,
# resulting in a duplicate event.
# During this time, after event, new events will be ignored.
anti-duplicate-sleep-millis: 10
# Enables logs of registering and unregistering listeners
logging: true

# Auto reload configs of FileWatcher
auto-reload: true
```

## API

Watching for a single file

```
File configFile = new File(getDataFolder(), "config.yml");

EventConsumer eventConsumer = FileWatcherAPI.registerFile(this, configFile, file -> {
    getLogger().info("Changed file: " + file.toString());
});
```

Watching for all files in directory

```
File dataDir = getDataFolder();

EventConsumer eventConsumer = FileWatcherAPI.registerDirectory(this, dataDir, file -> {
    getLogger().info("Changed file: " + file.toString());
});
```

Unregistering watcher

```
FileWatcherAPI.unregister(eventConsumer);
```