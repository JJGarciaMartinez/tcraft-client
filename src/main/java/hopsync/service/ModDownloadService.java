package hopsync.service;

import hopsync.event.UpdateEvent;
import hopsync.model.ConfigEntry;
import hopsync.model.ModEntry;
import hopsync.model.ModInfo;
import hopsync.util.SafePaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles downloading of mod and config files.
 */
public class ModDownloadService {
    private final DownloadService downloadService;
    private final FileSystemService fileSystemService;

    public ModDownloadService(DownloadService downloadService, FileSystemService fileSystemService) {
        this.downloadService = downloadService;
        this.fileSystemService = fileSystemService;
    }

    public void downloadMods(List<ModEntry> mods, Consumer<UpdateEvent> eventPublisher) {
        Path modsFolder = fileSystemService.getModsFolder().toPath();
        int total = mods.size();
        int processed = 0;

        for (ModEntry mod : mods) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            processed++;
            eventPublisher.accept(new UpdateEvent.ProgressChanged(processed, total));
            eventPublisher.accept(new UpdateEvent.ModProcessing(ModInfo.of(mod)));

            try {
                File destination = SafePaths.resolveInside(modsFolder, mod.file()).toFile();

                if (destination.exists()) {
                    eventPublisher.accept(new UpdateEvent.ModAlreadyInstalled(mod.file()));
                    eventPublisher.accept(new UpdateEvent.LogMessage("OK: " + mod.file() + " (ya existe)"));
                } else {
                    eventPublisher.accept(new UpdateEvent.ModDownloading(mod.file()));
                    eventPublisher.accept(new UpdateEvent.LogMessage("Descargando: " + mod.file()));
                    downloadService.downloadFile(mod.url(), destination);
                    eventPublisher.accept(new UpdateEvent.LogMessage("Completado: " + mod.file()));
                    eventPublisher.accept(new UpdateEvent.ModCompleted(mod.file(), true));
                }
            } catch (IOException e) {
                eventPublisher.accept(new UpdateEvent.ModError(mod.file(), e.getMessage()));
                eventPublisher.accept(new UpdateEvent.LogMessage("Error en " + mod.file() + ": " + e.getMessage()));
                eventPublisher.accept(new UpdateEvent.ModCompleted(mod.file(), false));
            }
        }
    }

    public void downloadConfigs(List<ConfigEntry> configs, Consumer<UpdateEvent> eventPublisher) {
        Path configFolder = fileSystemService.getConfigFolder().toPath();

        for (ConfigEntry config : configs) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                Path destination = SafePaths.resolveInside(configFolder, config.path());
                if (!config.overwrite() && Files.exists(destination)) {
                    eventPublisher.accept(new UpdateEvent.LogMessage("Config conservada: " + config.path()));
                    continue;
                }
                Files.createDirectories(destination.getParent());
                eventPublisher.accept(new UpdateEvent.LogMessage("Descargando config: " + config.path()));
                downloadService.downloadFile(config.url(), destination.toFile());
                eventPublisher.accept(new UpdateEvent.LogMessage("Config completado: " + config.path()));
            } catch (IOException e) {
                eventPublisher.accept(new UpdateEvent.LogMessage(
                    "Advertencia: No se pudo descargar " + config.path() + ": " + e.getMessage()));
            }
        }
    }
}
