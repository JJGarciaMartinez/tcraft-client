package service;

import event.UpdateEvent;
import model.ModInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles downloading of mod files.
 */
public class ModDownloadService {
    private final DownloadService downloadService;
    private final FileSystemService fileSystemService;

    public ModDownloadService(DownloadService downloadService, FileSystemService fileSystemService) {
        this.downloadService = downloadService;
        this.fileSystemService = fileSystemService;
    }

    public void downloadMods(List<ModInfo> mods, Consumer<UpdateEvent> eventPublisher) {
        File modsFolder = fileSystemService.getModsFolder();
        File configFolder = fileSystemService.getConfigFolder();
        int total = mods.size();
        int processed = 0;

        for (ModInfo mod : mods) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            processed++;
            eventPublisher.accept(new UpdateEvent.ProgressChanged(processed, total));
            eventPublisher.accept(new UpdateEvent.ModProcessing(mod));

            try {
                File destination = new File(modsFolder, mod.name());

                if (destination.exists()) {
                    eventPublisher.accept(new UpdateEvent.ModAlreadyInstalled(mod.name()));
                    eventPublisher.accept(new UpdateEvent.LogMessage("OK: " + mod.name() + " (ya existe)"));
                } else {
                    eventPublisher.accept(new UpdateEvent.ModDownloading(mod.name()));
                    eventPublisher.accept(new UpdateEvent.LogMessage("Descargando: " + mod.name()));
                    downloadService.downloadFile(mod.url(), destination);
                    eventPublisher.accept(new UpdateEvent.LogMessage("Completado: " + mod.name()));
                    eventPublisher.accept(new UpdateEvent.ModCompleted(mod.name(), true));
                }

                downloadConfig(mod, configFolder, eventPublisher);

            } catch (IOException e) {
                eventPublisher.accept(new UpdateEvent.ModError(mod.name(), e.getMessage()));
                eventPublisher.accept(new UpdateEvent.LogMessage("Error en " + mod.name() + ": " + e.getMessage()));
                eventPublisher.accept(new UpdateEvent.ModCompleted(mod.name(), false));
            }
        }
    }

    private void downloadConfig(ModInfo mod, File configFolder, Consumer<UpdateEvent> eventPublisher) {
        if (mod.configUrl() != null && mod.configName() != null
                && !mod.configUrl().isEmpty() && !mod.configName().isEmpty()) {
            File configDestination = new File(configFolder, mod.configName());
            try {
                eventPublisher.accept(new UpdateEvent.LogMessage("Descargando config: " + mod.configName()));
                downloadService.downloadFile(mod.configUrl(), configDestination);
                eventPublisher.accept(new UpdateEvent.LogMessage("Config completado: " + mod.configName()));
            } catch (IOException e) {
                eventPublisher.accept(new UpdateEvent.LogMessage(
                    "Advertencia: No se pudo descargar " + mod.configName() + ": " + e.getMessage()));
            }
        }
    }
}
