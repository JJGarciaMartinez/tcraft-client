package hopsync.core.sync;

/**
 * How far an apply has got: files processed (downloaded, failed or removed) and bytes downloaded.
 */
public record ProgressSnapshot(int doneFiles, int totalFiles, long bytesDone, long bytesTotal) {

    /** 0–100, by bytes when there is something to download, else by files. */
    public int percent() {
        if (bytesTotal > 0) return Math.clamp(bytesDone * 100 / bytesTotal, 0, 100);
        return totalFiles > 0 ? doneFiles * 100 / totalFiles : 100;
    }
}
