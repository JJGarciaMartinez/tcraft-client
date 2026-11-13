package hopsync.desktop.effects;

import hopsync.core.pack.PackPaths;
import hopsync.core.sync.FileRecord;
import hopsync.core.sync.LocalState;
import hopsync.core.sync.StateStore;
import hopsync.desktop.store.InstalledMods;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a pack's {@code mods/} folder and HopSync's record of what it installed there.
 */
public class ModLoadService {

    /**
     * @return jars sorted alphabetically with their sizes, plus the managed files and their versions; nothing
     *         when the folder does not exist
     */
    public InstalledMods load(PackPaths paths) {
        File[] jarFiles = paths.mods().toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return InstalledMods.NONE;
        }
        Arrays.sort(jarFiles, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
        List<String> jars = Arrays.stream(jarFiles).map(File::getName).toList();
        Map<String, Long> sizes = new HashMap<>();
        for (File jar : jarFiles) {
            sizes.put(jar.getName(), jar.length());
        }
        LocalState state = new StateStore(paths.stateFile()).load();
        Map<String, String> versions = new HashMap<>();
        for (Map.Entry<String, FileRecord> entry : state.mods().entrySet()) {
            if (entry.getValue().version() != null) {
                versions.put(entry.getKey(), entry.getValue().version());
            }
        }
        return new InstalledMods(jars, sizes, state.mods().keySet(), versions);
    }
}
