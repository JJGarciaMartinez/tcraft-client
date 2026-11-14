package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.pack.Pack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.INFO;
import static hopsync.desktop.store.StoreFixtures.NOW;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.manifest;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReducerTest {
    private static final String D = DEMO.id();
    private static final String O = OTHER.id();

    private static PackUiState demo(UiState state) {
        return state.packs().get(D);
    }

    private static PackUiState other(UiState state) {
        return state.packs().get(O);
    }

    /** DEMO checked, reviewed and confirmed, now applying. */
    private static UiState applying() {
        return reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new UiAction.ConfirmPlan(D), new SyncEvent.ApplyStarted(D));
    }

    // Packs

    @Test
    void packsLoadInOrderUncheckedWithTheActiveOneSelected() {
        UiState state = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(DEMO, OTHER), O));

        assertEquals(List.of(D, O), List.copyOf(state.packs().keySet()));
        assertEquals(O, state.selectedPackId());
        assertEquals(PackPhase.UNCHECKED, demo(state).phase());
        assertEquals(DEMO.gameDir(), demo(state).gameDir());
    }

    @Test
    void noPacksOpensTheFirstRunWizard() {
        UiState state = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(), null));

        assertNull(state.selectedPackId());
        assertEquals(Sheet.AddPack.start(true), state.sheet());
    }

    @Test
    void preferencesArriveWithThePacks() {
        Preferences preferences = new Preferences(false, false, 2);

        UiState state = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(DEMO), D, preferences));

        assertEquals(preferences, state.preferences());
    }

    @Test
    void reloadingKeepsWhatWasCheckedForUnchangedPacks() {
        UiState checked = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan()));

        UiState state = reduce(checked, new UiAction.PacksLoaded(List.of(DEMO.withName("Renamed"), OTHER), D));

        assertEquals(PackPhase.UP_TO_DATE, demo(state).phase());
        assertEquals("Renamed", demo(state).name());
    }

    @Test
    void anotherLinkOrFolderStartsThePackOver() {
        UiState checked = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan()));
        Pack moved = DEMO.withGameDir(DEMO.gameDir().resolveSibling("moved"));

        UiState state = reduce(checked, new UiAction.PacksLoaded(List.of(moved, OTHER), D));

        assertEquals(PackPhase.UNCHECKED, demo(state).phase());
        assertNull(demo(state).plan());
        assertEquals(moved.gameDir(), demo(state).gameDir());
    }

    @Test
    void removedPackIsDroppedAndAnotherSelected() {
        UiState state = reduce(loaded(), new UiAction.PacksLoaded(List.of(OTHER), null));

        assertEquals(List.of(O), List.copyOf(state.packs().keySet()));
        assertEquals(O, state.selectedPackId());
    }

    @Test
    void installedModsAndTheManifestArriveForTheirPack() {
        UiState state = reduce(loaded(), new UiAction.InstalledModsLoaded(O, InstalledMods.of(List.of("a.jar"))),
            new UiAction.ManifestLoaded(D, StoreFixtures.manifest()));

        assertEquals(List.of("a.jar"), other(state).installed().jars());
        assertEquals(InstalledMods.NONE, demo(state).installed());
        assertEquals(INFO, demo(state).packInfo());
        assertEquals(3, demo(state).modCount());
        assertEquals("Demo", demo(state).displayName());
    }

    @Test
    void manifestFailureKeepsTheLastKnownManifest() {
        SyncFailure offline = new SyncFailure.Network("example.org", "timeout");
        UiState state = reduce(loaded(), new UiAction.ManifestLoaded(D, StoreFixtures.manifest()),
            new UiAction.ManifestFailed(D, offline));

        assertEquals(INFO, demo(state).packInfo(), "a cached manifest keeps the list on screen while offline");
        assertEquals(offline, demo(state).manifestError());
    }

    @Test
    void eachTabKeepsItsOwnSearch() {
        UiState state = reduce(loaded(), new UiAction.SetQuery("sod"), new UiAction.SelectTab(UiState.Tab.OWN_MODS),
            new UiAction.SetQuery("mine"));

        assertEquals("mine", state.query());
        assertEquals("sod", reduce(state, new UiAction.SelectTab(UiState.Tab.PACK)).query());
    }

    @Test
    void aNewPlanForTheSelectedPackShowsOnlyTheChanges() {
        UiState all = reduce(loaded(), new UiAction.SetFilter(UiState.Filter.ALL));

        UiState state = reduce(all, new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(UiState.Filter.CHANGES, state.filter());
    }

    // Check and review

    @Test
    void checkingClearsTheLastRun() {
        UiState failed = reduce(loaded(), new SyncEvent.UpdateStarted(D),
            new SyncEvent.UpdateFailed(D, new SyncFailure.Unexpected("x")));

        UiState state = reduce(failed, new SyncEvent.UpdateStarted(D));

        assertEquals(PackPhase.CHECKING, demo(state).phase());
        assertNull(demo(state).error());
        assertTrue(demo(state).rowStatus().isEmpty());
    }

    @Test
    void planReadyIsReviewedInTheHeaderWithoutADialog() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(PackPhase.REVIEW, demo(state).phase());
        assertEquals(plan(), demo(state).plan());
        assertEquals(INFO, demo(state).packInfo());
        assertEquals(3, demo(state).modCount());
        assertEquals(NOW, demo(state).lastChecked());
        assertNull(state.sheet());
    }

    @Test
    void planReadyForAnotherPackDoesNotOpenTheReview() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()));

        assertEquals(PackPhase.REVIEW, other(state).phase());
        assertEquals(PackPhase.UNCHECKED, demo(state).phase());
        assertNull(state.sheet());
    }

    @Test
    void upToDateRecordsThePlanWithoutReview() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan()));

        assertEquals(PackPhase.UP_TO_DATE, demo(state).phase());
        assertEquals(NOW, demo(state).lastChecked());
        assertNull(state.sheet());
    }

    @Test
    void confirmingOrDismissingWaitsForTheCore() {
        UiState review = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertSame(review, Reducer.reduce(review, new UiAction.ConfirmPlan(D)), "the phase follows the core, not the click");
        assertSame(review, Reducer.reduce(review, new UiAction.DismissPlan(D)));
    }

    @Test
    void cancelledWhileReviewingEndsCancelled() {
        UiState review = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(PackPhase.CANCELLED, demo(reduce(review, new SyncEvent.UpdateCancelled(D))).phase());
    }

    // Queue and apply

    @Test
    void queuedPackWaits() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()),
            new SyncEvent.Queued(O));

        assertEquals(PackPhase.QUEUED, other(state).phase());
        assertTrue(other(state).phase().isBusy());
    }

    @Test
    void cancellingAQueuedPackEndsCancelled() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()),
            new SyncEvent.Queued(O), new SyncEvent.UpdateCancelled(O));

        assertEquals(PackPhase.CANCELLED, other(state).phase());
    }

    @Test
    void applyStartsWithEveryDownloadPending() {
        PackUiState pack = demo(applying());

        assertEquals(PackPhase.APPLYING, pack.phase());
        assertEquals(0, pack.progress().percent());
        assertEquals(new RowStatus.Pending(), pack.rowStatus().get("new.jar"));
        assertEquals(new RowStatus.Pending(), pack.rowStatus().get("upd.jar"));
        assertNull(pack.rowStatus().get("kept.jar"), "up-to-date files are not downloaded");
    }

    @Test
    void progressAndCurrentFileAreTracked() {
        UiState state = reduce(applying(), new SyncEvent.ModDownloading(D, "new.jar"), new SyncEvent.Progress(D, 1, 3, 40, 100, 0));

        assertEquals(40, demo(state).progress().percent());
        assertEquals("new.jar", demo(state).progress().currentFile());
        assertEquals(new RowStatus.Downloading(0), demo(state).rowStatus().get("new.jar"));
        assertEquals(3, demo(state).progress().totalFiles());
    }

    @Test
    void eachFileShowsItsOwnProgressUntilItFinishes() {
        UiState state = reduce(applying(), new SyncEvent.ModDownloading(D, "new.jar"),
            new SyncEvent.ModProgress(D, "new.jar", 35));
        assertEquals(new RowStatus.Downloading(35), demo(state).rowStatus().get("new.jar"));

        state = reduce(state, new SyncEvent.ModCompleted(D, "new.jar", true, null), new SyncEvent.ModProgress(D, "new.jar", 100));
        assertEquals(new RowStatus.Done(), demo(state).rowStatus().get("new.jar"), "a late step does not undo the finish");
    }

    @Test
    void cancellingKeepsTheCountsOfWhatWasApplied() {
        UiState state = reduce(applying(), new SyncEvent.Progress(D, 2, 6, 10, 100, 0), new SyncEvent.UpdateCancelled(D));

        assertEquals(2, demo(state).progress().doneFiles());
        assertEquals(6, demo(state).progress().totalFiles());
    }

    @Test
    void finishedFilesKeepTheirFailureReason() {
        SyncFailure locked = new SyncFailure.FileLocked("upd.jar");
        UiState state = reduce(applying(), new SyncEvent.ModCompleted(D, "new.jar", true, null),
            new SyncEvent.ModCompleted(D, "upd.jar", false, locked), new SyncEvent.ModRemoved(D, "gone.jar"));

        assertEquals(new RowStatus.Done(), demo(state).rowStatus().get("new.jar"));
        assertEquals(new RowStatus.Failed(locked), demo(state).rowStatus().get("upd.jar"));
        assertEquals(new RowStatus.Removed(), demo(state).rowStatus().get("gone.jar"));
    }

    @Test
    void completedWithoutFailuresIsDone() {
        UiState state = reduce(applying(), new SyncEvent.UpdateCompleted(D, 2, 0));

        assertEquals(PackPhase.DONE, demo(state).phase());
        assertEquals(new PackUiState.Result(2, 0), demo(state).result());
        assertNull(demo(state).progress().currentFile());
    }

    @Test
    void completedWithFailuresIsDoneWithErrors() {
        UiState state = reduce(applying(), new SyncEvent.UpdateCompleted(D, 1, 1));

        assertEquals(PackPhase.DONE_WITH_ERRORS, demo(state).phase());
        assertEquals(1, demo(state).result().failed());
    }

    @Test
    void cancelledWhileApplyingKeepsTheFinishedRows() {
        UiState state = reduce(applying(), new SyncEvent.ModCompleted(D, "new.jar", true, null),
            new SyncEvent.UpdateCancelled(D));

        assertEquals(PackPhase.CANCELLED, demo(state).phase());
        assertEquals(new RowStatus.Done(), demo(state).rowStatus().get("new.jar"));
    }

    @Test
    void failureKeepsItsReason() {
        SyncFailure full = new SyncFailure.DiskFull("new.jar");
        UiState state = reduce(applying(), new SyncEvent.UpdateFailed(D, full));

        assertEquals(PackPhase.ERROR, demo(state).phase());
        assertEquals(full, demo(state).error());
        assertTrue(!demo(state).phase().isBusy());
    }

    @Test
    void eventsOfOnePackNeverTouchAnother() {
        UiState state = reduce(applying(), new SyncEvent.UpdateStarted(O), new SyncEvent.UpdateFailed(O,
            new SyncFailure.Unexpected("x")));

        assertEquals(PackPhase.APPLYING, demo(state).phase());
        assertEquals(PackPhase.ERROR, other(state).phase());
    }

    @Test
    void eventsOfARemovedPackAreIgnored() {
        UiState state = loaded();

        assertSame(state, Reducer.reduce(state, StoreFixtures.event(new SyncEvent.UpdateStarted("gone"))));
    }

    @Test
    void logMessagesChangeNothing() {
        UiState state = applying();

        assertEquals(state, reduce(state, new SyncEvent.LogMessage(D, "hello")));
    }

    // Settings and navigation

    @Test
    void packSettingsOpenOnTheLinkUnlessThePackIsBusy() {
        Sheet.PackSettings settings = assertInstanceOf(Sheet.PackSettings.class,
            reduce(loaded(), new UiAction.OpenSettings()).sheet());

        assertEquals(D, settings.packId());
        assertEquals(Sheet.PackSettings.Section.LINK, settings.section());
        assertEquals(DEMO.manifestUrl(), settings.url());
        assertEquals(DEMO.gameDir(), settings.folder().path());
        assertNull(reduce(applying(), new UiAction.OpenSettings()).sheet());
    }

    @Test
    void aPackNotYetInstalledCanStillBeEditedOrRemoved() {
        UiState checking = reduce(loaded(), new SyncEvent.UpdateStarted(D));
        UiState review = reduce(checking, new SyncEvent.PlanReady(D, plan()));
        UiState queued = reduce(review, new SyncEvent.Queued(D));

        for (UiState state : List.of(checking, review, queued)) {
            assertInstanceOf(Sheet.PackSettings.class, reduce(state, new UiAction.OpenSettings()).sheet());
            assertEquals(Sheet.PackSettings.Section.REMOVE, assertInstanceOf(Sheet.PackSettings.class,
                reduce(state, new UiAction.AskRemovePack()).sheet()).section());
        }
    }

    @Test
    void closingASheetClosesIt() {
        UiState open = reduce(loaded(), new UiAction.OpenSettings());

        assertNull(reduce(open, new UiAction.CloseSheet()).sheet());
        assertNull(reduce(open, new UiAction.SheetSaved()).sheet());
    }

    @Test
    void theFirstRunWizardCannotBeClosed() {
        UiState firstRun = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(), null));

        assertEquals(firstRun, reduce(firstRun, new UiAction.CloseSheet()));
    }

    @Test
    void addingAPackIsAlwaysPossible() {
        assertEquals(Sheet.AddPack.start(false), reduce(applying(), new UiAction.AddPack()).sheet());
    }

    @Test
    void removingOpensThePackSettingsAndNotWhileThePackIsBusy() {
        Sheet.PackSettings settings = assertInstanceOf(Sheet.PackSettings.class,
            reduce(loaded(), new UiAction.AskRemovePack()).sheet());

        assertEquals(Sheet.PackSettings.Section.REMOVE, settings.section());
        assertNull(reduce(applying(), new UiAction.AskRemovePack()).sheet());
        assertNull(reduce(UiState.INITIAL, new UiAction.AskRemovePack()).sheet());
    }

    @Test
    void appSettingsStartFromTheSavedPreferences() {
        UiState state = reduce(loaded(), new UiAction.OpenAppSettings(Sheet.AppSettings.Section.GENERAL),
            new UiAction.SetPreferences(new Preferences(false, true, 12)));

        Sheet.AppSettings settings = assertInstanceOf(Sheet.AppSettings.class, state.sheet());
        assertEquals(new Preferences(false, true, 8), settings.draft());
        assertEquals(Preferences.DEFAULT, state.preferences());
    }

    // Add-pack wizard

    private static Sheet.AddPack wizard(UiState state) {
        return assertInstanceOf(Sheet.AddPack.class, state.sheet());
    }

    private static UiState linkChecked(String url) {
        return reduce(loaded(), new UiAction.AddPack(), new UiAction.SetLinkUrl(url), new UiAction.CheckLink(),
            new UiAction.LinkChecked(url, manifest()));
    }

    @Test
    void linksAreCheckedWithoutTheNetworkFirst() {
        UiState open = reduce(loaded(), new UiAction.AddPack());

        assertEquals(LinkCheck.Problem.EMPTY, problem(reduce(open, new UiAction.CheckLink())));
        assertEquals(LinkCheck.Problem.NOT_HTTPS, problem(reduce(open,
            new UiAction.SetLinkUrl("http://example.org/pack.json"), new UiAction.CheckLink())));
        assertEquals(new LinkCheck.Checking("https://example.org/new.json"), wizard(reduce(open,
            new UiAction.SetLinkUrl("https://example.org/new.json "), new UiAction.CheckLink())).link());
    }

    private static LinkCheck.Problem problem(UiState state) {
        return assertInstanceOf(LinkCheck.Invalid.class, wizard(state).link()).problem();
    }

    @Test
    void aValidLinkShowsThePackAndUnlocksTheNextStep() {
        UiState state = linkChecked("https://example.org/new.json");

        LinkCheck.Valid valid = assertInstanceOf(LinkCheck.Valid.class, wizard(state).link());
        assertEquals(INFO, valid.info());
        assertEquals(manifest().mods().size(), valid.mods());
        assertEquals(2, wizard(reduce(state, new UiAction.WizardNext())).step());
    }

    @Test
    void aPackAlreadyAddedCanBeAddedAgainForAnotherFolder() {
        UiState state = linkChecked(DEMO.manifestUrl());

        LinkCheck.Valid valid = assertInstanceOf(LinkCheck.Valid.class, wizard(state).link());
        assertEquals(List.of(DEMO.gameDir()), valid.copies());
        assertEquals(2, wizard(reduce(state, new UiAction.WizardNext())).step());
        assertEquals(List.of(), assertInstanceOf(LinkCheck.Valid.class,
            wizard(linkChecked("https://example.org/new.json")).link()).copies());
    }

    @Test
    void aPackDoesNotCountAsACopyOfItself() {
        UiState state = reduce(loaded(), new UiAction.OpenSettings(), new UiAction.CheckLink(),
            new UiAction.LinkChecked(DEMO.manifestUrl(), manifest()));

        Sheet.PackSettings settings = assertInstanceOf(Sheet.PackSettings.class, state.sheet());
        assertEquals(List.of(), assertInstanceOf(LinkCheck.Valid.class, settings.link()).copies());
    }

    @Test
    void theNextStepNeedsAValidLink() {
        UiState checking = reduce(loaded(), new UiAction.AddPack(), new UiAction.SetLinkUrl("https://example.org/n.json"),
            new UiAction.CheckLink());

        assertEquals(checking, reduce(checking, new UiAction.WizardNext()));
    }

    @Test
    void anAnswerForALinkNoLongerTypedIsIgnored() {
        UiState retyped = reduce(loaded(), new UiAction.AddPack(), new UiAction.SetLinkUrl("https://example.org/a.json"),
            new UiAction.CheckLink(), new UiAction.SetLinkUrl("https://example.org/b.json"));

        UiState state = reduce(retyped, new UiAction.LinkChecked("https://example.org/a.json", manifest()));

        assertEquals(new LinkCheck.Unchecked(), wizard(state).link());
    }

    @Test
    void anUnreadableLinkKeepsTheReason() {
        SyncFailure failure = new SyncFailure.HttpStatus(404, "https://example.org/a.json");
        UiState state = reduce(loaded(), new UiAction.AddPack(), new UiAction.SetLinkUrl("https://example.org/a.json"),
            new UiAction.CheckLink(), new UiAction.LinkFailed("https://example.org/a.json", failure));

        LinkCheck.Invalid invalid = assertInstanceOf(LinkCheck.Invalid.class, wizard(state).link());
        assertEquals(LinkCheck.Problem.UNREADABLE, invalid.problem());
        assertEquals(failure, invalid.failure());
    }

    private static final GameFolder TAKEN = new GameFolder(GameFolder.Kind.PRISM, "Demo", DEMO.gameDir(), 40, "Demo");
    private static final GameFolder FREE = new GameFolder(GameFolder.Kind.OFFICIAL, null,
        Path.of("/games/.minecraft").toAbsolutePath(), 0, null);

    @Test
    void theFirstFreeFolderFoundIsPicked() {
        UiState state = reduce(linkChecked("https://example.org/new.json"), new UiAction.WizardNext(),
            new UiAction.FoldersFound(List.of(TAKEN, FREE)));

        assertEquals(List.of(TAKEN, FREE), wizard(state).folders());
        assertEquals(FREE, wizard(state).chosen());
    }

    @Test
    void aFolderUsedByAnotherPackCanBePickedButIsNotFree() {
        UiState state = reduce(linkChecked("https://example.org/new.json"), new UiAction.WizardNext(),
            new UiAction.FoldersFound(List.of(TAKEN, FREE)), new UiAction.ChooseFolder(TAKEN.path()));

        assertEquals(TAKEN, wizard(state).chosen());
        assertFalse(wizard(state).chosen().isFree());
    }

    @Test
    void aFolderPickedByHandIsCheckedAndKeepsItsKind() {
        Path picked = Path.of("/games/new").toAbsolutePath();
        UiState state = reduce(linkChecked("https://example.org/new.json"), new UiAction.WizardNext(),
            new UiAction.FoldersFound(List.of(FREE)), new UiAction.ChooseFolder(picked),
            new UiAction.FolderChecked(new GameFolder(GameFolder.Kind.OTHER, null, picked, 3, "Other")));

        assertEquals(new GameFolder(GameFolder.Kind.OTHER, null, picked, 3, "Other"), wizard(state).chosen());

        UiState official = reduce(state, new UiAction.ChooseFolder(FREE.path()),
            new UiAction.FolderChecked(new GameFolder(GameFolder.Kind.OTHER, null, FREE.path(), 5, null)));
        assertEquals(new GameFolder(GameFolder.Kind.OFFICIAL, null, FREE.path(), 5, null), wizard(official).chosen());
    }

    @Test
    void goingBackKeepsTheLinkAndForgetsTheFolders() {
        UiState state = reduce(linkChecked("https://example.org/new.json"), new UiAction.WizardNext(),
            new UiAction.FoldersFound(List.of(FREE)), new UiAction.WizardBack());

        assertEquals(1, wizard(state).step());
        assertInstanceOf(LinkCheck.Valid.class, wizard(state).link());
        assertEquals(2, wizard(reduce(state, new UiAction.WizardNext())).step());
        assertNull(wizard(reduce(state, new UiAction.WizardNext())).folders());
    }

    @Test
    void aFailedSaveIsShownInTheSheet() {
        SyncFailure failure = new SyncFailure.FolderUsedByPack("/games/demo", "Demo");
        UiState state = reduce(linkChecked("https://example.org/new.json"), new UiAction.SheetFailed(failure));

        assertEquals(failure, wizard(state).error());
    }

    @Test
    void packSettingsAcceptTheirOwnLink() {
        UiState state = reduce(loaded(), new UiAction.OpenSettings(), new UiAction.CheckLink());

        Sheet.PackSettings settings = assertInstanceOf(Sheet.PackSettings.class, state.sheet());
        assertEquals(new LinkCheck.Checking(DEMO.manifestUrl()), settings.link());
    }

    @Test
    void everyToastIsNewEvenWithTheSameText() {
        UiState once = reduce(loaded(), new UiAction.ShowToast("IP copiada"));
        UiState twice = reduce(once, new UiAction.ShowToast("IP copiada"));

        assertEquals("IP copiada", twice.toast().text());
        assertTrue(twice.toast().id() > once.toast().id());
    }

    @Test
    void theApplyingPackIsKnownToTheOthers() {
        UiState state = applying();

        assertTrue(state.anotherPackApplying(O));
        assertTrue(!state.anotherPackApplying(D));
        assertEquals(D, state.applyingPack().orElseThrow().packId());
    }

    @Test
    void selectingAPackResetsTheExpandedRow() {
        UiState expanded = reduce(loaded(), new UiAction.ToggleRow("new.jar"));

        UiState state = reduce(expanded, new UiAction.SelectPack(O));

        assertEquals(O, state.selectedPackId());
        assertNull(state.expandedRow());
        assertEquals(D, reduce(state, new UiAction.SelectPack("unknown"), new UiAction.SelectPack(D)).selectedPackId());
    }

    @Test
    void togglingARowExpandsAndCollapsesIt() {
        UiState once = reduce(loaded(), new UiAction.ToggleRow("new.jar"));
        assertEquals("new.jar", once.expandedRow());
        assertNull(reduce(once, new UiAction.ToggleRow("new.jar")).expandedRow());
        assertEquals("upd.jar", reduce(once, new UiAction.ToggleRow("upd.jar")).expandedRow());
    }

    @Test
    void tabFilterAndQueryAreKept() {
        UiState state = reduce(loaded(), new UiAction.SelectTab(UiState.Tab.CONFIGS),
            new UiAction.SetFilter(UiState.Filter.CHANGES), new UiAction.SetQuery(null), new UiAction.SetQuery("sod"));

        assertEquals(UiState.Tab.CONFIGS, state.tab());
        assertEquals(UiState.Filter.CHANGES, state.filter());
        assertEquals("sod", state.query());
    }

    @Test
    void commandsWithOnlySideEffectsLeaveTheStateAlone() {
        UiState state = loaded();

        assertSame(state, Reducer.reduce(state, new UiAction.CheckPack(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.CancelUpdate(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.ReloadInstalled(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.ContinueUpdate(D, Set.of("a.jar"))));
        assertSame(state, Reducer.reduce(state, new UiAction.Quit()));
        assertSame(state, Reducer.reduce(state, new UiAction.CheckAll()));
        assertSame(state, Reducer.reduce(state, new UiAction.OpenFolder(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.CopyServerIp(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.CopyPackLink(D)));
        assertSame(state, Reducer.reduce(state, new UiAction.OpenModPage("https://modrinth.com/project/x")));
        assertSame(state, Reducer.reduce(state, new UiAction.RemovePack(D)));
    }
}
