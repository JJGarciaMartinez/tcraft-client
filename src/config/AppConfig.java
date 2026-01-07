package config;

import java.io.InputStream;
import java.util.Properties;

/**
 * The AppConfig class contains static constants for application-wide configuration.
 * This class serves as a central place to define key constants such as the application's
 * version and external resource URLs.
 * <p>
 * The version is loaded from the version.properties file to maintain consistency
 * across the application and build scripts.
 * <p>
 * Constants included in this class:
 * - VERSION_APP: The current version of the application (loaded from version.properties).
 */
public class AppConfig {
    public static final String VERSION_APP = loadVersion();

    /**
     * Loads the application version from the version.properties file.
     * If the file cannot be read, defaults to "unknown".
     *
     * @return The application version string
     */
    private static String loadVersion() {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input == null) {
                System.err.println("Warning: version.properties not found, using default version");
                return "unknown";
            }

            Properties props = new Properties();
            props.load(input);

            String version = props.getProperty("app.version", "unknown");
            if(version.contains("a") | version.contains("b") | version.contains("rc")) {
                version += " (test build)";
            }
            return version;
        } catch (Exception e) {
            System.err.println("Error loading version: " + e.getMessage());
            return "unknown";
        }
    }
}

