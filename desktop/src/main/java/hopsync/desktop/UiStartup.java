package hopsync.desktop;

import hopsync.desktop.effects.Effects;
import hopsync.desktop.effects.ModLoadService;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiState;
import hopsync.desktop.ui.LauncherUI;
import hopsync.desktop.ui.LegacyRenderer;
import hopsync.desktop.ui.SettingsDialog;
import hopsync.platform.fs.DefaultGameDir;

import java.time.Clock;

/**
 * Builds the window and its one-way data flow: core events and clicks become actions, the store reduces
 * them into a new state, the renderer draws it and {@link Effects} performs the side effects.
 */
public final class UiStartup {

    private UiStartup() {
    }

    /** Must run on the EDT. */
    public static void start(AppContainer container) {
        HopSyncDark.setup();
        Store store = new Store(UiState.INITIAL);
        LauncherUI window = new LauncherUI(null);
        new LegacyRenderer(window, store, (owner, firstRun) -> SettingsDialog.show(owner, container.packRegistry(),
            container.manifestService(), container.presetManifestUrl(), container.settingsFile(),
            DefaultGameDir.detect(), firstRun));
        window.setVisible(true);
        new Effects(store, container.packRegistry(), container.packSessions(), container.manifestService(),
            new ModLoadService(), container.backgroundExecutor(), Clock.systemUTC()).start(container.eventBus());
    }
}
