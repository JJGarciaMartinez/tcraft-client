package hopsync.desktop.screens.pack;

import hopsync.core.sync.SyncPlan;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.store.PackPhase;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.RowStatus;
import hopsync.desktop.ui.ModInfo;
import hopsync.desktop.ui.StatusType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The cards of the current mod list for a pack, computed only from its state, so the list always matches
 * the state whatever order the events arrived in. Replaced by the mod table in phase 5.
 */
final class LegacyRows {

    enum Tone { ACCENT, ADDED, UPDATE, ERROR }

    record Row(ModInfo info, StatusType type, String text, Tone tone) {
    }

    /** Phases in which the list shows the plan; otherwise it shows what is on disk. */
    private static final Set<PackPhase> SHOWS_PLAN = Set.of(PackPhase.REVIEW, PackPhase.QUEUED, PackPhase.APPLYING,
        PackPhase.UP_TO_DATE, PackPhase.DONE, PackPhase.DONE_WITH_ERRORS);

    private LegacyRows() {
    }

    static List<Row> of(PackUiState pack) {
        if (pack.plan() != null && SHOWS_PLAN.contains(pack.phase())) {
            return planRows(pack);
        }
        return pack.installed().stream()
            .map(file -> new Row(ModInfo.installed(file), StatusType.IN_SYSTEM, Messages.get("mod.installed"), Tone.ACCENT))
            .toList();
    }

    private static List<Row> planRows(PackUiState pack) {
        List<Row> rows = new ArrayList<>();
        for (SyncPlan.ModAction action : pack.plan().mods()) {
            ModInfo info = ModInfo.of(action.mod());
            RowStatus status = pack.rowStatus().get(info.file());
            rows.add(status == null || status instanceof RowStatus.Pending
                ? planned(info, action.change())
                : applied(info, status));
        }
        for (String file : pack.plan().removals()) {
            boolean removed = pack.rowStatus().get(file) instanceof RowStatus.Removed;
            rows.add(new Row(ModInfo.installed(file), StatusType.WARNING,
                Messages.get(removed ? "mod.removed" : "mod.willRemove"), Tone.UPDATE));
        }
        return rows;
    }

    private static Row planned(ModInfo info, SyncPlan.ModChange change) {
        return switch (change) {
            case UP_TO_DATE -> new Row(info, StatusType.SUCCESS, Messages.get("mod.upToDate"), Tone.ACCENT);
            case NEW -> new Row(info, StatusType.PROCESSING, Messages.get("mod.new"), Tone.ADDED);
            case UPDATE -> new Row(info, StatusType.PROCESSING, Messages.get("mod.willUpdate"), Tone.UPDATE);
        };
    }

    private static Row applied(ModInfo info, RowStatus status) {
        return switch (status) {
            case RowStatus.Downloading s -> new Row(info, StatusType.PROCESSING, Messages.get("mod.downloading"), Tone.ADDED);
            case RowStatus.Done s -> new Row(info, StatusType.SUCCESS, Messages.get("mod.done"), Tone.ACCENT);
            case RowStatus.Failed s -> new Row(info, StatusType.ERROR,
                s.failure() == null ? Messages.get("mod.failed") : Messages.failure(s.failure()), Tone.ERROR);
            case RowStatus.Removed s -> new Row(info, StatusType.WARNING, Messages.get("mod.removed"), Tone.UPDATE);
            case RowStatus.Pending s -> throw new IllegalArgumentException("pending rows show the plan");
        };
    }
}
