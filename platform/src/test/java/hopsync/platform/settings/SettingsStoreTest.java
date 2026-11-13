package hopsync.platform.settings;

import hopsync.core.pack.Pack;
import hopsync.core.pack.PackSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTrips() throws IOException {
        SettingsStore store = new SettingsStore(dir.resolve("nested/settings.json"));
        Pack first = new Pack("a1", "https://example.org/manifest.json", dir.resolve("My Instance [1.21]"), "Demo");
        Pack second = new Pack("b2", "https://example.org/other.json", dir.resolve("other"), "");
        PackSettings settings = new PackSettings(List.of(first, second), "b2", false, false, 6);

        store.save(settings);

        assertEquals(settings, store.load());
        assertFalse(Files.exists(dir.resolve("nested/settings.json.tmp")));
    }

    @Test
    void missingOrCorruptFileMeansNoSettings() throws IOException {
        Path file = dir.resolve("settings.json");
        assertEquals(PackSettings.EMPTY, new SettingsStore(file).load());

        Files.writeString(file, "not json");
        assertEquals(PackSettings.EMPTY, new SettingsStore(file).load());

        Files.writeString(file, "{\"format\": 1, \"manifest_url\": 3, \"game_dir\": \"/x\"}");
        assertEquals(PackSettings.EMPTY, new SettingsStore(file).load());

        Files.writeString(file, "{\"format\": 1, \"manifest_url\": \"\", \"game_dir\": \"/x\"}");
        assertEquals(PackSettings.EMPTY, new SettingsStore(file).load());
    }

    @Test
    void unknownFormatMeansNoSettings() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{\"format\": 3, \"packs\": []}");
        assertEquals(PackSettings.EMPTY, new SettingsStore(file).load());
    }

    @Test
    void incompletePacksAreSkippedAndPreferencesClamped() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{\"format\": 2, \"active\": \"gone\", \"parallel_downloads\": 40, \"packs\": ["
            + "{\"id\": \"a\", \"manifest_url\": \"https://x/m.json\", \"game_dir\": \"" + jsonPath(dir.resolve("a")) + "\"},"
            + "{\"id\": \"b\", \"manifest_url\": \"https://x/m.json\"}]}");

        PackSettings settings = new SettingsStore(file).load();

        assertEquals(1, settings.packs().size());
        assertEquals("a", settings.activeId());
        assertEquals("", settings.packs().getFirst().name());
        assertEquals(PackSettings.MAX_PARALLEL_DOWNLOADS, settings.parallelDownloads());
        assertTrue(settings.checkOnStart());
    }

    @Test
    void singlePackFormatIsMigratedWithABackup() throws IOException {
        Path file = dir.resolve("settings.json");
        Path instance = dir.resolve("instance");
        String v1 = "{\"format\": 1, \"manifest_url\": \"https://example.org/m.json\", \"game_dir\": \"" + jsonPath(instance) + "\"}";
        Files.writeString(file, v1);
        SettingsStore store = new SettingsStore(file);

        PackSettings migrated = store.load();

        assertEquals(1, migrated.packs().size());
        Pack pack = migrated.packs().getFirst();
        assertEquals("https://example.org/m.json", pack.manifestUrl());
        assertEquals(instance.toAbsolutePath(), pack.gameDir());
        assertEquals(pack.id(), migrated.activeId());
        assertEquals(v1, Files.readString(store.backupFile()));
        // Saved in format 2 right away, so the id stays the same on the next start
        assertEquals(migrated, new SettingsStore(file).load());
    }

    @Test
    void backupIsNeverOverwritten() throws IOException {
        Path file = dir.resolve("settings.json");
        SettingsStore store = new SettingsStore(file);
        Files.writeString(store.backupFile(), "original");
        Files.writeString(file, "{\"format\": 1, \"manifest_url\": \"https://example.org/m.json\", \"game_dir\": \"/x\"}");

        store.load();

        assertEquals("original", Files.readString(store.backupFile()));
    }

    private static String jsonPath(Path path) {
        return path.toAbsolutePath().toString().replace("\\", "\\\\");
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
