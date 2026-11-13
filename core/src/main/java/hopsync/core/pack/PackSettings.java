package hopsync.core.pack;

import java.util.List;
import java.util.Optional;

/**
 * Everything the player configured: the packs, the one shown first and the app-wide preferences.
 *
 * @param packs             in the order they were added
 * @param activeId          id of the selected pack, or {@code null} when there are no packs
 * @param checkOnStart      check every pack in the background when the app starts
 * @param chainQueue        keep applying queued packs after the running one is cancelled
 * @param parallelDownloads files downloaded at the same time, 1–8
 */
public record PackSettings(List<Pack> packs, String activeId, boolean checkOnStart, boolean chainQueue,
                           int parallelDownloads) {

    public static final int MIN_PARALLEL_DOWNLOADS = 1;
    public static final int MAX_PARALLEL_DOWNLOADS = 8;
    public static final PackSettings EMPTY = new PackSettings(List.of(), null, true, true, 4);

    public PackSettings {
        packs = List.copyOf(packs);
        String requested = activeId;
        if (requested == null || packs.stream().noneMatch(pack -> pack.id().equals(requested))) {
            activeId = packs.isEmpty() ? null : packs.getFirst().id();
        }
        parallelDownloads = Math.clamp(parallelDownloads, MIN_PARALLEL_DOWNLOADS, MAX_PARALLEL_DOWNLOADS);
    }

    public Optional<Pack> active() {
        return find(activeId);
    }

    public Optional<Pack> find(String id) {
        return packs.stream().filter(pack -> pack.id().equals(id)).findFirst();
    }

    public PackSettings withPacks(List<Pack> newPacks, String newActiveId) {
        return new PackSettings(newPacks, newActiveId, checkOnStart, chainQueue, parallelDownloads);
    }
}
