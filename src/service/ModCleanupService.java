package service;

import event.UpdateEvent;
import model.ModInfo;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles cleanup of outdated mod files.
 */
public class ModCleanupService {
    private final FileSystemService fileSystemService;

    public ModCleanupService(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    public void removeOutdatedMods(List<ModInfo> remoteMods, Consumer<UpdateEvent> eventPublisher) {
        File modsFolder = fileSystemService.getModsFolder();
        File[] localFiles = modsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (localFiles == null) return;

        for (File localFile : localFiles) {
            boolean existsInRemote = remoteMods.stream()
                .anyMatch(mod -> mod.name().equals(localFile.getName()));

            if (!existsInRemote) {
                eventPublisher.accept(new UpdateEvent.LogMessage("Eliminando obsoleto: " + localFile.getName()));
                if (!localFile.delete()) {
                    eventPublisher.accept(new UpdateEvent.LogMessage("Advertencia: No se pudo eliminar " + localFile.getName()));
                }
            }
        }
    }
}
