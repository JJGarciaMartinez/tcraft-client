package hopsync.sync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes SHA-512 hashes, reusing a known record when the file's size and modification time
 * have not changed since it was hashed.
 */
public final class FileHasher {

    private FileHasher() {
    }

    /**
     * Returns the current record of {@code file}, hashing it only if it differs from {@code known}.
     */
    public static FileRecord describe(Path file, FileRecord known) throws IOException {
        long size = Files.size(file);
        long modified = Files.getLastModifiedTime(file).toMillis();
        if (known != null && known.size() == size && known.modifiedMillis() == modified) {
            return known;
        }
        return new FileRecord(sha512(file), size, modified);
    }

    public static String sha512(Path file) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is required by every Java platform", e);
        }
    }
}
