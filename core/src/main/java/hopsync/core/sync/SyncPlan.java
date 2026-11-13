package hopsync.core.sync;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;

import java.util.List;
import java.util.Map;

/**
 * Everything a sync will do, computed before touching any file so the player can review it.
 *
 * @param mods     one action per mod in the manifest, in manifest order
 * @param removals file names of mods HopSync installed earlier that are no longer in the pack
 * @param configs  one action per config in the manifest
 * @param kept     records of files already up to date, carried into the new state as they are
 */
public record SyncPlan(Manifest manifest, List<ModAction> mods, List<String> removals,
                       List<ConfigAction> configs, Map<String, FileRecord> keptMods,
                       Map<String, FileRecord> keptConfigs) {

    public enum ModChange { NEW, UPDATE, UP_TO_DATE }

    public enum ConfigChange {
        /** Missing on disk. */
        INSTALL,
        /** Differs and the manifest enforces it, or the player never edited the previous version. */
        REPLACE,
        /** The player edited it and the manifest does not enforce it. */
        KEEP_EDITED,
        UP_TO_DATE
    }

    public record ModAction(ModEntry mod, ModChange change) {
        public boolean needsDownload() {
            return change != ModChange.UP_TO_DATE;
        }
    }

    public record ConfigAction(ConfigEntry config, ConfigChange change) {
        public boolean needsDownload() {
            return change == ConfigChange.INSTALL || change == ConfigChange.REPLACE;
        }
    }

    public SyncPlan {
        mods = List.copyOf(mods);
        removals = List.copyOf(removals);
        configs = List.copyOf(configs);
        keptMods = Map.copyOf(keptMods);
        keptConfigs = Map.copyOf(keptConfigs);
    }

    public List<ModAction> modDownloads() {
        return mods.stream().filter(ModAction::needsDownload).toList();
    }

    public List<ConfigAction> configDownloads() {
        return configs.stream().filter(ConfigAction::needsDownload).toList();
    }

    public long count(ModChange change) {
        return mods.stream().filter(a -> a.change() == change).count();
    }

    public long downloadBytes() {
        return modDownloads().stream().mapToLong(a -> a.mod().size()).sum()
            + configDownloads().stream().mapToLong(a -> a.config().size()).sum();
    }

    public boolean isUpToDate() {
        return modDownloads().isEmpty() && removals.isEmpty() && configDownloads().isEmpty();
    }
}
