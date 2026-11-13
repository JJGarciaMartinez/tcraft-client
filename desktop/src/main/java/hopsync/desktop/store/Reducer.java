package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.pack.Pack;
import hopsync.core.sync.SyncPlan;
import hopsync.desktop.store.PackUiState.Progress;
import hopsync.desktop.store.PackUiState.Result;
import hopsync.desktop.store.UiState.Sheet;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Computes the next UI state from the current one and an action. Pure: no Swing, no I/O, no clock, so
 * every transition can be tested on its own.
 */
public final class Reducer {

    private Reducer() {
    }

    public static UiState reduce(UiState state, UiAction action) {
        return switch (action) {
            case UiAction.SyncEventReceived a -> onSyncEvent(state, a.event(), a.at());
            case UiAction.PacksLoaded a -> onPacksLoaded(state, a);
            case UiAction.InstalledModsLoaded a -> updatePack(state, a.packId(), pack -> pack.withInstalled(a.mods()));
            case UiAction.ManifestLoaded a -> updatePack(state, a.packId(), pack -> pack.withManifest(a.manifest(), null));
            // The last known manifest (maybe cached) keeps the list on screen; the header shows the failure
            case UiAction.ManifestFailed a -> updatePack(state, a.packId(),
                pack -> pack.withManifest(pack.manifest(), a.failure()));
            // Commands handled by Effects; the state follows from the core events they cause
            case UiAction.CheckPack a -> state;
            case UiAction.CheckAll a -> state;
            case UiAction.ConfirmPlan a -> state;
            case UiAction.DismissPlan a -> state;
            case UiAction.CancelUpdate a -> state;
            case UiAction.ReloadInstalled a -> state;
            case UiAction.OpenFolder a -> state;
            case UiAction.CopyServerIp a -> state;
            case UiAction.CopyPackLink a -> state;
            case UiAction.OpenModPage a -> state;
            case UiAction.RemovePack a -> state;
            case UiAction.OpenSettings a -> openForSelectedPack(state,
                state.packs().isEmpty() ? Sheet.SETTINGS_FIRST_RUN : Sheet.SETTINGS);
            case UiAction.AddPack a -> state.withSheet(Sheet.ADD_PACK);
            case UiAction.AskRemovePack a -> state.packs().isEmpty() ? state
                : openForSelectedPack(state, Sheet.REMOVE_PACK);
            case UiAction.OpenAbout a -> state.withSheet(Sheet.ABOUT);
            case UiAction.SheetClosed a -> state.withSheet(Sheet.NONE);
            case UiAction.ShowToast a -> state.withToast(a.text());
            case UiAction.SelectPack a -> state.packs().containsKey(a.packId())
                ? state.withSelected(a.packId()).withFilter(UiState.Filter.CHANGES) : state;
            case UiAction.SelectTab a -> state.withTab(a.tab());
            case UiAction.SetFilter a -> state.withFilter(a.filter());
            case UiAction.SetQuery a -> state.withQuery(a.query());
            case UiAction.ToggleRow a -> state.withExpandedRow(Objects.equals(state.expandedRow(), a.file()) ? null : a.file());
        };
    }

    private static UiState onSyncEvent(UiState state, SyncEvent event, Instant at) {
        PackUiState pack = state.packs().get(event.packId());
        if (pack == null) {
            // A pack removed while its last events were still on the way
            return state;
        }
        UiState next = state.withPack(onPackEvent(pack, event, at));
        if (event instanceof SyncEvent.PlanReady && event.packId().equals(state.selectedPackId())) {
            // A new plan opens on what will change
            return next.withFilter(UiState.Filter.CHANGES);
        }
        return next;
    }

    private static PackUiState onPackEvent(PackUiState pack, SyncEvent event, Instant at) {
        return switch (event) {
            case SyncEvent.UpdateStarted e -> pack.withPhase(PackPhase.CHECKING).withError(null)
                .withProgress(Progress.NONE).withRowStatus(Map.of()).withResult(Result.NONE);
            case SyncEvent.PlanReady e -> pack.withPlan(e.plan(), at).withPhase(PackPhase.REVIEW);
            case SyncEvent.UpToDate e -> pack.withPlan(e.plan(), at).withPhase(PackPhase.UP_TO_DATE);
            case SyncEvent.Queued e -> pack.withPhase(PackPhase.QUEUED);
            case SyncEvent.ApplyStarted e -> pack.withPhase(PackPhase.APPLYING).withProgress(Progress.NONE)
                .withRowStatus(pendingRows(pack.plan()));
            case SyncEvent.ProgressChanged e -> pack.withProgress(new Progress(e.percent(), pack.progress().currentFile()));
            case SyncEvent.ModDownloading e -> pack.withRowStatus(withRow(pack, e.file(), new RowStatus.Downloading()))
                .withProgress(new Progress(pack.progress().percent(), e.file()));
            case SyncEvent.ModCompleted e -> pack.withRowStatus(withRow(pack, e.file(),
                e.success() ? new RowStatus.Done() : new RowStatus.Failed(e.failure())));
            case SyncEvent.ModRemoved e -> pack.withRowStatus(withRow(pack, e.file(), new RowStatus.Removed()));
            case SyncEvent.UpdateCompleted e -> pack
                .withPhase(e.failCount() > 0 ? PackPhase.DONE_WITH_ERRORS : PackPhase.DONE)
                .withResult(new Result(e.successCount(), e.failCount()))
                .withProgress(new Progress(100, null));
            case SyncEvent.UpdateCancelled e -> pack.withPhase(PackPhase.CANCELLED).withProgress(Progress.NONE);
            case SyncEvent.UpdateFailed e -> pack.withPhase(PackPhase.ERROR).withError(e.failure())
                .withProgress(Progress.NONE);
            case SyncEvent.LogMessage e -> pack;
        };
    }

    private static UiState onPacksLoaded(UiState state, UiAction.PacksLoaded loaded) {
        Map<String, PackUiState> packs = new LinkedHashMap<>();
        for (Pack pack : loaded.packs()) {
            PackUiState known = state.packs().get(pack.id());
            boolean samePack = known != null && known.manifestUrl().equals(pack.manifestUrl())
                && known.gameDir().equals(pack.gameDir());
            // A new link or folder makes what was checked before meaningless
            packs.put(pack.id(), samePack ? known.withPack(pack) : PackUiState.of(pack));
        }
        String selected = packs.containsKey(loaded.activeId()) ? loaded.activeId()
            : packs.containsKey(state.selectedPackId()) ? state.selectedPackId()
            : packs.keySet().stream().findFirst().orElse(null);
        UiState next = state.withPacks(packs, selected);
        if (packs.isEmpty() && state.sheet() == Sheet.NONE) {
            return next.withSheet(Sheet.SETTINGS_FIRST_RUN);
        }
        return next;
    }

    private static UiState openForSelectedPack(UiState state, Sheet sheet) {
        if (state.selected().map(pack -> pack.phase().isBusy()).orElse(false)) {
            // The pack being updated keeps its link and folder until it finishes
            return state;
        }
        return state.withSheet(sheet);
    }

    private static UiState updatePack(UiState state, String packId, UnaryOperator<PackUiState> change) {
        PackUiState pack = state.packs().get(packId);
        return pack == null ? state : state.withPack(change.apply(pack));
    }

    private static Map<String, RowStatus> pendingRows(SyncPlan plan) {
        Map<String, RowStatus> rows = new HashMap<>();
        if (plan != null) {
            plan.modDownloads().forEach(action -> rows.put(action.mod().file(), new RowStatus.Pending()));
        }
        return rows;
    }

    private static Map<String, RowStatus> withRow(PackUiState pack, String file, RowStatus status) {
        Map<String, RowStatus> rows = new HashMap<>(pack.rowStatus());
        rows.put(file, status);
        return rows;
    }
}