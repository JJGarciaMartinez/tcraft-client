package hopsync.desktop.screens;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.screens.library.PackLibrary;
import hopsync.desktop.screens.pack.PackHero;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.INFO;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds and paints every screen (no window needed) in every phase, so a broken style or a renderer that
 * throws fails the build instead of the app.
 */
class ScreensTest {
    private static final String D = DEMO.id();
    private final List<UiAction> dispatched = new ArrayList<>();

    @BeforeAll
    static void installTheme() {
        HopSyncDark.setup();
    }

    /** One state per phase of DEMO, with OTHER applying in the review one. */
    private static List<UiState> everyPhase() {
        UiState base = reduce(loaded(), new UiAction.PackInfoLoaded(D, INFO, 77),
            new UiAction.InstalledModsLoaded(D, List.of("a.jar")));
        return List.of(
            base,
            reduce(base, new SyncEvent.UpdateStarted(D)),
            reduce(base, new SyncEvent.UpdateStarted(OTHER.id()), new SyncEvent.PlanReady(OTHER.id(), plan()),
                new SyncEvent.ApplyStarted(OTHER.id()), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan())),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()), new SyncEvent.Queued(D)),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()), new SyncEvent.ApplyStarted(D),
                new SyncEvent.ModDownloading(D, "new.jar"), new SyncEvent.ProgressChanged(D, 40)),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan())),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()), new SyncEvent.ApplyStarted(D),
                new SyncEvent.UpdateCompleted(D, 1, 1)),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.UpdateCancelled(D)),
            reduce(base, new SyncEvent.UpdateStarted(D),
                new SyncEvent.UpdateFailed(D, new SyncFailure.Network("github.com", "timeout"))));
    }

    @Test
    void heroAndLibraryRenderEveryPhase() {
        PackHero hero = new PackHero(dispatched::add, ZoneOffset.UTC);
        PackLibrary library = new PackLibrary(dispatched::add);
        StatusBar statusBar = new StatusBar(ZoneOffset.UTC);
        for (UiState state : everyPhase()) {
            PackUiState pack = state.selected().orElseThrow();
            hero.render(pack, state);
            library.render(state);
            statusBar.render(pack);
            paint(hero, 746, 134);
            paint(library, 214, 560);
            paint(statusBar, 960, 28);
        }
        assertTrue(dispatched.isEmpty(), "rendering never dispatches: " + dispatched);
    }

    @Test
    void emptyStateRenders() {
        new StatusBar(ZoneOffset.UTC).render(null);
        paint(new Onboarding(dispatched::add), 960, 560);
        PackLibrary library = new PackLibrary(dispatched::add);
        library.render(UiState.INITIAL);
        paint(library, 214, 560);
    }

    @Test
    void toastShowsEachNoticeOnce() {
        JLayeredPane layer = new JLayeredPane();
        layer.setSize(960, 620);
        Toast toast = new Toast(layer);
        UiState state = reduce(loaded(), new UiAction.ShowToast("IP copiada"));

        toast.render(state.toast());
        assertTrue(toast.isVisible());
        assertEquals("IP copiada", toast.getText());
        toast.setVisible(false);
        toast.render(state.toast());
        assertTrue(!toast.isVisible(), "the same notice is not shown again");
    }

    private static void paint(JComponent component, int width, int height) {
        component.setSize(width, height);
        component.doLayout();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        component.paint(g);
        g.dispose();
    }
}
