package hopsync.core.session;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe phase of one pack's update process.
 */
public class UpdateState {
    public enum Phase {
        IDLE, PLANNING, AWAITING_CONFIRMATION, QUEUED, APPLYING, COMPLETED, FAILED, CANCELLED;

        public boolean isActive() {
            return this == PLANNING || this == AWAITING_CONFIRMATION || this == QUEUED || this == APPLYING;
        }
    }

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.IDLE);

    public Phase getPhase() {
        return phase.get();
    }

    public void setPhase(Phase p) {
        phase.set(p);
    }

    /**
     * Atomically moves from {@code expected} to {@code next}; returns false if another thread got there first.
     */
    public boolean transition(Phase expected, Phase next) {
        return phase.compareAndSet(expected, next);
    }

    /**
     * Starts a new update unless one is already active.
     */
    public boolean tryBegin() {
        Phase current = phase.get();
        return !current.isActive() && phase.compareAndSet(current, Phase.PLANNING);
    }

    public boolean isActive() {
        return phase.get().isActive();
    }
}
