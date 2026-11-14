package hopsync.desktop.screens.pack;

import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlan.ModChange;

import java.util.Locale;

/**
 * Counts of what a plan will do, for the review tags and the library.
 *
 * @param bytes total size of the files to download
 */
public record PlanSummary(long newMods, long updatedMods, long removedMods, long configs, long bytes) {

    public static PlanSummary of(SyncPlan plan) {
        return new PlanSummary(plan.count(ModChange.NEW), plan.count(ModChange.UPDATE), plan.removals().size(),
            plan.configDownloads().size(), plan.downloadBytes());
    }

    /** Files that will be written or deleted. */
    public long changes() {
        return newMods + updatedMods + removedMods + configs;
    }

    /** "21.3 / 35.2 MB": both amounts in the unit of the total, so they compare at a glance. */
    public static String formatPart(long done, long total) {
        if (total < 1024) return done + " / " + total + " B";
        double unit = total < 1024 * 1024 ? 1024.0 : 1024.0 * 1024.0;
        String name = total < 1024 * 1024 ? "KB" : "MB";
        return String.format(Locale.ROOT, "%.1f / %.1f %s", done / unit, total / unit, name);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
