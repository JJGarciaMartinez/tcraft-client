package hopsync.core.event;

import hopsync.core.sync.SyncPlan;

/**
 * All possible events during the update process.
 * Sealed so that switches over events are checked for exhaustiveness.
 */
public sealed interface UpdateEvent {
    /** The update began; the manifest is being fetched and compared with local files. */
    record UpdateStarted() implements UpdateEvent {}
    /** There are changes to apply; waiting for the player to confirm them. */
    record PlanReady(SyncPlan plan) implements UpdateEvent {}
    /** Nothing to change: every file already matches the manifest. */
    record UpToDate(SyncPlan plan) implements UpdateEvent {}
    /** The player confirmed the plan and files are being written. */
    record ApplyStarted() implements UpdateEvent {}
    record UpdateCompleted(int successCount, int failCount) implements UpdateEvent {}
    record UpdateCancelled() implements UpdateEvent {}
    record UpdateFailed(String error) implements UpdateEvent {}
    record ProgressChanged(int percent) implements UpdateEvent {}
    record ModDownloading(String file) implements UpdateEvent {}
    record ModCompleted(String file, boolean success) implements UpdateEvent {}
    record ModRemoved(String file) implements UpdateEvent {}
    record LogMessage(String message) implements UpdateEvent {}
}
