package hopsync.platform.http;

/**
 * Settings for every HTTP request the app makes.
 */
public record NetworkConfig(int connectTimeoutMillis, int readTimeoutMillis, String userAgent) {
    public static final NetworkConfig DEFAULT = new NetworkConfig(10_000, 30_000, "HopSync");
}
