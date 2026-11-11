package hopsync.event;

import hopsync.model.ModInfo;

/**
 * All possible events during the update process.
 * Sealed so that switches over events are checked for exhaustiveness.
 */
public sealed interface UpdateEvent {
    record UpdateStarted() implements UpdateEvent {}
    record UpdateCompleted(int successCount, int failCount) implements UpdateEvent {}
    record UpdateCancelled() implements UpdateEvent {}
    record UpdateFailed(String error) implements UpdateEvent {}
    record ProgressChanged(int current, int total) implements UpdateEvent {}
    record ModProcessing(ModInfo mod) implements UpdateEvent {}
    record ModDownloading(String modName) implements UpdateEvent {}
    record ModCompleted(String modName, boolean success) implements UpdateEvent {}
    record ModAlreadyInstalled(String modName) implements UpdateEvent {}
    record ModError(String modName, String error) implements UpdateEvent {}
    record LogMessage(String message) implements UpdateEvent {}
}
