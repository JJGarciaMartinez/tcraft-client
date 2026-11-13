package hopsync.platform.fs;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Where Minecraft keeps its game folder when the player has not chosen one.
 */
public final class DefaultGameDir {

    private DefaultGameDir() {
    }

    public static Path detect() {
        return forSystem(System.getProperty("os.name"), System.getProperty("user.home"),
            Path.of(System.getProperty("user.dir")));
    }

    /**
     * When launched from inside a {@code mods} folder, its parent is the game folder; otherwise the
     * standard Minecraft location for the operating system.
     */
    static Path forSystem(String osName, String userHome, Path workingDir) {
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
