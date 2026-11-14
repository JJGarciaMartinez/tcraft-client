package hopsync.core.event;

import hopsync.core.failure.SyncFailure;
import hopsync.core.sync.SyncPlan;

/**
 * Everything that happens while a pack is checked and updated. Each event names its pack, so the UI can
 * tell several packs apart. Sealed so that switches over events are checked for exhaustiveness.
 */
public sealed interface SyncEvent {
    /** Id of the {@link hopsync.core.pack.Pack} the event belongs to. */
    String packId();

    /** The update began; the manifest is being fetched and compared with local files. */
    record UpdateStarted(String packId) implements SyncEvent {}
    /** There are changes to apply; waiting for the player to confirm them. */
    record PlanReady(String packId, SyncPlan plan) implements SyncEvent {}
    /** Nothing to change: every file already matches the manifest. */
    record UpToDate(String packId, SyncPlan plan) implements SyncEvent {}
    /** The player confirmed the plan, but another pack is applying; this one starts after it. */
    record Queued(String packId) implements SyncEvent {}
    /** The player confirmed the plan and files are being written. */
    record ApplyStarted(String packId) implements SyncEvent {}
    record UpdateCompleted(String packId, int successCount, int failCount) implements SyncEvent {}
    record UpdateCancelled(String packId) implements SyncEvent {}
    /** The update stopped before finishing; nothing beyond what the state records was changed. */
    record UpdateFailed(String packId, SyncFailure failure) implements SyncEvent {}
    /**
     * How far the apply has got.
     *
     * @param doneFiles      files processed so far (downloaded, failed or removed)
     * @param bytesPerSecond average download speed of the last seconds
     */
    record Progress(String packId, int doneFiles, int totalFiles, long bytesDone, long bytesTotal,
                    long bytesPerSecond) implements SyncEvent {}
    /** One mod's download, in steps of 5 %. */
    record ModProgress(String packId, String file, int percent) implements SyncEvent {}
    record ModDownloading(String packId, String file) implements SyncEvent {}
    /** @param failure why the mod could not be installed, or {@code null} on success */
    record ModCompleted(String packId, String file, boolean success, SyncFailure failure) implements SyncEvent {}
    record ModRemoved(String packId, String file) implements SyncEvent {}
    record LogMessage(String packId, String message) implements SyncEvent {}
}
