package hopsync.desktop.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the jars currently in a mods folder.
 */
public class ModLoadService {

    public List<ModInfo> loadCurrentMods(Path modsPath) {
        List<ModInfo> mods = new ArrayList<>();
        File modsFolder = modsPath.toFile();

        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            return mods;
        }

        File[] jarFiles = modsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return mods;

        for (File jar : jarFiles) {
            mods.add(ModInfo.installed(jar.getName()));
        }

        return mods;
    }
}
