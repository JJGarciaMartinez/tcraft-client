package service;

import event.UpdateEvent;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Orchestrates the mod update process using specialized services.
 * Replaces the God Object ModUpdater.
 */
public class ModUpdateService {
    private final FileSystemService fileSystemService;
    private final ManifestService manifestService;
    private final ModCleanupService cleanupService;
    private final ModDownloadService downloadService;

    public ModUpdateService(
            FileSystemService fileSystemService,
            ManifestService manifestService,
            ModCleanupService cleanupService,
            ModDownloadService downloadService) {
        this.fileSystemService = fileSystemService;
        this.manifestService = manifestService;
        this.cleanupService = cleanupService;
        this.downloadService = downloadService;
    }

    public void performUpdate(Consumer<UpdateEvent> eventPublisher) throws IOException {
        eventPublisher.accept(new UpdateEvent.LogMessage("Iniciando actualizador..."));

        // 1. Ensure folders exist
        ensureFoldersExist(eventPublisher);

        // 2. Fetch manifest
        eventPublisher.accept(new UpdateEvent.LogMessage("Obteniendo lista de mods del servidor..."));
        var remoteMods = manifestService.fetchManifest();
        eventPublisher.accept(new UpdateEvent.LogMessage("Encontrados " + remoteMods.size() + " mods en el servidor."));

        // 3. Clean up outdated mods
        eventPublisher.accept(new UpdateEvent.LogMessage("--- Verificando archivos antiguos ---"));
        cleanupService.removeOutdatedMods(remoteMods, eventPublisher);

        // 4. Download new/updated mods
        eventPublisher.accept(new UpdateEvent.LogMessage("--- Verificando actualizaciones ---"));
        downloadService.downloadMods(remoteMods, eventPublisher);

        eventPublisher.accept(new UpdateEvent.LogMessage("\n--- ¡ACTUALIZACIÓN COMPLETADA! ---"));
        eventPublisher.accept(new UpdateEvent.LogMessage("Puedes cerrar esta ventana y jugar."));
    }

    private void ensureFoldersExist(Consumer<UpdateEvent> eventPublisher) throws IOException {
        File modsFolder = fileSystemService.getModsFolder();
        File configFolder = fileSystemService.getConfigFolder();

        eventPublisher.accept(new UpdateEvent.LogMessage("Carpeta de mods detectada: " + modsFolder));
        if (!modsFolder.exists() && !modsFolder.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de mods: " + modsFolder);
        }

        eventPublisher.accept(new UpdateEvent.LogMessage("Carpeta de config detectada: " + configFolder));
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de config: " + configFolder);
        }
    }
}
