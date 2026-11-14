package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.pack.Pack;
import hopsync.core.sync.SyncPlan;
import hopsync.desktop.store.PackUiState.Progress;
import hopsync.desktop.store.PackUiState.Result;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            case UiAction.ContinueUpdate a -> state;
            case UiAction.Quit a -> state;
            case UiAction.ReloadInstalled a -> state;
            case UiAction.OpenFolder a -> state;
            case UiAction.CopyServerIp a -> state;
            case UiAction.CopyPackLink a -> state;
            case UiAction.OpenModPage a -> state;
            case UiAction.RemovePack a -> state.withSheet(null);
            case UiAction.OpenSettings a -> state.packs().isEmpty() ? state.withSheet(Sheet.AddPack.start(true))
                : openPackSettings(state, Sheet.PackSettings.Section.LINK);
            case UiAction.AddPack a -> state.withSheet(Sheet.AddPack.start(state.packs().isEmpty()));
            case UiAction.AskRemovePack a -> state.packs().isEmpty() ? state
                : openPackSettings(state, Sheet.PackSettings.Section.REMOVE);
            case UiAction.OpenAppSettings a -> state.withSheet(new Sheet.AppSettings(a.section(), state.preferences()));
            case UiAction.CloseSheet a -> state.sheet() instanceof Sheet.AddPack wizard && wizard.firstRun() ? state
                : state.withSheet(null);
            case UiAction.SheetSaved a -> state.withSheet(null);
            case UiAction.SheetFailed a -> switch (state.sheet()) {
                case Sheet.AddPack wizard -> state.withSheet(wizard.withError(a.failure()));
                case Sheet.PackSettings settings -> state.withSheet(settings.withError(a.failure()));
                case null, default -> state;
            };
            case UiAction.SetLinkUrl a -> switch (state.sheet()) {
                case Sheet.AddPack wizard -> state.withSheet(wizard.withUrl(a.url(), new LinkCheck.Unchecked()));
                case Sheet.PackSettings settings -> state.withSheet(settings.withUrl(a.url(), new LinkCheck.Unchecked()));
                case null, default -> state;
            };
            case UiAction.CheckLink a -> checkLink(state);
            case UiAction.LinkChecked a -> withLinkResult(state, a.url(), valid(state, a.url(), a.manifest()));
            case UiAction.LinkFailed a -> withLinkResult(state, a.url(),
                new LinkCheck.Invalid(LinkCheck.Problem.UNREADABLE, a.failure()));
            case UiAction.WizardNext a -> state.sheet() instanceof Sheet.AddPack wizard && wizard.step() == 1
                && wizard.link() instanceof LinkCheck.Valid ? state.withSheet(wizard.withStep(2)) : state;
            case UiAction.WizardBack a -> state.sheet() instanceof Sheet.AddPack wizard
                ? state.withSheet(wizard.withStep(1)) : state;
            case UiAction.FoldersFound a -> state.sheet() instanceof Sheet.AddPack wizard && wizard.step() == 2
                ? state.withSheet(wizard.withFolders(a.folders())) : state;
            case UiAction.ChooseFolder a -> chooseFolder(state, a.path());
            case UiAction.FolderChecked a -> folderChecked(state, a.folder());
            case UiAction.SelectPackSection a -> state.sheet() instanceof Sheet.PackSettings settings
                ? state.withSheet(settings.withSection(a.section())) : state;
            case UiAction.SetPreferences a -> state.sheet() instanceof Sheet.AppSettings settings
                ? state.withSheet(new Sheet.AppSettings(settings.section(), a.preferences())) : state;
            // Saved by Effects, which answers with SheetSaved or SheetFailed
            case UiAction.FinishAddPack a -> state;
            case UiAction.SavePackSettings a -> state;
            case UiAction.SaveAppSettings a -> state;
            case UiAction.CopyDiagnostics a -> state;
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
            case SyncEvent.Progress e -> pack.withProgress(new Progress(e.doneFiles(), e.totalFiles(), e.bytesDone(),
                e.bytesTotal(), e.bytesPerSecond(), pack.progress().currentFile()));
            case SyncEvent.ModDownloading e -> pack.withRowStatus(withRow(pack, e.file(), new RowStatus.Downloading(0)))
                .withProgress(pack.progress().withCurrentFile(e.file()));
            // A late step after the file finished must not turn it back into a download
            case SyncEvent.ModProgress e -> pack.rowStatus().get(e.file()) instanceof RowStatus.Downloading
                ? pack.withRowStatus(withRow(pack, e.file(), new RowStatus.Downloading(e.percent()))) : pack;
            case SyncEvent.ModCompleted e -> pack.withRowStatus(withRow(pack, e.file(),
                e.success() ? new RowStatus.Done() : new RowStatus.Failed(e.failure())));
            case SyncEvent.ModRemoved e -> pack.withRowStatus(withRow(pack, e.file(), new RowStatus.Removed()));
            case SyncEvent.UpdateCompleted e -> pack
                .withPhase(e.failCount() > 0 ? PackPhase.DONE_WITH_ERRORS : PackPhase.DONE)
                .withResult(new Result(e.successCount(), e.failCount()))
                .withProgress(pack.progress().withCurrentFile(null));
            // The counts stay, so the header can say how much was applied before the cancel
            case SyncEvent.UpdateCancelled e -> pack.withPhase(PackPhase.CANCELLED)
                .withProgress(pack.progress().withCurrentFile(null));
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
        UiState next = state.withPacks(packs, selected).withPreferences(loaded.preferences());
        if (packs.isEmpty() && state.sheet() == null) {
            return next.withSheet(Sheet.AddPack.start(true));
        }
        return next;
    }

    private static UiState openPackSettings(UiState state, Sheet.PackSettings.Section section) {
        PackUiState pack = state.selected().orElse(null);
        if (pack == null || pack.phase().isWriting()) {
            // The pack writing files keeps its link and folder until it finishes
            return state;
        }
        GameFolder folder = new GameFolder(GameFolder.Kind.OTHER, null, pack.gameDir(), pack.installed().jars().size(),
            null);
        return state.withSheet(new Sheet.PackSettings(pack.packId(), section, pack.manifestUrl(),
            new LinkCheck.Unchecked(), folder, null));
    }

    /**
     * Problems the player can fix without the network are answered at once; otherwise the link is marked as
     * being checked and Effects fetches it.
     */
    private static UiState checkLink(UiState state) {
        String url = switch (state.sheet()) {
            case Sheet.AddPack wizard -> wizard.url().trim();
            case Sheet.PackSettings settings -> settings.url().trim();
            case null, default -> null;
        };
        if (url == null) return state;
        LinkCheck result;
        if (url.isEmpty()) {
            result = new LinkCheck.Invalid(LinkCheck.Problem.EMPTY, null);
        } else if (!url.startsWith("https://")) {
            result = new LinkCheck.Invalid(LinkCheck.Problem.NOT_HTTPS, null);
        } else {
            result = new LinkCheck.Checking(url);
        }
        return withLink(state, url, result);
    }

    private static UiState withLink(UiState state, String url, LinkCheck link) {
        return switch (state.sheet()) {
            case Sheet.AddPack wizard -> state.withSheet(wizard.withUrl(url, link));
            case Sheet.PackSettings settings -> state.withSheet(settings.withUrl(url, link));
            case null, default -> state;
        };
    }

    /** Only the answer for the link still being checked counts; the player may have typed another since. */
    private static UiState withLinkResult(UiState state, String url, LinkCheck result) {
        LinkCheck current = switch (state.sheet()) {
            case Sheet.AddPack wizard -> wizard.link();
            case Sheet.PackSettings settings -> settings.link();
            case null, default -> null;
        };
        if (!(current instanceof LinkCheck.Checking checking) || !checking.url().equals(url)) {
            return state;
        }
        return switch (state.sheet()) {
            case Sheet.AddPack wizard -> state.withSheet(wizard.withLink(result));
            case Sheet.PackSettings settings -> state.withSheet(settings.withLink(result));
            default -> state;
        };
    }

    /** The same pack may be added again for another folder; the copies are listed so the player knows. */
    private static LinkCheck valid(UiState state, String url, Manifest manifest) {
        long bytes = manifest.mods().stream().mapToLong(ModEntry::size).sum()
            + manifest.configs().stream().mapToLong(ConfigEntry::size).sum();
        String ownPackId = state.sheet() instanceof Sheet.PackSettings settings ? settings.packId() : null;
        List<Path> copies = state.packs().values().stream()
            .filter(pack -> pack.manifestUrl().equals(url) && !pack.packId().equals(ownPackId))
            .map(PackUiState::gameDir)
            .toList();
        return new LinkCheck.Valid(url, manifest.pack(), manifest.mods().size(), bytes, copies);
    }

    private static UiState chooseFolder(UiState state, Path path) {
        return switch (state.sheet()) {
            case Sheet.AddPack wizard -> state.withSheet(wizard.withChosen(Optional.ofNullable(wizard.folders())
                .flatMap(found -> found.stream().filter(folder -> folder.path().equals(path)).findFirst())
                .orElse(new GameFolder(GameFolder.Kind.OTHER, null, path, 0, null))));
            case Sheet.PackSettings settings -> state.withSheet(settings.withFolder(
                new GameFolder(GameFolder.Kind.OTHER, null, path, 0, null)));
            case null, default -> state;
        };
    }

    /** Effects' answer about who uses a folder, for the folder still chosen. */
    private static UiState folderChecked(UiState state, GameFolder checked) {
        return switch (state.sheet()) {
            case Sheet.AddPack wizard when wizard.chosen() != null && wizard.chosen().path().equals(checked.path()) ->
                state.withSheet(wizard.withChosen(sameKind(wizard.chosen(), checked)));
            case Sheet.PackSettings settings when settings.folder().path().equals(checked.path()) ->
                state.withSheet(settings.withFolder(sameKind(settings.folder(), checked)));
            case null, default -> state;
        };
    }

    /** A launcher instance stays one after the check, which only knows the path. */
    private static GameFolder sameKind(GameFolder chosen, GameFolder checked) {
        return new GameFolder(chosen.kind(), chosen.name(), checked.path(), checked.jarCount(), checked.usedBy());
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