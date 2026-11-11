package hopsync.service;

import hopsync.config.UserSettings;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Resolves the game folders HopSync works with: the folder chosen in settings, or the default
 * Minecraft installation when the player has not chosen one.
 */
public class FileSystemService {
    private final SettingsService settingsService;

    public FileSystemService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * The folder that contains {@code mods/} and {@code config/}.
     */
    public File getGameDir() {
        return settingsService.get()
            .map(UserSettings::gameDir)
            .orElseGet(FileSystemService::detectDefaultGameDir)
            .toFile();
    }

    public File getModsFolder() {
        return new File(getGameDir(), "mods");
    }

    public File getConfigFolder() {
        return new File(getGameDir(), "config");
    }

    /**
     * Returns the file where HopSync records which files it installed in this game folder.
     */
    public File getStateFile() {
        return new File(new File(getGameDir(), ".hopsync"), "state.json");
    }

    public static Path detectDefaultGameDir() {
        return defaultGameDir(System.getProperty("os.name"), System.getProperty("user.home"),
            Path.of(System.getProperty("user.dir")));
    }

    /**
     * When launched from inside a {@code mods} folder, its parent is the game folder; otherwise the
     * standard Minecraft location for the operating system.
     */
    static Path defaultGameDir(String osName, String userHome, Path workingDir) {
        Path cwd = workingDir.toAbsolutePath().normalize();
        if (cwd.getFileName() != null && cwd.getFileName().toString().equalsIgnoreCase("mods") && cwd.getParent() != null) {
            return cwd.getParent();
        }
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        Path home = Path.of(userHome);
        if (os.contains("win")) {
            return home.resolve("AppData").resolve("Roaming").resolve(".minecraft");
        } else if (os.contains("mac")) {
            return home.resolve("Library").resolve("Application Support").resolve("minecraft");
        }
        return home.resolve(".minecraft");
    }
}
