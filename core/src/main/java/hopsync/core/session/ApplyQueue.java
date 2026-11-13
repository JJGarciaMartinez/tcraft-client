package hopsync.core.session;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * Applies confirmed plans one pack at a time, in the order they were confirmed. Checking packs runs in
 * parallel; writing them does not.
 */
public class ApplyQueue {

    /** A confirmed plan waiting for its turn. */
    public interface Job {
        String packId();

        /** Called when the job has to wait, before any later job can start. */
        void queued();

        /** Applies the plan; blocks until it is done. */
        void run();
    }

    private final Executor executor;
    private final BooleanSupplier chainAfterCancel;
    private final Deque<Job> waiting = new ArrayDeque<>();
    private Job running;
    private boolean runningCancelled;
    private boolean paused;

    /**
     * @param chainAfterCancel whether the queue keeps going after the running job is cancelled; when
     *                         false it pauses until {@link #resume()}
     */
    public ApplyQueue(Executor executor, BooleanSupplier chainAfterCancel) {
        this.executor = executor;
        this.chainAfterCancel = chainAfterCancel;
    }

    public synchronized void submit(Job job) {
        waiting.add(job);
        if (running != null || paused) {
            job.queued();
        }
        startNext();
    }

    /**
     * Takes the pack out of the queue if it is still waiting.
     *
     * @return true if it was waiting and is now removed; false if it is running (the caller stops it)
     *         or was never queued
     */
    public synchronized boolean cancel(String packId) {
        if (waiting.removeIf(job -> job.packId().equals(packId))) {
            return true;
        }
        if (running != null && running.packId().equals(packId)) {
            runningCancelled = true;
        }
        return false;
    }

    /** Starts the waiting jobs again after a cancellation paused the queue. */
    public synchronized void resume() {
        paused = false;
        startNext();
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    /** True while a pack is being applied, so the next one confirmed will wait. */
    public synchronized boolean isBusy() {
        return running != null;
    }

    private void startNext() {
        if (running != null || paused || waiting.isEmpty()) {
            return;
        }
        Job job = waiting.poll();
        running = job;
        runningCancelled = false;
        executor.execute(() -> {
            try {
                job.run();
            } finally {
                finished();
            }
        });
    }

    private synchronized void finished() {
        if (runningCancelled && !chainAfterCancel.getAsBoolean() && !waiting.isEmpty()) {
            paused = true;
        }
        running = null;
        startNext();
    }
}
