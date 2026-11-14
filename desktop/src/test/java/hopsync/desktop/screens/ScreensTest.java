package hopsync.desktop.screens;

import com.formdev.flatlaf.util.UIScale;
import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.screens.library.PackLibrary;
import hopsync.desktop.screens.mods.ModsPanel;
import hopsync.desktop.screens.pack.PackHero;
import hopsync.desktop.screens.sheets.SheetLayer;
import hopsync.desktop.store.GameFolder;
import hopsync.desktop.store.Preferences;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.InstalledMods;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.manifest;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        UiState base = reduce(loaded(), new UiAction.ManifestLoaded(D, manifest()),
            new UiAction.InstalledModsLoaded(D, InstalledMods.of(List.of("a.jar", "kept.jar"))));
        return List.of(
            base,
            reduce(base, new SyncEvent.UpdateStarted(D)),
            reduce(base, new SyncEvent.UpdateStarted(OTHER.id()), new SyncEvent.PlanReady(OTHER.id(), plan()),
                new SyncEvent.ApplyStarted(OTHER.id()), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan())),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()), new SyncEvent.Queued(D)),
            reduce(base, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()), new SyncEvent.ApplyStarted(D),
                new SyncEvent.ModDownloading(D, "new.jar"), new SyncEvent.Progress(D, 1, 3, 40, 100, 0)),
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
        ModsPanel mods = new ModsPanel(dispatched::add);
        for (UiState phase : everyPhase()) {
            for (UiState state : List.of(phase, reduce(phase, new UiAction.SetFilter(UiState.Filter.ALL),
                    new UiAction.ToggleRow("kept.jar")), reduce(phase, new UiAction.SelectTab(UiState.Tab.OWN_MODS)),
                    reduce(phase, new UiAction.SelectTab(UiState.Tab.CONFIGS)))) {
                PackUiState pack = state.selected().orElseThrow();
                hero.render(pack, state);
                library.render(state);
                statusBar.render(pack);
                mods.render(pack, state);
                paint(hero, 746, 134);
                paint(library, 214, 560);
                paint(statusBar, 960, 28);
                paint(mods, 746, 420);
            }
        }
        assertTrue(dispatched.isEmpty(), "rendering never dispatches: " + dispatched);
    }

    @Test
    void progressBarKeepsItsWidthWhateverFileIsDownloading() throws Exception {
        UiState applying = reduce(loaded(), new UiAction.ManifestLoaded(D, manifest()), new SyncEvent.UpdateStarted(D),
            new SyncEvent.PlanReady(D, plan()), new SyncEvent.ApplyStarted(D), new SyncEvent.Progress(D, 1, 3, 40, 100, 0));
        PackHero hero = new PackHero(dispatched::add, ZoneOffset.UTC);

        int shortName = barWidth(hero, reduce(applying, new SyncEvent.ModDownloading(D, "a.jar")));
        int longName = barWidth(hero, reduce(applying, new SyncEvent.ModDownloading(D,
            "create-connected-and-all-its-addons-mc1.21.1-neoforge-1.1.10-super-long-file-name.jar")));

        assertTrue(shortName > 0);
        assertEquals(shortName, longName);
    }

    /**
     * Lays the hero out as a shown window would. Without a window Swing never marks components valid, so
     * {@code revalidate()} stops early and layouts keep their cached sizes; invalidating the tree by hand
     * clears them, as the window does.
     */
    private static int barWidth(PackHero hero, UiState state) throws Exception {
        int[] width = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            hero.render(state.selected().orElseThrow(), state);
            // The width the hero has in a 960 px window
            hero.setSize(UIScale.scale(746), UIScale.scale(134));
            invalidateTree(hero);
            layoutTree(hero);
            width[0] = find(hero, JProgressBar.class).getWidth();
        });
        return width[0];
    }

    private static void invalidateTree(Container container) {
        container.invalidate();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) invalidateTree(inner);
        }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) layoutTree(inner);
        }
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container inner) {
                T found = find(inner, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    void emptyStateRenders() {
        new StatusBar(ZoneOffset.UTC).render(null);
        PackLibrary library = new PackLibrary(dispatched::add);
        library.render(UiState.INITIAL);
        paint(library, 214, 560);
    }

    /** Every sheet in every state it can be in, as the store would leave it. */
    private static List<UiState> everySheet() {
        String url = "https://example.org/new.json";
        UiState empty = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(), null));
        UiState add = reduce(loaded(), new UiAction.AddPack(), new UiAction.SetLinkUrl(url));
        UiState valid = reduce(add, new UiAction.CheckLink(), new UiAction.LinkChecked(url, manifest()));
        GameFolder taken = new GameFolder(GameFolder.Kind.PRISM, "Demo", DEMO.gameDir(), 40, "Demo");
        GameFolder free = new GameFolder(GameFolder.Kind.OFFICIAL, null, Path.of("/games/.minecraft").toAbsolutePath(),
            0, null);
        UiState folders = reduce(valid, new UiAction.WizardNext());
        UiState packSettings = reduce(loaded(), new UiAction.OpenSettings());
        return List.of(
            empty,
            reduce(empty, new UiAction.SetLinkUrl("http://example.org"), new UiAction.CheckLink()),
            add,
            reduce(add, new UiAction.CheckLink()),
            reduce(add, new UiAction.CheckLink(), new UiAction.LinkFailed(url, new SyncFailure.HttpStatus(404, url))),
            reduce(add, new UiAction.SetLinkUrl(DEMO.manifestUrl()), new UiAction.CheckLink(),
                new UiAction.LinkChecked(DEMO.manifestUrl(), manifest())),
            valid,
            folders,
            reduce(folders, new UiAction.FoldersFound(List.of())),
            reduce(folders, new UiAction.FoldersFound(List.of(taken, free))),
            reduce(folders, new UiAction.FoldersFound(List.of(taken)), new UiAction.ChooseFolder(taken.path())),
            reduce(folders, new UiAction.FoldersFound(List.of(free)), new UiAction.SheetFailed(
                new SyncFailure.PermissionDenied("/games/.minecraft"))),
            packSettings,
            reduce(packSettings, new UiAction.SetLinkUrl(url), new UiAction.CheckLink(),
                new UiAction.LinkChecked(url, manifest())),
            reduce(packSettings, new UiAction.SelectPackSection(Sheet.PackSettings.Section.FOLDER),
                new UiAction.ChooseFolder(OTHER.gameDir()),
                new UiAction.FolderChecked(new GameFolder(GameFolder.Kind.OTHER, null, OTHER.gameDir(), 2, "Other"))),
            reduce(packSettings, new UiAction.SelectPackSection(Sheet.PackSettings.Section.REMOVE)),
            reduce(loaded(), new UiAction.OpenAppSettings(Sheet.AppSettings.Section.GENERAL),
                new UiAction.SetPreferences(new Preferences(false, true, 6))),
            reduce(loaded(), new UiAction.OpenAppSettings(Sheet.AppSettings.Section.ABOUT)));
    }

    @Test
    void sheetsRenderEveryState() {
        JLayeredPane window = new JLayeredPane();
        window.setSize(960, 620);
        SheetLayer sheets = new SheetLayer(window, dispatched::add, "https://example.org/demo.json",
            Path.of("settings.json"));
        JComponent firstRun = sheets.firstRunWizard();
        for (UiState state : everySheet()) {
            sheets.render(state);
            paint(sheets, 960, 620);
            paint(firstRun, 960, 620);
        }
        sheets.render(UiState.INITIAL);
        assertFalse(sheets.isVisible());
        assertTrue(dispatched.isEmpty(), "rendering never dispatches: " + dispatched);
    }

    @Test
    void typingALinkDispatchesItButRenderingItBackDoesNot() {
        JLayeredPane window = new JLayeredPane();
        window.setSize(960, 620);
        SheetLayer sheets = new SheetLayer(window, dispatched::add, "", Path.of("settings.json"));
        UiState add = reduce(loaded(), new UiAction.AddPack());
        sheets.render(add);
        JTextField field = find(sheets, JTextField.class);

        field.setText("https://example.org/a.json");
        assertEquals(new UiAction.SetLinkUrl("https://example.org/a.json"), dispatched.getLast());
        dispatched.clear();
        sheets.render(reduce(add, new UiAction.SetLinkUrl("https://example.org/b.json")));
        assertEquals("https://example.org/b.json", field.getText());
        assertTrue(dispatched.isEmpty(), "rendering never dispatches: " + dispatched);
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
