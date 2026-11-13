package hopsync.desktop.screens.mods;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.manifest.ModSource;
import hopsync.core.sync.SyncPlan;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Icons.Glyph;
import hopsync.desktop.store.InstalledMods;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.desktop.store.UiState.Filter;
import hopsync.desktop.store.UiState.Tab;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.INFO;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.manifest;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModRowModelTest {
    private static final String D = DEMO.id();

    /** Manifest known, jars on disk: kept.jar and upd.jar from the pack (upd.jar recorded at 0.9), mine.jar. */
    private static UiState base() {
        return reduce(loaded(), new UiAction.ManifestLoaded(D, manifest()),
            new UiAction.InstalledModsLoaded(D, new InstalledMods(List.of("kept.jar", "mine.jar", "upd.jar"),
                Map.of("mine.jar", 2048L), Set.of("kept.jar", "upd.jar", "gone.jar"), Map.of("upd.jar", "mc1.21.1-0.9"))));
    }

    private static UiState reviewing() {
        return reduce(base(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));
    }

    private static ModRowModel model(UiState state) {
        return ModRowModel.of(state.packs().get(D), state);
    }

    private static List<String> keys(ModRowModel model) {
        return model.rows().stream().map(row -> switch (row) {
            case ModRow.Mod m -> m.key();
            case ModRow.Group g -> "# " + g.title();
            case ModRow.More m -> "more";
            case ModRow.Detail d -> "detail " + d.key();
            case ModRow.Message m -> "msg " + m.text();
            case ModRow.Banner b -> "banner";
        }).toList();
    }

    private static ModRow.Mod mod(ModRowModel model, String key) {
        return model.rows().stream().filter(row -> row instanceof ModRow.Mod m && m.key().equals(key))
            .map(ModRow.Mod.class::cast).findFirst().orElseThrow(() -> new AssertionError("no row " + key));
    }

    @Test
    void beforeCheckingEveryPackModIsListedWithWhatIsOnDisk() {
        ModRowModel model = model(base());

        assertEquals(List.of("new.jar", "upd.jar", "kept.jar"), keys(model));
        assertEquals(Glyph.PENDING, mod(model, "new.jar").icon(), "not on disk yet");
        assertEquals(Glyph.CHECK, mod(model, "kept.jar").icon());
        assertNull(mod(model, "kept.jar").tag());
        assertFalse(model.showsFilter());
        assertEquals(3, model.packCount());
        assertEquals(1, model.ownCount());
    }

    @Test
    void withoutManifestTheListSaysItIsLoading() {
        ModRowModel model = model(loaded());

        assertEquals(List.of("msg " + Messages.get("mods.loading")), keys(model));
    }

    @Test
    void reviewGroupsTheChangesAndOffersTheRest() {
        ModRowModel model = model(reviewing());

        assertEquals(List.of("# " + Messages.get("mods.group.update", 1), "upd.jar",
            "# " + Messages.get("mods.group.new", 1), "new.jar",
            "# " + Messages.get("mods.group.remove", 1), "gone.jar", "more"), keys(model));
        assertTrue(model.showsFilter());
        assertEquals(new UiAction.SetFilter(Filter.ALL), ((ModRow.More) model.rows().getLast()).action());
    }

    @Test
    void updatesShowTheRecordedVersionCleaned() {
        ModRow.Mod updated = mod(model(reviewing()), "upd.jar");

        assertEquals("0.9", updated.oldVersion());
        assertEquals("1.0", updated.version());
        assertNull(mod(model(reviewing()), "new.jar").oldVersion());
    }

    @Test
    void allShowsEveryModInManifestOrderThenRemovals() {
        ModRowModel model = model(reduce(reviewing(), new UiAction.SetFilter(Filter.ALL)));

        assertEquals(List.of("new.jar", "upd.jar", "kept.jar", "gone.jar"), keys(model));
    }

    @Test
    void searchFiltersWithinTheTabAndSaysWhenNothingMatches() {
        assertEquals(List.of("kept.jar"), keys(model(reduce(base(), new UiAction.SetQuery("KEP")))));
        assertEquals(List.of("msg " + Messages.get("mods.noMatch", "zzz")),
            keys(model(reduce(base(), new UiAction.SetQuery("zzz")))));
    }

    @Test
    void applyingShowsEachFileState() {
        SyncFailure locked = new SyncFailure.FileLocked("upd.jar");
        ModRowModel model = model(reduce(reviewing(), new UiAction.ConfirmPlan(D), new SyncEvent.ApplyStarted(D),
            new SyncEvent.ModDownloading(D, "new.jar"), new SyncEvent.ModCompleted(D, "upd.jar", false, locked),
            new SyncEvent.ModRemoved(D, "gone.jar")));

        assertEquals(ModRow.Look.CURRENT, mod(model, "new.jar").look());
        assertEquals(Glyph.SPINNER, mod(model, "new.jar").icon());
        assertEquals(ModRow.Look.ERROR, mod(model, "upd.jar").look());
        assertEquals(Messages.failure(locked), mod(model, "upd.jar").author(), "the reason replaces the author");
        assertEquals(Messages.get("mods.status.removed"), mod(model, "gone.jar").tag().text());
    }

    @Test
    void pendingDownloadsSayPending() {
        ModRowModel model = model(reduce(reviewing(), new UiAction.ConfirmPlan(D), new SyncEvent.ApplyStarted(D)));

        assertEquals(Messages.get("mods.status.pending"), mod(model, "new.jar").tag().text());
    }

    @Test
    void expandedModShowsItsDetailsAndPage() {
        ModRowModel model = model(reduce(base(), new UiAction.ToggleRow("kept.jar")));

        List<String> keys = keys(model);
        assertEquals(keys.indexOf("kept.jar") + 1, keys.indexOf("detail kept.jar"));
        ModRow.Detail detail = (ModRow.Detail) model.rows().get(keys.indexOf("detail kept.jar"));
        assertEquals("kept.jar", detail.file());
        assertNull(detail.url(), "x.org is neither Modrinth nor a GitHub release");
    }

    @Test
    void ownModsAreTheJarsNeitherThePackNorHopSyncKnows() {
        ModRowModel model = model(reduce(base(), new UiAction.SelectTab(Tab.OWN_MODS)));

        assertEquals(List.of("banner", "mine.jar"), keys(model));
        assertEquals("2.0 KB", mod(model, "mine.jar").size());
        assertFalse(mod(model, "mine.jar").expandable());
    }

    @Test
    void noOwnModsSaysSo() {
        UiState state = reduce(loaded(), new UiAction.ManifestLoaded(D, manifest()), new UiAction.SelectTab(Tab.OWN_MODS));

        assertEquals(List.of("banner", "msg " + Messages.get("mods.own.empty")), keys(model(state)));
    }

    @Test
    void configsShowWhatThePlanWillDoWithThem() {
        ConfigEntry edited = new ConfigEntry("edited.toml", "https://x.org/e", "a".repeat(128), 10, false);
        ModEntry mod = manifest().mods().getFirst();
        Manifest withConfig = new Manifest(2, INFO, "c".repeat(64), List.of(mod), List.of(edited));
        SyncPlan plan = new SyncPlan(withConfig, List.of(new SyncPlan.ModAction(mod, SyncPlan.ModChange.UP_TO_DATE)),
            List.of(), List.of(new SyncPlan.ConfigAction(edited, SyncPlan.ConfigChange.KEEP_EDITED)), Map.of(), Map.of());
        UiState state = reduce(loaded(), new UiAction.ManifestLoaded(D, withConfig), new SyncEvent.UpdateStarted(D),
            new SyncEvent.PlanReady(D, plan), new UiAction.SelectTab(Tab.CONFIGS));

        ModRowModel model = model(state);
        assertInstanceOf(ModRow.Group.class, model.rows().getFirst());
        assertEquals(Messages.get("mods.config.kept"), mod(model, "edited.toml").tag().text());
        assertEquals(1, model.configCount());
    }

    @Test
    void modPagesComeFromTheDownloadUrl() {
        ModEntry modrinth = new ModEntry("id", "Mod", "1", "", "", "m.jar",
            "https://cdn.modrinth.com/data/Gi02250Z/versions/Esy04ZZ2/m.jar", "a".repeat(128), 1, ModSource.MODRINTH);
        ModEntry github = new ModEntry("id", "Mod", "1", "", "", "m.jar",
            "https://github.com/o/pack/releases/download/hopsync/abc-m.jar", "a".repeat(128), 1, ModSource.LOCAL);

        assertEquals("https://modrinth.com/project/Gi02250Z", ModLinks.pageOf(modrinth));
        assertEquals("https://github.com/o/pack/releases/tag/hopsync", ModLinks.pageOf(github));
    }
}
