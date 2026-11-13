package hopsync.desktop.screens;

import hopsync.desktop.config.AppVersion;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.desktop.store.UiState.Sheet;
import hopsync.platform.system.SystemInfoService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Opens the dialog {@link UiState#sheet()} asks for and reports how it closed with actions. Dialogs are
 * modal and run their own event loop, so they open with invokeLater, after the store has finished handling
 * the action that asked for them.
 */
public final class Sheets {

    /** Opens the pack settings dialog in the given mode; returns true if the player saved. */
    @FunctionalInterface
    public interface SettingsOpener {
        boolean open(Window owner, Mode mode);
    }

    public enum Mode { FIRST_RUN, ADD, EDIT }

    private final Window owner;
    private final Consumer<UiAction> dispatch;
    private final SettingsOpener settings;
    private final Path settingsFile;
    private Sheet open = Sheet.NONE;

    Sheets(Window owner, Consumer<UiAction> dispatch, SettingsOpener settings, Path settingsFile) {
        this.owner = owner;
        this.dispatch = dispatch;
        this.settings = settings;
        this.settingsFile = settingsFile;
    }

    void render(UiState state) {
        if (state.sheet() == open) return;
        open = state.sheet();
        switch (state.sheet()) {
            case SETTINGS -> openSettings(Mode.EDIT);
            case SETTINGS_FIRST_RUN -> openSettings(Mode.FIRST_RUN);
            case ADD_PACK -> openSettings(Mode.ADD);
            case REMOVE_PACK -> state.selected().ifPresent(this::confirmRemove);
            case ABOUT -> SwingUtilities.invokeLater(this::showAbout);
            case NONE -> { }
        }
    }

    private void openSettings(Mode mode) {
        SwingUtilities.invokeLater(() -> dispatch.accept(new UiAction.SheetClosed(settings.open(owner, mode))));
    }

    private void confirmRemove(PackUiState pack) {
        SwingUtilities.invokeLater(() -> {
            Object[] options = {Messages.get("remove.confirm"), Messages.get("remove.cancel")};
            int choice = JOptionPane.showOptionDialog(owner, Messages.get("remove.message", pack.displayName()),
                Messages.get("remove.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                options, options[1]);
            if (choice == 0) {
                dispatch.accept(new UiAction.RemovePack(pack.packId()));
            }
            dispatch.accept(new UiAction.SheetClosed(false));
        });
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(owner, Messages.get("about.body", AppVersion.VERSION_APP,
                SystemInfoService.getOS(), System.getProperty("java.version"), settingsFile),
            Messages.get("about.title"), JOptionPane.INFORMATION_MESSAGE);
        dispatch.accept(new UiAction.SheetClosed(false));
    }
}
