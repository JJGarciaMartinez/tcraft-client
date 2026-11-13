package hopsync.desktop.store;

import hopsync.core.failure.SyncFailure;

/**
 * What happened to one file of the plan while applying it.
 */
public sealed interface RowStatus {
    record Pending() implements RowStatus {}

    /** @param percent of the file downloaded, in steps of 5 */
    record Downloading(int percent) implements RowStatus {}

    record Done() implements RowStatus {}

    /** @param failure why it failed, or {@code null} when the engine gave no reason */
    record Failed(SyncFailure failure) implements RowStatus {}

    record Removed() implements RowStatus {}
}