package hopsync.desktop.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration loader that reads from app.properties.
 * Falls back to sensible defaults if properties file is missing.
 */
public class AppProperties {
    private static final System.Logger LOG = System.getLogger(AppProperties.class.getName());

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    // Font
    public static String getFallbackFont() { return get("font.fallback", "Arial"); }

    private static void ensureLoaded() {
        if (!loaded) {
            try (InputStream input = AppProperties.class.getClassLoader()
                    .getResourceAsStream("app.properties")) {
                if (input != null) props.load(input);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING, "Could not load app.properties, using defaults", e);
            }
            loaded = true;
        }
    }

    private static String get(String key, String defaultValue) {
        ensureLoaded();
        return props.getProperty(key, defaultValue);
    }
}
