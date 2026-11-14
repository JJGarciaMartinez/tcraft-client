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

    /**
     * True only while files are being written. Until then a check, a plan or a place in the queue can be
     * dropped, so the pack's link and folder can still change and the pack can be removed.
     */
    public boolean isWriting() {
        return this == APPLYING;
    }
}