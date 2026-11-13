package hopsync.core.failure;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.Locale;

/**
 * Classifies exceptions into {@link SyncFailure}s.
 */
public final class SyncFailures {

    private SyncFailures() {
    }

    /**
     * @param path the file or folder being worked on, used when the exception does not name one
     */
    public static SyncFailure from(Throwable error, String path) {
        if (error instanceof SyncException e) {
            return e.failure();
        }
        if (error instanceof AccessDeniedException e) {
            return new SyncFailure.PermissionDenied(e.getFile() != null ? e.getFile() : path);
        }
        if (error instanceof IOException) {
            String message = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
            String file = error instanceof FileSystemException fse && fse.getFile() != null ? fse.getFile() : path;
            // The JDK reports these only through the OS message: Linux/macOS first, then Windows wording
            if (message.contains("no space left") || message.contains("not enough space")) {
                return new SyncFailure.DiskFull(file);
            }
            if (message.contains("being used by another process") || message.contains("locked a portion of the file")) {
                return new SyncFailure.FileLocked(file);
            }
        }
        return new SyncFailure.Unexpected(error.toString());
    }
}
