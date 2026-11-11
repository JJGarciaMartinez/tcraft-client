package hopsync.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void versionComesFromVersionProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("version.properties")) {
            assertNotNull(input, "version.properties must be packaged");
            props.load(input);
        }
        assertTrue(AppConfig.VERSION_APP.startsWith(props.getProperty("app.version")));
    }
}
