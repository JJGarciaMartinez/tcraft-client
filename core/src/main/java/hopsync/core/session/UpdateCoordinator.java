package hopsync.core.session;

import hopsync.core.event.EventBus;
import hopsync.core.event.UpdateEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.failure.SyncFailures;
import hopsync.core.session.UpdateState.Phase;
import hopsync.core.sync.SyncExecutor;
import hopsync.core.sync.SyncPlan;
import hopsync.core.usecase.ModUpdateService;
import hopsync.core.util.CancellationToken;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates the update process and publishes events.
 * This is the single source of truth for update state transitions:
 * PLANNING → (UpToDate | AWAITING_CONFIRMATION → APPLYING) → COMPLETED / FAILED / CANCELLED.
 */
public class UpdateCoordinator {
    private static final System.Logger LOG = System.getLogger(UpdateCoordinator.class.getName());

    private final EventBus eventBus;
    private final UpdateState state = new UpdateState();
    private final ModUpdateService modUpdateService;
    private final AtomicReference<SyncPlan> pendingPlan = new AtomicReference<>();
    private final Executor executor;
    // A fresh token per update, so cancelling one run never affects the next
    private volatile CancellationToken cancel = new CancellationToken();

    /**
     * @param executor runs planning and applying off the caller's thread
     */
    public UpdateCoordinator(EventBus eventBus, ModUpdateService modUpdateService, Executor executor) {
        this.eventBus = eventBus;
        this.modUpdateService = modUpdateService;
        this.executor = executor;
    }

    /** Fetches the manifest and computes the plan; publishes {@code PlanReady} or {@code UpToDate}. */
    public void startUpdate() {
        if (!state.tryBegin()) return;

        cancel = new CancellationToken();
        eventBus.publish(new UpdateEvent.UpdateStarted());
        executor.execute(this::prepare);
    }

    /** Applies the plan published in the last {@code PlanReady}. */
    public synchronized void confirmPlan() {
        SyncPlan plan = pendingPlan.getAndSet(null);
        if (plan == null || !state.transition(Phase.AWAITING_CONFIRMATION, Phase.APPLYING)) return;

        eventBus.publish(new UpdateEvent.ApplyStarted());
        executor.execute(() -> apply(plan));
    }

    public synchronized void cancelUpdate() {
        if (pendingPlan.getAndSet(null) != null && state.transition(Phase.AWAITING_CONFIRMATION, Phase.CANCELLED)) {
            eventBus.publish(new UpdateEvent.UpdateCancelled());
            return;
        }
        if (state.isActive()) {
            // The worker notices, stops and publishes UpdateCancelled itself
            cancel.cancel();
        }
    }

    private void prepare() {
        try {
            SyncPlan plan = modUpdateService.prepare(this::publish);
            if (cancel.isCancelled()) {
                finishCancelled();
            } else if (plan.isUpToDate()) {
                // Nothing to download, but the state still records the files and manifest
                modUpdateService.apply(plan, this::publish, cancel);
                state.setPhase(Phase.COMPLETED);
                eventBus.publish(new UpdateEvent.UpToDate(plan));
            } else {
                synchronized (this) {
                    if (cancel.isCancelled()) {
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
            SyncExecutor.Result result = modUpdateService.apply(plan, this::publish, cancel);
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

    private void finishCancelled() {
        state.setPhase(Phase.CANCELLED);
        eventBus.publish(new UpdateEvent.UpdateCancelled());
    }

    private void fail(Exception e) {
        SyncFailure failure = SyncFailures.from(e, "");
        LOG.log(System.Logger.Level.WARNING, "Update failed: " + failure.describe(), e);
        state.setPhase(Phase.FAILED);
        eventBus.publish(new UpdateEvent.UpdateFailed(failure));
    }

    private void publish(UpdateEvent event) {
        eventBus.publish(event);
    }

    public Phase getPhase() { return state.getPhase(); }
    public boolean isRunning() { return state.isActive(); }
}
