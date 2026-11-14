package hopsync.desktop;

import hopsync.desktop.effects.Effects;
import hopsync.desktop.effects.ModLoadService;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.screens.MainWindow;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiState;
import hopsync.platform.fs.InstanceFinder;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Builds the window and its one-way data flow: core events and clicks become actions, the store reduces
 * them into a new state, the window draws it and {@link Effects} performs the side effects.
 */
public final class UiStartup {

    private UiStartup() {
    }

    /** Must run on the EDT. */
    public static void start(AppContainer container) {
        HopSyncDark.setup();
        Store store = new Store(UiState.INITIAL);
        MainWindow window = new MainWindow(store::dispatch, container.presetManifestUrl(), container.settingsFile(),
            ZoneId.systemDefault());
        store.subscribe(window::render);
        window.setVisible(true);
        new Effects(store, container.packRegistry(), container.packSessions(), container.manifestService(),
            new ModLoadService(), InstanceFinder.forCurrentUser(), container.backgroundExecutor(),
            container.settingsFile(), Clock.systemUTC(), () -> System.exit(0))
            .start(container.eventBus());
    }
}
