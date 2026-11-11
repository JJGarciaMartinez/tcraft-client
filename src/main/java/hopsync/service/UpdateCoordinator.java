package hopsync.service;

import hopsync.event.EventBus;
import hopsync.event.UpdateEvent;
import hopsync.state.UpdateState;
import hopsync.state.UpdateState.Phase;
import hopsync.sync.SyncExecutor;
import hopsync.sync.SyncPlan;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates the update process and publishes events.
 * This is the single source of truth for update state transitions:
 * PLANNING → (UpToDate | AWAITING_CONFIRMATION → APPLYING) → COMPLETED / FAILED / CANCELLED.
 */
public class UpdateCoordinator {
    private final EventBus eventBus;
    private final UpdateState state = new UpdateState();
    private final ModUpdateService modUpdateService;
    private final AtomicReference<SyncPlan> pendingPlan = new AtomicReference<>();
    private final AtomicReference<Thread> worker = new AtomicReference<>();
    private final AtomicBoolean cancelRequested = new AtomicBoolean();

    public UpdateCoordinator(EventBus eventBus, ModUpdateService modUpdateService) {
        this.eventBus = eventBus;
        this.modUpdateService = modUpdateService;
    }

    /** Fetches the manifest and computes the plan; publishes {@code PlanReady} or {@code UpToDate}. */
    public void startUpdate() {
        if (!state.tryBegin()) return;

        cancelRequested.set(false);
        eventBus.publish(new UpdateEvent.UpdateStarted());
        startWorker(this::prepare);
    }

    /** Applies the plan published in the last {@code PlanReady}. */
    public synchronized void confirmPlan() {
        SyncPlan plan = pendingPlan.getAndSet(null);
        if (plan == null || !state.transition(Phase.AWAITING_CONFIRMATION, Phase.APPLYING)) return;

        eventBus.publish(new UpdateEvent.ApplyStarted());
        startWorker(() -> apply(plan));
    }

    public synchronized void cancelUpdate() {
        if (pendingPlan.getAndSet(null) != null && state.transition(Phase.AWAITING_CONFIRMATION, Phase.CANCELLED)) {
            eventBus.publish(new UpdateEvent.UpdateCancelled());
            return;
        }
        if (state.isActive()) {
            // The worker notices, stops and publishes UpdateCancelled itself
            cancelRequested.set(true);
            Thread thread = worker.get();
            if (thread != null) thread.interrupt();
        }
    }

    private void prepare() {
        try {
            SyncPlan plan = modUpdateService.prepare(this::publish);
            if (cancelRequested.get()) {
                finishCancelled();
            } else if (plan.isUpToDate()) {
                // Nothing to download, but the state still records the files and manifest
                modUpdateService.apply(plan, this::publish, cancelRequested::get);
                state.setPhase(Phase.COMPLETED);
                eventBus.publish(new UpdateEvent.UpToDate(plan));
            } else {
                synchronized (this) {
                    if (cancelRequested.get()) {
                        finishCancelled();
                        return;
                    }
                    pendingPlan.set(plan);
                    state.setPhase(Phase.AWAITING_CONFIRMATION);
                }
                eventBus.publish(new UpdateEvent.PlanReady(plan));
            }
        } catch (IOException | RuntimeException e) {
            fail(e);
        }
    }

    private void apply(SyncPlan plan) {
        try {
            SyncExecutor.Result result = modUpdateService.apply(plan, this::publish, cancelRequested::get);
            if (result.cancelled()) {
                finishCancelled();
            } else {
                state.setPhase(Phase.COMPLETED);
                eventBus.publish(new UpdateEvent.UpdateCompleted(result.succeeded(), result.failed()));
            }
        } catch (IOException | RuntimeException e) {
            fail(e);
        }
    }

    private void startWorker(Runnable task) {
        Thread thread = new Thread(task, "hopsync-update");
        worker.set(thread);
        thread.start();
    }

    private void finishCancelled() {
        state.setPhase(Phase.CANCELLED);
        eventBus.publish(new UpdateEvent.UpdateCancelled());
    }

    private void fail(Exception e) {
        state.setPhase(Phase.FAILED);
        eventBus.publish(new UpdateEvent.UpdateFailed(e.getMessage()));
    }

    private void publish(UpdateEvent event) {
        eventBus.publish(event);
    }

    public Phase getPhase() { return state.getPhase(); }
    public boolean isRunning() { return state.isActive(); }
}
