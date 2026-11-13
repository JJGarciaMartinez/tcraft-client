package hopsync.desktop.effects;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Lists the jars currently in a mods folder.
 */
public class ModLoadService {

    /**
     * @return jar file names sorted alphabetically, or an empty list when the folder does not exist
     */
    public List<String> loadCurrentMods(Path modsPath) {
        File[] jarFiles = modsPath.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return List.of();
        }
        return Arrays.stream(jarFiles).map(File::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
