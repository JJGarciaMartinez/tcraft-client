package hopsync.model;

/**
 * A configuration file listed in the manifest.
 *
 * @param path      path relative to the Minecraft {@code config/} folder (validated: no traversal)
 * @param overwrite {@code true} to always replace the player's copy, {@code false} to install it only if missing
 */
public record ConfigEntry(String path, String url, String sha512, long size, boolean overwrite) {
}
