package hopsync.core.util;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Guards against path traversal when writing files whose names come from a remote manifest.
 */
public final class SafePaths {
    // ':' covers Windows drive letters and alternate data streams
    private static final Pattern FORBIDDEN = Pattern.compile("[\\\\:\\p{Cntrl}]");

    private SafePaths() {
    }

    /**
     * Returns true for a plain file name: no separators, not "." or "..".
     */
    public static boolean isSafeFileName(String name) {
        return name != null && !name.isBlank() && !name.contains("/")
            && !FORBIDDEN.matcher(name).find() && !name.equals(".") && !name.equals("..");
    }

    /**
     * Returns true for a relative path made of safe file names joined by '/'.
     */
    public static boolean isSafeRelativePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/")) {
            return false;
        }
        for (String segment : path.split("/", -1)) {
            if (!isSafeFileName(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves {@code relative} against {@code base} and verifies the result stays inside {@code base}.
     *
     * @throws IOException if the path is unsafe or escapes the base directory
     */
    public static Path resolveInside(Path base, String relative) throws IOException {
        if (!isSafeRelativePath(relative)) {
            throw new SyncException(new SyncFailure.UnsafePath(relative));
        }
        Path normalizedBase = base.toAbsolutePath().normalize();
        try {
            Path resolved = normalizedBase.resolve(relative).normalize();
            if (!resolved.startsWith(normalizedBase) || resolved.equals(normalizedBase)) {
                throw new SyncException(new SyncFailure.UnsafePath(relative));
            }
            return resolved;
        } catch (InvalidPathException e) {
            throw new SyncException(new SyncFailure.UnsafePath(relative), e);
        }
    }
}
