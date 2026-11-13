package hopsync.core.sync;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FolderLockTest {

    @TempDir
    Path dir;

    @Test
    void secondHolderIsRejectedUntilReleased() throws IOException {
        Path lockFile = dir.resolve(".hopsync/apply.lock");

        FolderLock held = FolderLock.acquire(lockFile, dir);
        try {
            SyncException e = assertThrows(SyncException.class, () -> FolderLock.acquire(lockFile, dir));
            assertEquals(new SyncFailure.FolderInUse(dir.toString()), e.failure());
        } finally {
            held.close();
        }

        assertDoesNotThrow(() -> FolderLock.acquire(lockFile, dir).close());
    }
}
