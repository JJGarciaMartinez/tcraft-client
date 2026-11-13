package hopsync.core.failure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SyncFailuresTest {

    @Test
    void keepsTheFailureOfASyncException() {
        SyncFailure failure = new SyncFailure.HashMismatch("a.jar");
        assertEquals(failure, SyncFailures.from(new SyncException(failure), "ignored"));
    }

    @Test
    void accessDeniedIsPermissionDenied() {
        assertEquals(new SyncFailure.PermissionDenied("/game/mods/a.jar"),
            SyncFailures.from(new AccessDeniedException("/game/mods/a.jar"), "a.jar"));
    }

    @Test
    void fullDiskOnLinuxAndWindows() {
        assertEquals(new SyncFailure.DiskFull("a.jar"), SyncFailures.from(new IOException("No space left on device"), "a.jar"));
        assertEquals(new SyncFailure.DiskFull("C:\\mods\\a.jar"), SyncFailures.from(
            new FileSystemException("C:\\mods\\a.jar", null, "There is not enough space on the disk"), "a.jar"));
    }

    @Test
    void fileInUseOnWindows() {
        assertEquals(new SyncFailure.FileLocked("C:\\mods\\a.jar"), SyncFailures.from(new FileSystemException(
            "C:\\mods\\a.jar", null, "The process cannot access the file because it is being used by another process"), "a.jar"));
    }

    @Test
    void anythingElseIsUnexpected() {
        assertInstanceOf(SyncFailure.Unexpected.class, SyncFailures.from(new IllegalStateException("bug"), "a.jar"));
        assertInstanceOf(SyncFailure.Unexpected.class, SyncFailures.from(new IOException("Stream closed"), "a.jar"));
    }
}
