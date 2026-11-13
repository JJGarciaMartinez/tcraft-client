package hopsync.core.util;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cooperative cancellation shared by everything working on one operation. Long-running work polls
 * {@link #isCancelled()}; blocking I/O registers a callback with {@link #onCancel} to abort itself.
 */
public final class CancellationToken {
    private static final System.Logger LOG = System.getLogger(CancellationToken.class.getName());

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<>();

    /** A token that is never cancelled, for callers that do not support cancellation. */
    public static CancellationToken none() {
        return new CancellationToken();
    }

    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            callbacks.forEach(CancellationToken::runQuietly);
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new CancellationException("Cancelled");
        }
    }

    /**
     * Runs {@code callback} once when the token is cancelled, or right away if it already is.
     * Close the returned registration when the work finishes so the callback is dropped.
     */
    public Registration onCancel(Runnable callback) {
        callbacks.add(callback);
        if (isCancelled() && callbacks.remove(callback)) {
            runQuietly(callback);
        }
        return () -> callbacks.remove(callback);
    }

    private static void runQuietly(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Cancellation callback failed", e);
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
