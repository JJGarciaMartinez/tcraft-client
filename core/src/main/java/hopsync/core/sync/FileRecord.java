package hopsync.core.sync;

/**
 * What HopSync knows about a file it installed: its hash and the size and modification time
 * it had when the hash was computed. A file whose size and time still match is not rehashed.
 */
public record FileRecord(String sha512, long size, long modifiedMillis) {
}
