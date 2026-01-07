package service;

import event.EventBus;
import event.UpdateEvent;
import state.UpdateState;

/**
 * Coordinates the update process and publishes events.
 * This is the single source of truth for update state transitions.
 */
public class UpdateCoordinator {
    private final EventBus eventBus;
    private final UpdateState state;
    private final ModUpdateService modUpdateService;

    public UpdateCoordinator(EventBus eventBus, ModUpdateService modUpdateService) {
        this.eventBus = eventBus;
        this.state = new UpdateState();
        this.modUpdateService = modUpdateService;
    }

    public void startUpdate() {
        if (state.isRunning()) return;

        state.reset();
        state.setPhase(UpdateState.Phase.STARTING);
        eventBus.publish(new UpdateEvent.UpdateStarted());

        Thread thread = new Thread(this::executeUpdate);
        state.setUpdateThread(thread);
        thread.start();
    }

    public void cancelUpdate() {
        if (state.isRunning()) {
            state.cancel();
            state.setPhase(UpdateState.Phase.CANCELLED);
            eventBus.publish(new UpdateEvent.UpdateCancelled());
        }
    }

    private void executeUpdate() {
        try {
            state.setPhase(UpdateState.Phase.DOWNLOADING);
            modUpdateService.performUpdate(this::handleEvent);

            state.setPhase(UpdateState.Phase.COMPLETED);
            eventBus.publish(new UpdateEvent.UpdateCompleted(
                state.getCompletedCount(),
                state.getFailedCount()
            ));
        } catch (Exception e) {
            state.setPhase(UpdateState.Phase.FAILED);
            eventBus.publish(new UpdateEvent.UpdateFailed(e.getMessage()));
        }
    }

    private void handleEvent(UpdateEvent event) {
        if (event instanceof UpdateEvent.ModCompleted) {
            UpdateEvent.ModCompleted mc = (UpdateEvent.ModCompleted) event;
            if (mc.success()) state.incrementCompleted();
            else state.incrementFailed();
        }
        eventBus.publish(event);
    }

    public UpdateState getState() { return state; }
    public boolean isRunning() { return state.isRunning(); }
}
