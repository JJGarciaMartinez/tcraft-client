package hopsync.core.sync;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.sync.SyncPlan.ConfigAction;
import hopsync.core.sync.SyncPlan.ConfigChange;
import hopsync.core.sync.SyncPlan.ModAction;
import hopsync.core.sync.SyncPlan.ModChange;
import hopsync.core.util.SafePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compares a manifest with the files on disk and what HopSync installed before, without
 * modifying anything. Files HopSync did not install are never scheduled for removal.
 */
public class SyncPlanner {
    private final Path modsDir;
    private final Path configDir;

    public SyncPlanner(Path modsDir, Path configDir) {
        this.modsDir = modsDir;
        this.configDir = configDir;
    }

    public SyncPlan plan(Manifest manifest, LocalState state) throws IOException {
        List<ModAction> modActions = new ArrayList<>();
        Map<String, FileRecord> keptMods = new HashMap<>();
        for (ModEntry mod : manifest.mods()) {
            Path path = SafePaths.resolveInside(modsDir, mod.file());
            if (!Files.isRegularFile(path)) {
                modActions.add(new ModAction(mod, ModChange.NEW));
                continue;
            }
            FileRecord current = FileHasher.describe(path, state.mods().get(mod.file()));
            if (current.sha512().equals(mod.sha512())) {
                modActions.add(new ModAction(mod, ModChange.UP_TO_DATE));
                // Same hash as the manifest, so this is the manifest's version (also fills in old records)
                keptMods.put(mod.file(), current.withVersion(mod.version()));
            } else {
                modActions.add(new ModAction(mod, ModChange.UPDATE));
            }
        }

        List<String> removals = planRemovals(manifest, state);

        List<ConfigAction> configActions = new ArrayList<>();
        Map<String, FileRecord> keptConfigs = new HashMap<>();
        for (ConfigEntry config : manifest.configs()) {
            Path path = SafePaths.resolveInside(configDir, config.path());
            if (!Files.isRegularFile(path)) {
                configActions.add(new ConfigAction(config, ConfigChange.INSTALL));
                continue;
            }
            FileRecord installed = state.configs().get(config.path());
            FileRecord current = FileHasher.describe(path, installed);
            boolean untouchedByPlayer = installed != null && installed.sha512().equals(current.sha512());
            if (current.sha512().equals(config.sha512())) {
                configActions.add(new ConfigAction(config, ConfigChange.UP_TO_DATE));
                keptConfigs.put(config.path(), current);
            } else if (config.overwrite() || untouchedByPlayer) {
                configActions.add(new ConfigAction(config, ConfigChange.REPLACE));
            } else {
                configActions.add(new ConfigAction(config, ConfigChange.KEEP_EDITED));
            }
        }

        return new SyncPlan(manifest, modActions, removals, configActions, keptMods, keptConfigs);
    }

    /**
     * Mods HopSync installed that left the pack. A file the player replaced since installation
     * (hash no longer matches the record) is left alone and simply stops being managed.
     */
    private List<String> planRemovals(Manifest manifest, LocalState state) throws IOException {
        Set<String> inManifest = manifest.mods().stream().map(ModEntry::file).collect(Collectors.toSet());
        Set<String> removals = new TreeSet<>();
        for (Map.Entry<String, FileRecord> entry : state.mods().entrySet()) {
            String file = entry.getKey();
            if (inManifest.contains(file) || !SafePaths.isSafeFileName(file)) {
                continue;
            }
            Path path = SafePaths.resolveInside(modsDir, file);
            if (Files.isRegularFile(path)
                    && FileHasher.describe(path, entry.getValue()).sha512().equals(entry.getValue().sha512())) {
                removals.add(file);
            }
        }
        return List.copyOf(removals);
    }
}
