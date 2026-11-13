package hopsync.desktop.config;

import hopsync.platform.http.NetworkConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void readsNetworkAndPresetFromProperties() {
        Properties props = new Properties();
        props.setProperty("network.connect.timeout", "5000");
        props.setProperty("network.read.timeout", "7000");
        props.setProperty("network.user.agent", "HopSync-Test");
        props.setProperty("manifest.url", "https://example.org/manifest.json");

        AppConfig config = AppConfig.fromProperties(props);

        assertEquals(new NetworkConfig(5000, 7000, "HopSync-Test"), config.network());
        assertEquals("https://example.org/manifest.json", config.presetManifestUrl());
    }

    @Test
    void invalidOrMissingValuesFallBackToDefaults() {
        Properties props = new Properties();
        props.setProperty("network.connect.timeout", "soon");

        AppConfig config = AppConfig.fromProperties(props);

        assertEquals(NetworkConfig.DEFAULT, config.network());
        assertEquals(AppConfig.DEFAULT_MANIFEST_URL, config.presetManifestUrl());
    }

    @Test
    void packagedPropertiesLoad() {
        AppConfig config = AppConfig.load();
        assertTrue(config.presetManifestUrl().startsWith("https://"));
        assertTrue(config.network().connectTimeoutMillis() > 0);
    }
}
