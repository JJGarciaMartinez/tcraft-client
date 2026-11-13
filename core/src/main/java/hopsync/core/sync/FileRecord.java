package hopsync.core.sync;

/**
 * What HopSync knows about a file it installed: its hash and the size and modification time
 * it had when the hash was computed. A file whose size and time still match is not rehashed.
 *
 * @param version version from the manifest the file came from, for display only; {@code null} for configs
 *                and for files recorded before HopSync kept versions
 */
public record FileRecord(String sha512, long size, long modifiedMillis, String version) {

    public FileRecord(String sha512, long size, long modifiedMillis) {
        this(sha512, size, modifiedMillis, null);
    }

    public FileRecord withVersion(String newVersion) {
        return new FileRecord(sha512, size, modifiedMillis, newVersion);
    }
}
