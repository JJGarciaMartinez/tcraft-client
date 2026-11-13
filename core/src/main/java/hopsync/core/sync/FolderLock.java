package hopsync.core.sync;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Exclusive lock on a game folder while HopSync writes to it, so two HopSync processes never apply
 * changes to the same folder at once. The OS releases it if the process dies.
 */
public final class FolderLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private FolderLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * @param lockFile file used as the lock, created if missing
     * @param folder   the folder being protected, named in the failure when the lock is taken
     * @throws SyncException with {@link SyncFailure.FolderInUse} if another process holds the lock
     */
    public static FolderLock acquire(Path lockFile, Path folder) throws IOException {
        Files.createDirectories(lockFile.toAbsolutePath().getParent());
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new SyncException(new SyncFailure.FolderInUse(folder.toString()));
            }
            return new FolderLock(channel, lock);
        } catch (OverlappingFileLockException e) {
            channel.close();
            throw new SyncException(new SyncFailure.FolderInUse(folder.toString()), e);
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
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
