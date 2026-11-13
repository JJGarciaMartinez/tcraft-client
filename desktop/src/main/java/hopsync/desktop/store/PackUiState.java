package hopsync.desktop.store;

import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.PackInfo;
import hopsync.core.pack.Pack;
import hopsync.core.sync.SyncPlan;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Everything the UI shows about one pack. Immutable: the reducer returns a new instance on each change.
 *
 * @param packInfo      pack details from the last manifest fetched, or {@code null}
 * @param modCount      mods in the last manifest fetched, 0 until then
 * @param packInfoError why the manifest could not be fetched for display, or {@code null}
 * @param plan          the last plan computed, or {@code null} before the first check
 * @param installed     jar files found in the pack's {@code mods/} folder
 * @param progress      while applying
 * @param rowStatus     per file, only for files the running or last apply touched
 * @param result        counts of the last finished apply
 * @param error         why the last update stopped, in {@link PackPhase#ERROR}
 * @param lastChecked   when the last check finished, or {@code null}
 */
public record PackUiState(String packId, String name, String manifestUrl, Path gameDir, PackPhase phase,
                          PackInfo packInfo, int modCount, SyncFailure packInfoError, SyncPlan plan, List<String> installed,
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
        installed = List.copyOf(installed);
        rowStatus = Map.copyOf(rowStatus);
    }

    /** Name to show: from the manifest when fetched, else the cached one, else the link. */
    public String displayName() {
        if (packInfo != null) return packInfo.name();
        return name.isEmpty() ? manifestUrl : name;
    }

    public static PackUiState of(Pack pack) {
        return new PackUiState(pack.id(), pack.name(), pack.manifestUrl(), pack.gameDir(), PackPhase.UNCHECKED,
            null, 0, null, null, List.of(), Progress.NONE, Map.of(), Result.NONE, null, null);
    }

    /** Same pack with its link, folder and name taken from the registry. */
    PackUiState withPack(Pack pack) {
        return new PackUiState(packId, pack.name(), pack.manifestUrl(), pack.gameDir(), phase, packInfo, modCount,
            packInfoError, plan, installed, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withPhase(PackPhase newPhase) {
        return new PackUiState(packId, name, manifestUrl, gameDir, newPhase, packInfo, modCount, packInfoError,
            plan, installed, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withPackInfo(PackInfo info, int mods, SyncFailure infoError) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, info, mods, infoError, plan, installed,
            progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withPlan(SyncPlan newPlan, Instant checkedAt) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, newPlan.manifest().pack(),
            newPlan.manifest().mods().size(), null, newPlan, installed, progress, rowStatus, result, error, checkedAt);
    }

    PackUiState withInstalled(List<String> files) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, packInfo, modCount, packInfoError, plan,
            files, progress, rowStatus, result, error, lastChecked);
    }

    PackUiState withProgress(Progress newProgress) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, packInfo, modCount, packInfoError, plan,
            installed, newProgress, rowStatus, result, error, lastChecked);
    }

    PackUiState withRowStatus(Map<String, RowStatus> rows) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, packInfo, modCount, packInfoError, plan,
            installed, progress, rows, result, error, lastChecked);
    }

    PackUiState withResult(Result newResult) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, packInfo, modCount, packInfoError, plan,
            installed, progress, rowStatus, newResult, error, lastChecked);
    }

    PackUiState withError(SyncFailure failure) {
        return new PackUiState(packId, name, manifestUrl, gameDir, phase, packInfo, modCount, packInfoError, plan,
            installed, progress, rowStatus, result, failure, lastChecked);
    }
}
