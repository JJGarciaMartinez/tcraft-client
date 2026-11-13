package hopsync.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SettingsDialogTest {

    @TempDir
    Path dir;

    @Test
    void validatesUrl() {
        assertNull(SettingsDialog.validateUrl("https://example.org/manifest.json"));
        assertNotNull(SettingsDialog.validateUrl(""));
        assertNotNull(SettingsDialog.validateUrl("http://example.org/manifest.json"));
        assertNotNull(SettingsDialog.validateUrl("example.org/manifest.json"));
    }

    @Test
    void validatesGameDir() {
        assertNull(SettingsDialog.validateGameDir(dir.toString()));
        assertNotNull(SettingsDialog.validateGameDir(""));
        assertNotNull(SettingsDialog.validateGameDir(dir.resolve("missing").toString()));
    }
}
