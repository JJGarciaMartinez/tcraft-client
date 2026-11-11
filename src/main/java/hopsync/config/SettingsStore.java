package hopsync.config;

import hopsync.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Persists {@link UserSettings} as JSON in the operating system's per-user configuration folder.
 */
public class SettingsStore {
    private static final int FORMAT = 1;

    private final Path file;

    public SettingsStore(Path file) {
        this.file = file;
    }

    /**
     * Where settings live for the current user.
     */
    public static SettingsStore forCurrentUser() {
        return new SettingsStore(defaultLocation(System.getProperty("os.name"), System.getenv(),
            System.getProperty("user.home")));
    }

    /**
     * Resolves the settings file following each platform's convention:
     * {@code %APPDATA%\HopSync} on Windows, {@code ~/Library/Application Support/HopSync} on macOS and
     * {@code $XDG_CONFIG_HOME/hopsync} (default {@code ~/.config/hopsync}) elsewhere.
     */
    static Path defaultLocation(String osName, Map<String, String> env, String userHome) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        Path home = Path.of(userHome);
        Path dir;
        if (os.contains("win")) {
            String appData = env.get("APPDATA");
            dir = (isUsable(appData) ? Path.of(appData) : home.resolve("AppData").resolve("Roaming")).resolve("HopSync");
        } else if (os.contains("mac")) {
            dir = home.resolve("Library").resolve("Application Support").resolve("HopSync");
        } else {
            String xdg = env.get("XDG_CONFIG_HOME");
            // The XDG spec says relative values must be ignored
            dir = (isUsable(xdg) && Path.of(xdg).isAbsolute() ? Path.of(xdg) : home.resolve(".config")).resolve("hopsync");
        }
        return dir.resolve("settings.json");
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }

    public Path getFile() {
        return file;
    }

    /**
     * Returns the saved settings, or empty if there are none yet or they cannot be read.
     */
    public Optional<UserSettings> load() {
        try {
            Map<?, ?> root = (Map<?, ?>) Json.parse(Files.readString(file, StandardCharsets.UTF_8));
            if (!Long.valueOf(FORMAT).equals(root.get("format"))) {
                return Optional.empty();
            }
            String url = (String) root.get("manifest_url");
            String gameDir = (String) root.get("game_dir");
            if (url == null || url.isBlank() || gameDir == null || gameDir.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new UserSettings(url, Path.of(gameDir)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | ClassCastException | InvalidPathException e) {
            System.err.println("Warning: ignoring unreadable settings " + file + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(UserSettings settings) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", FORMAT);
        root.put("manifest_url", settings.manifestUrl());
        root.put("game_dir", settings.gameDir().toAbsolutePath().toString());

        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, Json.write(root), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
