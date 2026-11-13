package hopsync.desktop.store;

import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.PackInfo;
import hopsync.core.pack.Pack;
import hopsync.core.sync.SyncPlan;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Everything the UI shows about one pack. Immutable: the reducer returns a new instance on each change.
 *
 * @param manifest      the last manifest known (from the network or the cache), or {@code null}
 * @param manifestError why the manifest could not be fetched for display, or {@code null}
 * @param plan          the last plan computed, or {@code null} before the first check
 * @param installed     what is in the pack's {@code mods/} folder
 * @param progress      while applying
 * @param rowStatus     per file, only for files the running or last apply touched
 * @param result        counts of the last finished apply
 * @param error         why the last update stopped, in {@link PackPhase#ERROR}
 * @param lastChecked   when the last check finished, or {@code null}
 */
public record PackUiState(String packId, String name, String manifestUrl, Path gameDir, PackPhase phase,
                          Manifest manifest, SyncFailure manifestError, SyncPlan plan, InstalledMods installed,
                          Progress progress, Map<String, RowStatus> rowStatus, Result result, SyncFailure error,
                          Instant lastChecked) {

    /** @param currentFile the file being downloaded, or {@code null} */
    public record Progress(int percent, String currentFile) {
        public static final Progress NONE = new Progress(0, null);
    }

    public record Result(int succeeded, int failed) {
        public static final Result NONE = new Result(0, 0);
    }

    public PackUiState {
        rowStatus = Map.copyOf(rowStatus);
    }

    /** Pack details from the manifest, or {@code null} before it is known. */
    public PackInfo packInfo() {
        return manifest == null ? null : manifest.pack();
    }

    /** Mods the pack lists, 0 before the manifest is known. */
    public int modCount() {
        return manifest == null ? 0 : manifest.mods().size();
    }

    /** Name to show: from the manifest when known, else the cached one, else the link. */
    public String displayName() {
        if (manifest != null) return manifest.pack().name();
        return name.isEmpty() ? manifestUrl : name;
    }

    public static PackUiState of(Pack pack) {
        return new PackUiState(pack.id(), pack.name(), pack.manifestUrl(), pack.gameDir(), PackPhase.UNCHECKED,
            null, null, null, InstalledMods.NONE, Progress.NONE, Map.of(), Result.NONE, null, null);
    }

    /** Same pack with its link, folder and name taken from the registry. */
    PackUiState withPack(Pack pack) {
        return new PackUiState(packId, pack.name(), pack.manifestUrl(), pack.gameDir(), phase, manifest,
            manifestError, plan, installed, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withPhase(PackPhase newPhase) {
        return new PackUiState(packId, name, manifestUrl, gameDir, newPhase, manifest, manifestError,
            plan, installed, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withManifest(Manifest newManifest, SyncFailure newError) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, newManifest, newError, plan, installed,
            progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withPlan(SyncPlan newPlan, Instant checkedAt) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, newPlan.manifest(), null, newPlan,
            installed, progress, rowStatus, result, error, checkedAt);
    }

    PackUiState withInstalled(InstalledMods files) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, manifest, manifestError, plan,
            files, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withProgress(Progress newProgress) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, manifest, manifestError, plan,
            installed, newProgress, rowStatus, result, error, lastChecked);
    }

    PackUiState withRowStatus(Map<String, RowStatus> rows) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, manifest, manifestError, plan,
            installed, progress, rows, result, error, lastChecked);
    }

    PackUiState withResult(Result newResult) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, manifest, manifestError, plan,
            installed, progress, rowStatus, newResult, error, lastChecked);
    }

    PackUiState withError(SyncFailure failure) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, manifest, manifestError, plan,
            installed, progress, rowStatus, result, failure, lastChecked);
    }
}
