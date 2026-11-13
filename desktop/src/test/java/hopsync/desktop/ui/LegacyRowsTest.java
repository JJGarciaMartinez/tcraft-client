package hopsync.desktop.ui;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyRowsTest {
    private static final String D = DEMO.id();

    private static List<LegacyRows.Row> rows(UiState state) {
        return LegacyRows.of(state.packs().get(D));
    }

    private static LegacyRows.Row row(UiState state, String file) {
        return rows(state).stream().filter(r -> r.info().file().equals(file)).findFirst().orElseThrow();
    }

    @Test
    void uncheckedPackShowsWhatIsOnDisk() {
        UiState state = reduce(loaded(), new UiAction.InstalledModsLoaded(D, List.of("a.jar", "b.jar")));

        assertEquals(List.of("a.jar", "b.jar"), rows(state).stream().map(r -> r.info().file()).toList());
        assertEquals(StatusType.IN_SYSTEM, row(state, "a.jar").type());
    }

    @Test
    void reviewShowsWhatThePlanWillDo() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(List.of("new.jar", "upd.jar", "kept.jar", "gone.jar"),
            rows(state).stream().map(r -> r.info().file()).toList());
        assertEquals(Messages.get("mod.new"), row(state, "new.jar").text());
        assertEquals(Messages.get("mod.willUpdate"), row(state, "upd.jar").text());
        assertEquals(Messages.get("mod.upToDate"), row(state, "kept.jar").text());
        assertEquals(Messages.get("mod.willRemove"), row(state, "gone.jar").text());
    }

    @Test
    void applyingShowsEachFileAsItFinishes() {
        SyncFailure locked = new SyncFailure.FileLocked("upd.jar");
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.ModDownloading(D, "new.jar"),
            new SyncEvent.ModCompleted(D, "upd.jar", false, locked), new SyncEvent.ModRemoved(D, "gone.jar"));

        assertEquals(Messages.get("mod.downloading"), row(state, "new.jar").text());
        assertEquals(LegacyRows.Tone.ERROR, row(state, "upd.jar").tone());
        assertEquals(Messages.failure(locked), row(state, "upd.jar").text());
        assertEquals(Messages.get("mod.removed"), row(state, "gone.jar").text());
    }

    @Test
    void listFollowsTheStateWhateverOrderTheLoadsArriveIn() {
        UiState installedFirst = reduce(loaded(), new UiAction.InstalledModsLoaded(D, List.of("a.jar")),
            new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));
        UiState planFirst = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new UiAction.InstalledModsLoaded(D, List.of("a.jar")));

        assertEquals(rows(installedFirst), rows(planFirst));
    }

    @Test
    void afterCancellingTheListIsWhatIsOnDiskAgain() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.UpdateCancelled(D));
        state = reduce(state, new UiAction.InstalledModsLoaded(D, List.of("a.jar")));

        assertEquals(List.of("a.jar"), rows(state).stream().map(r -> r.info().file()).toList());
    }

    @Test
    void statusLineNamesThePackWhenUpToDate() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D),
            new SyncEvent.UpToDate(D, upToDatePlan()));
        PackUiState pack = state.packs().get(D);

        assertEquals(Messages.get("status.upToDate", "Demo", "1.2.0"), LegacyRenderer.statusText(pack));
    }
}
