package hopsync.core.manifest;

/**
 * A mod listed in the manifest.
 *
 * @param file   file name inside the {@code mods/} folder (validated: no path separators)
 * @param sha512 lowercase hex SHA-512 of the file
 * @param size   file size in bytes
 */
public record ModEntry(String id, String name, String version, String author, String description,
                       String file, String url, String sha512, long size, ModSource source) {
}
