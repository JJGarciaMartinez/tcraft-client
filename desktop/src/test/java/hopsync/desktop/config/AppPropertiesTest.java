package hopsync.desktop.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppPropertiesTest {

    private static Properties loadAppProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = AppProperties.class.getClassLoader().getResourceAsStream("app.properties")) {
            assertNotNull(input, "app.properties must be packaged");
            props.load(input);
        }
        return props;
    }

    @Test
    void networkTimeoutsAreIntegers() throws IOException {
        Properties props = loadAppProperties();
        props.stringPropertyNames().stream()
            .filter(key -> key.endsWith(".timeout"))
            .forEach(key -> assertDoesNotThrow(() -> Integer.parseInt(props.getProperty(key)), key));
    }
}
