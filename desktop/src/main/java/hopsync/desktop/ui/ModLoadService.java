package hopsync.desktop.ui;

import hopsync.platform.fs.FileSystemService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the jars currently in the mods folder.
 */
public class ModLoadService {
    private final FileSystemService fileSystemService;

    public ModLoadService(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    public List<ModInfo> loadCurrentMods() {
        List<ModInfo> mods = new ArrayList<>();
        File modsFolder = fileSystemService.getModsFolder();

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
