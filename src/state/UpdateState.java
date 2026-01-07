package state;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe state container for the update process.
 * Replaces the scattered state across multiple UI components.
 */
public class UpdateState {
    public enum Phase { IDLE, STARTING, DOWNLOADING, COMPLETED, FAILED, CANCELLED }

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.IDLE);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicReference<Thread> updateThread = new AtomicReference<>();

    public Phase getPhase() { return phase.get(); }
    public void setPhase(Phase p) { phase.set(p); }

    public int getCompletedCount() { return completedCount.get(); }
    public void incrementCompleted() { completedCount.incrementAndGet(); }

    public int getFailedCount() { return failedCount.get(); }
    public void incrementFailed() { failedCount.incrementAndGet(); }

    public int getTotalCount() { return totalCount.get(); }
    public void setTotalCount(int total) { totalCount.set(total); }

    public void reset() {
        phase.set(Phase.IDLE);
        completedCount.set(0);
        failedCount.set(0);
        totalCount.set(0);
    }

    public boolean isRunning() {
        Thread t = updateThread.get();
        return t != null && t.isAlive();
    }

    public void setUpdateThread(Thread t) { updateThread.set(t); }

    public void cancel() {
        Thread t = updateThread.get();
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }
}
