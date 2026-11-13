package hopsync.platform.system;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Keeps a second copy of the app from running at the same time for the same user. The OS releases
 * the lock when the process exits, even after a crash.
 */
public final class InstanceLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private InstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * @return the lock, or empty if another instance already holds it
     */
    public static Optional<InstanceLock> tryAcquire(Path lockFile) throws IOException {
        Files.createDirectories(lockFile.toAbsolutePath().getParent());
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock != null) {
                return Optional.of(new InstanceLock(channel, lock));
            }
        } catch (OverlappingFileLockException e) {
            // Held by this same process
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
        channel.close();
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
        }
    }
}
