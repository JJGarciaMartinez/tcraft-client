package hopsync.service;

import hopsync.event.UpdateEvent;
import hopsync.model.ModEntry;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles cleanup of outdated mod files.
 */
public class ModCleanupService {
    private final FileSystemService fileSystemService;

    public ModCleanupService(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    public void removeOutdatedMods(List<ModEntry> remoteMods, Consumer<UpdateEvent> eventPublisher) {
        File modsFolder = fileSystemService.getModsFolder();
        File[] localFiles = modsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (localFiles == null) return;

        Set<String> remoteFiles = remoteMods.stream().map(ModEntry::file).collect(Collectors.toSet());
        for (File localFile : localFiles) {
            if (!remoteFiles.contains(localFile.getName())) {
                eventPublisher.accept(new UpdateEvent.LogMessage("Eliminando obsoleto: " + localFile.getName()));
                if (!localFile.delete()) {
                    eventPublisher.accept(new UpdateEvent.LogMessage("Advertencia: No se pudo eliminar " + localFile.getName()));
                }
            }
        }
    }
}
