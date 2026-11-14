package hopsync.desktop.store;

/**
 * App-wide preferences, as the settings sheet edits them.
 *
 * @param parallelDownloads files downloaded at the same time, 1–8
 */
public record Preferences(boolean checkOnStart, boolean chainQueue, int parallelDownloads) {
    public static final int MIN_PARALLEL = 1;
    public static final int MAX_PARALLEL = 8;
    public static final Preferences DEFAULT = new Preferences(true, true, 4);

    public Preferences {
        parallelDownloads = Math.clamp(parallelDownloads, MIN_PARALLEL, MAX_PARALLEL);
    }
}
