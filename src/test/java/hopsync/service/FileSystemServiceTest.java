package hopsync.service;

import hopsync.config.SettingsStore;
import hopsync.config.UserSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemServiceTest {

    @TempDir
    Path dir;

    @Test
    void defaultGameDirPerOs() {
        Path cwd = Path.of("/somewhere");
        assertEquals(Path.of("/home/p/.minecraft"), FileSystemService.defaultGameDir("Linux", "/home/p", cwd));
        assertEquals(Path.of("/Users/p/Library/Application Support/minecraft"),
            FileSystemService.defaultGameDir("Mac OS X", "/Users/p", cwd));
        assertEquals(Path.of("/u/p/AppData/Roaming/.minecraft"), FileSystemService.defaultGameDir("Windows 11", "/u/p", cwd));
    }

    @Test
    void launchedFromModsFolderUsesItsParent() {
        assertEquals(Path.of("/games/instance"),
            FileSystemService.defaultGameDir("Linux", "/home/p", Path.of("/games/instance/mods")));
    }

    @Test
    void savedGameDirDrivesAllFolders() throws IOException {
        SettingsService settings = new SettingsService(new SettingsStore(dir.resolve("settings.json")));
        Path instance = dir.resolve("instance");
        settings.save(new UserSettings("https://example.org/m.json", instance));

        FileSystemService fs = new FileSystemService(settings);

        assertEquals(instance.toFile(), fs.getGameDir());
        assertEquals(new File(instance.toFile(), "mods"), fs.getModsFolder());
        assertEquals(new File(instance.toFile(), "config"), fs.getConfigFolder());
        assertEquals(instance.resolve(".hopsync").resolve("state.json").toFile(), fs.getStateFile());
    }

    @Test
    void unconfiguredUsesPresetManifestUrl() {
        SettingsService settings = new SettingsService(new SettingsStore(dir.resolve("none.json")));
        assertTrue(settings.getManifestUrl().startsWith("https://"));
    }

    @Test
    void manifestUrlMustBeHttps() {
        SettingsService settings = new SettingsService(new SettingsStore(dir.resolve("none.json")));
        ManifestService manifests = new ManifestService(new DownloadService(), settings);
        IOException e = assertThrows(IOException.class, () -> manifests.fetchManifest("http://example.org/m.json"));
        assertTrue(e.getMessage().contains("https"));
    }
}
