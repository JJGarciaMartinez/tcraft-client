package hopsync.platform.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceLockTest {

    @TempDir
    Path dir;

    @Test
    void onlyOneHolderAtATime() throws IOException {
        Path lockFile = dir.resolve("hopsync/hopsync.lock");

        InstanceLock first = InstanceLock.tryAcquire(lockFile).orElseThrow();
        try {
            assertTrue(InstanceLock.tryAcquire(lockFile).isEmpty());
        } finally {
            first.close();
        }

        Optional<InstanceLock> again = InstanceLock.tryAcquire(lockFile);
        assertTrue(again.isPresent());
        again.get().close();
    }
}
