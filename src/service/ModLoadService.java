package service;

import model.ModInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading current mods from the file system.
 * Moved from ui/ package to service/ package.
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
            mods.add(new ModInfo(
                jar.getName(),
                "Instalado",
                "",
                "",
                "",
                null,
                null
            ));
        }

        return mods;
    }
}
