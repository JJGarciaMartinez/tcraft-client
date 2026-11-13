package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.pack.Pack;
import hopsync.desktop.store.UiState.Sheet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.INFO;
import static hopsync.desktop.store.StoreFixtures.NOW;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void noPacksOpensTheFirstRunSettings() {
        UiState state = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(), null));

        assertNull(state.selectedPackId());
        assertEquals(Sheet.SETTINGS_FIRST_RUN, state.sheet());
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
    void installedModsAndPackInfoArriveForTheirPack() {
        UiState state = reduce(loaded(), new UiAction.InstalledModsLoaded(O, List.of("a.jar")),
            new UiAction.PackInfoLoaded(D, INFO));

        assertEquals(List.of("a.jar"), other(state).installed());
        assertEquals(List.of(), demo(state).installed());
        assertEquals(INFO, demo(state).packInfo());
    }

    @Test
    void packInfoFailureReplacesTheInfo() {
        SyncFailure offline = new SyncFailure.Network("example.org", "timeout");
        UiState state = reduce(loaded(), new UiAction.PackInfoLoaded(D, INFO), new UiAction.PackInfoFailed(D, offline));

        assertNull(demo(state).packInfo());
        assertEquals(offline, demo(state).packInfoError());
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
    void planReadyForTheSelectedPackOpensTheReview() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(PackPhase.REVIEW, demo(state).phase());
        assertEquals(plan(), demo(state).plan());
        assertEquals(INFO, demo(state).packInfo());
        assertEquals(NOW, demo(state).lastChecked());
        assertEquals(Sheet.REVIEW, state.sheet());
    }

    @Test
    void planReadyForAnotherPackDoesNotOpenTheReview() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()));

        assertEquals(PackPhase.REVIEW, other(state).phase());
        assertEquals(PackPhase.UNCHECKED, demo(state).phase());
        assertEquals(Sheet.NONE, state.sheet());
    }

    @Test
    void upToDateRecordsThePlanWithoutReview() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan()));

        assertEquals(PackPhase.UP_TO_DATE, demo(state).phase());
        assertEquals(NOW, demo(state).lastChecked());
        assertEquals(Sheet.NONE, state.sheet());
    }

    @Test
    void confirmingClosesTheReview() {
        UiState review = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        UiState state = reduce(review, new UiAction.ConfirmPlan(D));

        assertEquals(Sheet.NONE, state.sheet());
        assertEquals(PackPhase.REVIEW, demo(state).phase(), "the phase follows the core, not the click");
    }

    @Test
    void dismissingClosesTheReview() {
        UiState review = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(Sheet.NONE, reduce(review, new UiAction.DismissPlan(D)).sheet());
    }

    @Test
    void cancelledWhileReviewingClosesTheReview() {
        UiState review = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        UiState state = reduce(review, new SyncEvent.UpdateCancelled(D));

        assertEquals(PackPhase.CANCELLED, demo(state).phase());
        assertEquals(Sheet.NONE, state.sheet());
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
        UiState state = reduce(applying(), new SyncEvent.ModDownloading(D, "new.jar"), new SyncEvent.ProgressChanged(D, 40));

        assertEquals(40, demo(state).progress().percent());
        assertEquals("new.jar", demo(state).progress().currentFile());
        assertEquals(new RowStatus.Downloading(), demo(state).rowStatus().get("new.jar"));
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
    void settingsOpenUnlessThePackIsBusy() {
        assertEquals(Sheet.SETTINGS, reduce(loaded(), new UiAction.OpenSettings()).sheet());
        assertEquals(Sheet.NONE, reduce(applying(), new UiAction.OpenSettings()).sheet());
    }

    @Test
    void closingSettingsClosesTheSheet() {
        UiState open = reduce(loaded(), new UiAction.OpenSettings());

        assertEquals(Sheet.NONE, reduce(open, new UiAction.SettingsClosed(false)).sheet());
    }

    @Test
    void cancellingTheFirstRunDoesNotReopenIt() {
        UiState firstRun = reduce(UiState.INITIAL, new UiAction.PacksLoaded(List.of(), null));

        UiState state = reduce(firstRun, new UiAction.SettingsClosed(false));

        assertEquals(Sheet.NONE, state.sheet());
        assertEquals(Sheet.SETTINGS_FIRST_RUN, reduce(state, new UiAction.OpenSettings()).sheet());
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
    }
}
