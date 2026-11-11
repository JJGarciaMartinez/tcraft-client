package hopsync.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SettingsStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTrips() throws IOException {
        SettingsStore store = new SettingsStore(dir.resolve("nested/settings.json"));
        UserSettings settings = new UserSettings("https://example.org/manifest.json", dir.resolve("My Instance [1.21]"));

        store.save(settings);

        assertEquals(Optional.of(settings), store.load());
        assertFalse(Files.exists(dir.resolve("nested/settings.json.tmp")));
    }

    @Test
    void missingOrCorruptFileMeansNoSettings() throws IOException {
        Path file = dir.resolve("settings.json");
        assertEquals(Optional.empty(), new SettingsStore(file).load());

        Files.writeString(file, "not json");
        assertEquals(Optional.empty(), new SettingsStore(file).load());

        Files.writeString(file, "{\"format\": 1, \"manifest_url\": 3, \"game_dir\": \"/x\"}");
        assertEquals(Optional.empty(), new SettingsStore(file).load());

        Files.writeString(file, "{\"format\": 2, \"manifest_url\": \"https://x\", \"game_dir\": \"/x\"}");
        assertEquals(Optional.empty(), new SettingsStore(file).load());

        Files.writeString(file, "{\"format\": 1, \"manifest_url\": \"\", \"game_dir\": \"/x\"}");
        assertEquals(Optional.empty(), new SettingsStore(file).load());
    }

    @Test
    void windowsUsesAppData() {
        assertEquals(Path.of("C:/Users/p/AppData/Roaming/HopSync/settings.json"),
            SettingsStore.defaultLocation("Windows 11", Map.of("APPDATA", "C:/Users/p/AppData/Roaming"), "C:/Users/p"));
        assertEquals(Path.of("/home/p/AppData/Roaming/HopSync/settings.json"),
            SettingsStore.defaultLocation("Windows 10", Map.of(), "/home/p"));
    }

    @Test
    void macUsesApplicationSupport() {
        assertEquals(Path.of("/Users/p/Library/Application Support/HopSync/settings.json"),
            SettingsStore.defaultLocation("Mac OS X", Map.of(), "/Users/p"));
    }

    @Test
    void linuxFollowsXdg() {
        assertEquals(Path.of("/home/p/.config/hopsync/settings.json"),
            SettingsStore.defaultLocation("Linux", Map.of(), "/home/p"));
        assertEquals(Path.of("/xdg/hopsync/settings.json"),
            SettingsStore.defaultLocation("Linux", Map.of("XDG_CONFIG_HOME", "/xdg"), "/home/p"));
        assertEquals(Path.of("/home/p/.config/hopsync/settings.json"),
            SettingsStore.defaultLocation("Linux", Map.of("XDG_CONFIG_HOME", "relative/dir"), "/home/p"));
    }
}
