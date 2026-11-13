package hopsync.desktop.store;

/**
 * Where a pack is in its check → review → apply cycle, as the UI shows it.
 */
public enum PackPhase {
    UNCHECKED, CHECKING, REVIEW, QUEUED, UP_TO_DATE, APPLYING, DONE, DONE_WITH_ERRORS, CANCELLED, ERROR;

    /** True while an update is in progress: the pack's settings cannot change and cancel is offered. */
    public boolean isBusy() {
        return this == CHECKING || this == REVIEW || this == QUEUED || this == APPLYING;
    }
}