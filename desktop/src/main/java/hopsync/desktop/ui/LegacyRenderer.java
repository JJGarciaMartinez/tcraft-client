package hopsync.desktop.ui;

import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.PackInfo;
import hopsync.core.sync.SyncPlan;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.PackPhase;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.desktop.store.UiState.Sheet;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Draws the UI state on the current window (header, mod cards, control panel) until the new screens
 * replace it. Nothing here reacts to core events: {@link #render} compares the new state with the last one
 * it drew and only touches what changed. Buttons and dialogs report back by dispatching actions.
 */
public final class LegacyRenderer {

    /** Opens the pack settings dialog; returns true if the player saved. */
    @FunctionalInterface
    public interface SettingsOpener {
        boolean open(Window owner, boolean firstRun);
    }

    private final LauncherUI ui;
    private final Store store;
    private final SettingsOpener settings;
    private PackUiState renderedPack;
    private List<LegacyRows.Row> renderedRows;
    private String renderedStatus;
    private Sheet openSheet = Sheet.NONE;
    private boolean renderedNoPack;

    public LegacyRenderer(LauncherUI ui, Store store, SettingsOpener settings) {
        this.ui = ui;
        this.store = store;
        this.settings = settings;
        ControlPanel controls = ui.getControlPanel();
        controls.setUpdateButtonListener(e -> store.dispatch(selectedId()
            .<UiAction>map(UiAction.CheckPack::new).orElseGet(UiAction.OpenSettings::new)));
        controls.setCancelButtonListener(e -> selectedId().ifPresent(id -> store.dispatch(new UiAction.CancelUpdate(id))));
        controls.setRefreshButtonListener(e -> selectedId().ifPresent(id -> store.dispatch(new UiAction.ReloadInstalled(id))));
        controls.setSettingsButtonListener(e -> store.dispatch(new UiAction.OpenSettings()));
        store.subscribe(this::render);
    }

    public void render(UiState state) {
        PackUiState pack = state.selected().orElse(null);
        if (pack == null) {
            renderNoPack();
        } else {
            PackUiState before = renderedPack != null && renderedPack.packId().equals(pack.packId()) ? renderedPack : null;
            renderHeader(before, pack);
            renderControls(before, pack);
            renderRows(pack);
            renderedNoPack = false;
        }
        renderedPack = pack;
        renderSheet(state);
    }

    private void renderNoPack() {
        if (renderedNoPack) return;
        ui.getHeaderPanel().showPackError(Messages.failure(new SyncFailure.NotConfigured()));
        ui.getControlPanel().setUpdateInProgress(false);
        ui.getControlPanel().hideProgress();
        setStatus(Messages.get("status.ready"));
        ui.getModListPanel().showEmpty();
        renderedRows = List.of();
        renderedNoPack = true;
    }

    private void renderHeader(PackUiState before, PackUiState pack) {
        HeaderPanel header = ui.getHeaderPanel();
        if (before == null || !before.gameDir().equals(pack.gameDir())) {
            header.setGameDir(pack.gameDir().toFile());
        }
        if (before == null || !Objects.equals(before.packInfo(), pack.packInfo())
                || !Objects.equals(before.packInfoError(), pack.packInfoError())) {
            if (pack.packInfo() != null) {
                header.showPack(pack.packInfo());
            } else if (pack.packInfoError() != null) {
                header.showPackError(Messages.failure(pack.packInfoError()));
            }
        }
        if (before == null || before.installed().size() != pack.installed().size()) {
            header.updateModsStatus(Messages.get("mods.installedCount", pack.installed().size()));
        }
        if (before == null || before.phase() != pack.phase()) {
            switch (pack.phase()) {
                case CHECKING, REVIEW, QUEUED, APPLYING -> header.setStatusProcessing();
                case UP_TO_DATE, DONE -> header.setStatusSuccess();
                case DONE_WITH_ERRORS, CANCELLED -> header.setStatusWarning();
                case ERROR -> header.setStatusError();
                case UNCHECKED -> { }
            }
        }
    }

    private void renderControls(PackUiState before, PackUiState pack) {
        ControlPanel controls = ui.getControlPanel();
        boolean busy = pack.phase().isBusy();
        if (before == null || before.phase().isBusy() != busy) {
            controls.setUpdateInProgress(busy);
        }
        boolean applying = pack.phase() == PackPhase.APPLYING;
        boolean wasApplying = before != null && before.phase() == PackPhase.APPLYING;
        if (applying && (!wasApplying || before.progress().percent() != pack.progress().percent())) {
            controls.updateProgress(pack.progress().percent());
        } else if (!applying && (wasApplying || before == null)) {
            controls.hideProgress();
        }
        setStatus(statusText(pack));
    }

    private void setStatus(String text) {
        if (!text.equals(renderedStatus)) {
            ui.getControlPanel().updateStatus(text);
            renderedStatus = text;
        }
    }

    static String statusText(PackUiState pack) {
        return switch (pack.phase()) {
            case UNCHECKED -> Messages.get("status.ready");
            case CHECKING -> Messages.get("status.checking");
            case REVIEW -> Messages.get("status.review");
            case QUEUED -> Messages.get("status.queued");
            case UP_TO_DATE -> {
                PackInfo info = pack.packInfo();
                yield Messages.get("status.upToDate", info.name(), info.version());
            }
            case APPLYING -> Messages.get("status.applying");
            case DONE -> Messages.get("status.completed");
            case DONE_WITH_ERRORS -> Messages.get("status.completedWithErrors", pack.result().failed());
            case CANCELLED -> Messages.get("status.cancelled");
            case ERROR -> Messages.failure(pack.error());
        };
    }

    private void renderRows(PackUiState pack) {
        List<LegacyRows.Row> rows = LegacyRows.of(pack);
        ModListPanel list = ui.getModListPanel();
        if (!sameCards(renderedRows, rows)) {
            list.clear();
            if (rows.isEmpty()) {
                list.showEmpty();
            }
            for (LegacyRows.Row row : rows) {
                list.addModSync(row.info());
                setRow(list, row);
            }
        } else {
            for (int i = 0; i < rows.size(); i++) {
                if (!rows.get(i).equals(renderedRows.get(i))) {
                    setRow(list, rows.get(i));
                }
            }
        }
        renderedRows = rows;
    }

    private static boolean sameCards(List<LegacyRows.Row> before, List<LegacyRows.Row> rows) {
        if (before == null || before.size() != rows.size()) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (!before.get(i).info().equals(rows.get(i).info())) return false;
        }
        return true;
    }

    private static void setRow(ModListPanel list, LegacyRows.Row row) {
        list.setModStatus(row.info().file(), row.type(), row.text(), color(row.tone()));
    }

    private static Color color(LegacyRows.Tone tone) {
        return switch (tone) {
            case ACCENT -> Palette.accent();
            case ADDED -> Palette.added();
            case UPDATE -> Palette.update();
            case ERROR -> Palette.error();
        };
    }

    /**
     * Opens the dialog the state asks for. Deferred with invokeLater: a modal dialog runs its own event
     * loop, which must not start while the store is still handling the action that opened it.
     */
    private void renderSheet(UiState state) {
        if (state.sheet() == openSheet) return;
        openSheet = state.sheet();
        switch (state.sheet()) {
            case REVIEW -> {
                String packId = state.selectedPackId();
                SyncPlan plan = state.selected().map(PackUiState::plan).orElse(null);
                SwingUtilities.invokeLater(() -> {
                    boolean apply = plan != null && SyncPlanDialog.confirm(ui, plan);
                    store.dispatch(apply ? new UiAction.ConfirmPlan(packId) : new UiAction.DismissPlan(packId));
                });
            }
            case SETTINGS, SETTINGS_FIRST_RUN -> {
                boolean firstRun = state.sheet() == Sheet.SETTINGS_FIRST_RUN;
                SwingUtilities.invokeLater(() -> store.dispatch(new UiAction.SettingsClosed(settings.open(ui, firstRun))));
            }
            case NONE -> { }
        }
    }

    private Optional<String> selectedId() {
        return Optional.ofNullable(store.state().selectedPackId());
    }
}
