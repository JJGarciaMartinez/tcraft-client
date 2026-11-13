package hopsync.desktop.screens.library;

import hopsync.core.failure.SyncFailure;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.screens.pack.PlanSummary;
import hopsync.desktop.store.PackUiState;

/**
 * The one-line status of a pack in the library. Pure, so each phase is tested without a window.
 *
 * @param percent download progress for {@link StatusMark.Kind#PROGRESS}, else 0
 */
public record LibraryStatus(StatusMark.Kind mark, int percent, String text) {

    public static LibraryStatus of(PackUiState pack) {
        return switch (pack.phase()) {
            case UNCHECKED -> new LibraryStatus(StatusMark.Kind.IDLE, 0, Messages.get("library.status.unchecked"));
            case CHECKING -> new LibraryStatus(StatusMark.Kind.SPINNER, 0, Messages.get("library.status.checking"));
            case REVIEW -> new LibraryStatus(StatusMark.Kind.WARN, 0,
                Messages.get("library.status.review", PlanSummary.of(pack.plan()).changes()));
            case QUEUED -> new LibraryStatus(StatusMark.Kind.QUEUED, 0, Messages.get("library.status.queued"));
            case APPLYING -> new LibraryStatus(StatusMark.Kind.PROGRESS, pack.progress().percent(),
                Messages.get("library.status.applying", pack.progress().percent()));
            case UP_TO_DATE, DONE -> new LibraryStatus(StatusMark.Kind.OK, 0, Messages.get("library.status.upToDate"));
            case DONE_WITH_ERRORS -> new LibraryStatus(StatusMark.Kind.WARN, 0,
                Messages.get("library.status.errors", pack.result().failed()));
            case CANCELLED -> new LibraryStatus(StatusMark.Kind.IDLE, 0, Messages.get("library.status.pending"));
            case ERROR -> new LibraryStatus(StatusMark.Kind.ERROR, 0, Messages.get(
                pack.error() instanceof SyncFailure.Network ? "library.status.offline" : "library.status.error"));
        };
    }
}
