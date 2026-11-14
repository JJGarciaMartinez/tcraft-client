package hopsync.desktop.screens.library;

import hopsync.core.failure.SyncFailure;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.screens.pack.PlanSummary;
import hopsync.desktop.store.PackUiState;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The one-line status of a pack in the library. Pure, so each phase is tested without a window.
 *
 * @param percent download progress for {@link StatusMark.Kind#PROGRESS}, else 0
 */
public record LibraryStatus(StatusMark.Kind mark, int percent, String text) {

    /**
     * The name each pack shows in the library. Copies of the same pack (same name) add their folder, e.g.
     * "Valle Encantado · Create 1.21", so the player can tell them apart.
     */
    public static Map<String, String> names(Collection<PackUiState> packs) {
        Map<String, Long> sameName = packs.stream()
            .collect(Collectors.groupingBy(PackUiState::displayName, Collectors.counting()));
        Map<String, String> names = new LinkedHashMap<>();
        for (PackUiState pack : packs) {
            String name = pack.displayName();
            names.put(pack.packId(), sameName.get(name) > 1 ? name + " · " + folderName(pack.gameDir()) : name);
        }
        return names;
    }

    /** A folder's own name; for launcher instances kept in ".../<instance>/minecraft", the instance's. */
    static String folderName(Path folder) {
        Path name = folder.getFileName();
        if (name == null) return folder.toString();
        Path parent = folder.getParent();
        if (name.toString().equals("minecraft") && parent != null && parent.getFileName() != null) {
            return parent.getFileName().toString();
        }
        return name.toString();
    }

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
