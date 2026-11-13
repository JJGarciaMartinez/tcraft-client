package hopsync.desktop.config;

import hopsync.platform.http.NetworkConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Settings read once from app.properties at startup and passed to the services that need them.
 *
 * @param presetManifestUrl manifest offered to players who have not chosen a pack yet
 */
public record AppConfig(NetworkConfig network, String presetManifestUrl) {
    static final String DEFAULT_MANIFEST_URL =
        "https://github.com/JJGarciaMartinez/hopsync-demo/releases/download/hopsync/manifest.json";

    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) props.load(input);
        } catch (IOException e) {
            System.err.println("Warning: could not load app.properties, using defaults: " + e.getMessage());
        }
        return fromProperties(props);
    }

    static AppConfig fromProperties(Properties props) {
        NetworkConfig defaults = NetworkConfig.DEFAULT;
        NetworkConfig network = new NetworkConfig(
            intOr(props, "network.connect.timeout", defaults.connectTimeoutMillis()),
            intOr(props, "network.read.timeout", defaults.readTimeoutMillis()),
            props.getProperty("network.user.agent", defaults.userAgent()));
        return new AppConfig(network, props.getProperty("manifest.url", DEFAULT_MANIFEST_URL));
    }

    private static int intOr(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
