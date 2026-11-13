package hopsync.core.session;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.failure.SyncFailures;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.core.session.UpdateState.Phase;
import hopsync.core.sync.SyncExecutor;
import hopsync.core.sync.SyncPlan;
import hopsync.core.usecase.ModUpdateService;
import hopsync.core.util.CancellationToken;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The update lifecycle of one pack and the single source of truth for its transitions:
 * PLANNING → (UpToDate | AWAITING_CONFIRMATION → QUEUED → APPLYING) → COMPLETED / FAILED / CANCELLED.
 * Every event it publishes carries the pack's id.
 */
public class PackSession {
    private static final System.Logger LOG = System.getLogger(PackSession.class.getName());

    private final String packId;
    private final PackRegistry registry;
    private final ModUpdateService modUpdateService;
    private final ApplyQueue applyQueue;
    private final Consumer<SyncEvent> events;
    private final Executor executor;
    private final UpdateState state = new UpdateState();
    // Plan waiting for the player, and the pack as it was when the plan was made
    private SyncPlan pendingPlan;
    private Pack plannedPack;
    // A fresh token per update, so cancelling one run never affects the next
    private volatile CancellationToken cancel = new CancellationToken();
    // Files the player already agreed to change in this run, or null when the plan needs a review
    private volatile Set<String> reviewed;

    /**
     * @param executor runs planning off the caller's thread; applying runs on the {@link ApplyQueue}
     */
    public PackSession(String packId, PackRegistry registry, ModUpdateService modUpdateService,
                       ApplyQueue applyQueue, Consumer<SyncEvent> events, Executor executor) {
        this.packId = packId;
        this.registry = registry;
        this.modUpdateService = modUpdateService;
        this.applyQueue = applyQueue;
        this.events = events;
        this.executor = executor;
    }

    public String packId() {
        return packId;
    }

    /** Fetches the manifest and computes the plan; publishes {@code PlanReady} or {@code UpToDate}. */
    public void startUpdate() {
        begin(null);
    }

    /**
     * Picks up an update the player already reviewed: after failed downloads ("Reintentar") or a cancel
     * ("Continuar"). The plan is computed again; if all it still has to do is among {@code expected}, it is
     * applied without asking again, otherwise (the pack changed meanwhile) it is shown for review.
     *
     * @param expected mod files and config paths the player already agreed to change
     */
    public void continueUpdate(Set<String> expected) {
        begin(Set.copyOf(expected));
    }

    private void begin(Set<String> alreadyReviewed) {
        if (!state.tryBegin()) return;

        cancel = new CancellationToken();
        reviewed = alreadyReviewed;
        events.accept(new SyncEvent.UpdateStarted(packId));
        executor.execute(this::prepare);
    }

    /**
     * Sends the plan published in the last {@code PlanReady} to the apply queue. If another pack is
     * applying, {@code Queued} is published and this one starts after it.
     */
    public synchronized void confirmPlan() {
        if (pendingPlan == null || !state.transition(Phase.AWAITING_CONFIRMATION, Phase.QUEUED)) return;
        SyncPlan plan = pendingPlan;
        Pack pack = plannedPack;
        pendingPlan = null;

        applyQueue.submit(new ApplyQueue.Job() {
            @Override
            public String packId() {
                return packId;
            }

            @Override
            public void queued() {
                events.accept(new SyncEvent.Queued(packId));
            }

            @Override
            public void run() {
                apply(pack, plan);
            }
        });
    }

    public synchronized void cancelUpdate() {
        Phase phase = state.getPhase();
        if (phase == Phase.AWAITING_CONFIRMATION && state.transition(Phase.AWAITING_CONFIRMATION, Phase.CANCELLED)) {
            pendingPlan = null;
            events.accept(new SyncEvent.UpdateCancelled(packId));
            return;
        }
        if (phase == Phase.QUEUED && applyQueue.cancel(packId)) {
            state.setPhase(Phase.CANCELLED);
            events.accept(new SyncEvent.UpdateCancelled(packId));
            return;
        }
        if (state.isActive()) {
            // The worker notices, stops and publishes UpdateCancelled itself
            cancel.cancel();
        }
    }

    public Phase getPhase() {
        return state.getPhase();
    }

    public boolean isRunning() {
        return state.isActive();
    }

    private void prepare() {
        try {
            Pack pack = registry.find(packId).orElseThrow(() -> new SyncException(new SyncFailure.NotConfigured()));
            SyncPlan plan = modUpdateService.prepare(pack, events);
            registry.rememberName(packId, plan.manifest().pack().name());
            if (cancel.isCancelled()) {
                finishCancelled();
            } else if (plan.isUpToDate()) {
                // Nothing to download, but the state still records the files and manifest
                modUpdateService.apply(pack, plan, events, cancel);
                state.setPhase(Phase.COMPLETED);
                events.accept(new SyncEvent.UpToDate(packId, plan));
            } else {
                synchronized (this) {
                    if (cancel.isCancelled()) {
                        finishCancelled();
                        return;
                    }
                    pendingPlan = plan;
                    plannedPack = pack;
                    state.setPhase(Phase.AWAITING_CONFIRMATION);
                }
                events.accept(new SyncEvent.PlanReady(packId, plan));
                Set<String> agreed = reviewed;
                if (agreed != null && agreed.containsAll(plan.pendingFiles())) {
                    confirmPlan();
                }
            }
        } catch (IOException | RuntimeException e) {
            fail(e);
        }
    }

    private void apply(Pack pack, SyncPlan plan) {
        synchronized (this) {
            if (cancel.isCancelled()) {
                // Cancelled as its turn came, before anything was written
                finishCancelled();
                return;
            }
            if (!state.transition(Phase.QUEUED, Phase.APPLYING)) {
                return;
            }
        }
        events.accept(new SyncEvent.ApplyStarted(packId));
        try {
            SyncExecutor.Result result = modUpdateService.apply(pack, plan, events, cancel);
            if (result.cancelled()) {
                finishCancelled();
            } else {
                state.setPhase(Phase.COMPLETED);
                events.accept(new SyncEvent.UpdateCompleted(packId, result.succeeded(), result.failed()));
            }
        } catch (IOException | RuntimeException e) {
            fail(e);
        }
    }

    private void finishCancelled() {
        state.setPhase(Phase.CANCELLED);
        events.accept(new SyncEvent.UpdateCancelled(packId));
    }

    private void fail(Exception e) {
        SyncFailure failure = SyncFailures.from(e, "");
        LOG.log(System.Logger.Level.WARNING, "Update of pack " + packId + " failed: " + failure.describe(), e);
        state.setPhase(Phase.FAILED);
        events.accept(new SyncEvent.UpdateFailed(packId, failure));
    }
}
