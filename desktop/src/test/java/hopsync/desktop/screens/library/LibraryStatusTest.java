package hopsync.desktop.screens.library;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.store.UiState;
import org.junit.jupiter.api.Test;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryStatusTest {
    private static final String D = DEMO.id();

    private static LibraryStatus status(UiState state) {
        return LibraryStatus.of(state.packs().get(D));
    }

    @Test
    void everyPhaseHasAMarkAndAText() {
        assertEquals(new LibraryStatus(StatusMark.Kind.IDLE, 0, Messages.get("library.status.unchecked")), status(loaded()));
        assertEquals(StatusMark.Kind.SPINNER, status(reduce(loaded(), new SyncEvent.UpdateStarted(D))).mark());
        assertEquals(new LibraryStatus(StatusMark.Kind.WARN, 0, Messages.get("library.status.review", 3)),
            status(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()))));
        assertEquals(StatusMark.Kind.OK,
            status(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan()))).mark());
        assertEquals(Messages.get("library.status.pending"),
            status(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpdateCancelled(D))).text());
    }

    @Test
    void applyingShowsThePercentInTheRing() {
        LibraryStatus status = status(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.Progress(D, 1, 3, 63, 100, 0)));

        assertEquals(new LibraryStatus(StatusMark.Kind.PROGRESS, 63, Messages.get("library.status.applying", 63)), status);
    }

    @Test
    void noConnectionIsNamedOtherErrorsAreGeneric() {
        assertEquals(Messages.get("library.status.offline"), status(reduce(loaded(), new SyncEvent.UpdateStarted(D),
            new SyncEvent.UpdateFailed(D, new SyncFailure.Network("github.com", "timeout")))).text());
        assertEquals(Messages.get("library.status.error"), status(reduce(loaded(), new SyncEvent.UpdateStarted(D),
            new SyncEvent.UpdateFailed(D, new SyncFailure.DiskFull("x")))).text());
    }
}
