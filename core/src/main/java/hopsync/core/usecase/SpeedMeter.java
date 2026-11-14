package hopsync.core.usecase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * Download speed as the moving average of the last {@value #WINDOW_SECONDS} seconds, so the number the
 * player reads does not jump with every chunk.
 */
final class SpeedMeter {
    private static final int WINDOW_SECONDS = 3;
    private static final long WINDOW_NANOS = WINDOW_SECONDS * 1_000_000_000L;

    private record Sample(long nanos, long bytes) {
    }

    private final LongSupplier clock;
    private final Deque<Sample> samples = new ArrayDeque<>();

    /** @param clock nanoseconds, as {@link System#nanoTime()} */
    SpeedMeter(LongSupplier clock) {
        this.clock = clock;
    }

    /** Records the bytes downloaded so far and returns the current speed in bytes per second. */
    synchronized long add(long bytesDone) {
        long now = clock.getAsLong();
        samples.addLast(new Sample(now, bytesDone));
        // Keep one sample older than the window, so the average always spans the whole window
        while (samples.size() > 2 && now - secondOldest().nanos() >= WINDOW_NANOS) {
            samples.removeFirst();
        }
        Sample oldest = samples.peekFirst();
        long elapsed = now - oldest.nanos();
        if (elapsed <= 0) return 0;
        return Math.max(0, (bytesDone - oldest.bytes()) * 1_000_000_000L / elapsed);
    }

    private Sample secondOldest() {
        var it = samples.iterator();
        it.next();
        return it.next();
    }
}
