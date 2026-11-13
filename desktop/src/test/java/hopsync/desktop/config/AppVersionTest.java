package hopsync.desktop.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    @Test
    void versionComesFromVersionProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = AppVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
            assertNotNull(input, "version.properties must be packaged");
            props.load(input);
        }
        assertTrue(AppVersion.VERSION_APP.startsWith(props.getProperty("app.version")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a26.1.0", "b26.1.4", "rc26.2.0"})
    void prefixedVersionsAreTestBuilds(String version) {
        assertTrue(AppVersion.isTestBuild(version));
        assertEquals(version + " (test build)", AppVersion.displayVersion(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26.1.4", "26.2.0", "unknown", "beta"})
    void releasesAreNotTestBuilds(String version) {
        assertFalse(AppVersion.isTestBuild(version));
        assertEquals(version, AppVersion.displayVersion(version));
    }
}
