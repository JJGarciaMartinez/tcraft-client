package service;

import config.AppConfig;
import model.ModInfo;
import util.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ModUpdater is responsible for managing the update process of mods in a specified folder.
 * It performs tasks such as identifying the mods folder, downloading a manifest from the server,
 * removing outdated mods, downloading new/updated mods, and notifying progress and changes
 * through provided callbacks.
 * <p>
 * The update process consists of the following steps:
 * 1. Detect or create the mods folder if necessary.
 * 2. Retrieve and parse the remote mod manifest (list of mods with metadata) from a configured server URL.
 * 3. Remove outdated mods that are no longer present in the remote manifest.
 * 4. Download new or updated mods from the remote source.
 * 5. Notify progress and mod information via the provided callbacks.
 * <p>
 * Thread-safety:
 * The class is not explicitly thread-safe. External synchronization might be needed if accessed
 * from multiple threads concurrently.
 * <p>
 * Constructor Parameters:
 * - logger: A Consumer function to log the progress or messages of the update process.
 * - progressUpdater: A BiConsumer function to report the progress of processed vs. total mods.
 * - modInfoCallback: A Consumer function to handle detailed information about each mod being processed.
 * <p>
 * Exception Handling:
 * The `initUpdate` method throws an Exception if a critical error is encountered that prevents
 * the update process from proceeding, such as an inability to create the mods folder or process
 * the remote manifest. Non-critical errors for individual mods are logged and skipped.
 */
public class ModUpdater {
    private final FileSystemService fileSystemService;
    private final DownloadService downloadService;
    private final Consumer<String> logger;
    private final BiConsumer<Integer, Integer> progressUpdater;
    private final Consumer<ModInfo> modInfoCallback;

    /**
     * Constructs a new instance of the {@code ModUpdater} class, which is responsible
     * for managing the update process of Minecraft mods. This constructor initializes
     * the updater with the necessary services and callbacks for logging, progress updates,
     * and mod information handling.
     *
     * @param logger A {@code Consumer<String>} used for logging messages during the update process.
     *               This can be used to output information, warnings, or error messages.
     * @param progressUpdater A {@code BiConsumer<Integer, Integer>} used for reporting progress updates.
     *                        The first parameter represents the current progress, and the second parameter
     *                        represents the total value to reach 100% completion.
     * @param modInfoCallback A {@code Consumer<ModInfo>} that is invoked for handling metadata about a mod.
     *                        This callback provides detailed information about a specific mod during the update process.
     */
    public ModUpdater(Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater, Consumer<ModInfo> modInfoCallback) {
        this.fileSystemService = new FileSystemService();
        this.downloadService = new DownloadService();
        this.logger = logger;
        this.progressUpdater = progressUpdater;
        this.modInfoCallback = modInfoCallback;
    }

    /**
     * Initializes and executes the update process for Minecraft mods.
     * <p>
     * This method performs the following operations in sequence:
     * 1. Detects and ensures the existence of the mods' folder. Creates the folder if it does not exist.
     * 2. Downloads the manifest (list of mods) from a remote server and parses it into a list of valid mods.
     * 3. Validates the manifest to ensure it contains at least one valid mod. Throws an exception if no valid mods are found.
     * 4. Cleans up outdated or obsolete mod files from the local mods folder by comparing them with the remote manifest.
     * 5. Downloads or skips each mod based on its existence in the local folder. If a mod file already exists locally,
     *    it is skipped, otherwise, it is downloaded from the server.
     * 6. Handles logging and progress updates throughout the update process.
     * <p>
     * If the process is interrupted (e.g., thread interruption), the method will stop gracefully with an appropriate
     * log message. Any issues with downloading individual mods are logged but do not cause the entire update process
     * to fail.
     *
     * @throws Exception if there is an issue creating the mods folder, or if the manifest obtained from the server is invalid
     *                   or empty.
     */
    public void initUpdate() throws Exception {
        logger.accept("Iniciando actualizador...");

        // 1. Detect or create mods folder
        File modsFolder = fileSystemService.getModsFolder();
        logger.accept("Carpeta de mods detectada: " + modsFolder);

        if (!modsFolder.exists()) {
            boolean created = modsFolder.mkdirs();
            if (!created) {
                throw new IOException("No se pudo crear la carpeta de mods: " + modsFolder);
            }
            logger.accept("Carpeta de mods creada.");
        }

        // 1.1 Detect or create config folder
        File configFolder = fileSystemService.getConfigFolder();
        logger.accept("Carpeta de config detectada: " + configFolder);

        if (!configFolder.exists()) {
            boolean created = configFolder.mkdirs();
            if (!created) {
                throw new IOException("No se pudo crear la carpeta de config: " + configFolder);
            }
            logger.accept("Carpeta de config creada.");
        }

        // 2. Download manifest from the server
        logger.accept("Obteniendo lista de mods del servidor...");
        String jsonContent = downloadService.getUrlString(AppConfig.URL_MANIFEST);
        List<ModInfo> modRemotes = JsonParser.jsonSimpleParse(jsonContent);

        logger.accept("Encontrados " + modRemotes.size() + " mods en el servidor.");

        // Validate manifest
        if (modRemotes.isEmpty()) {
            logger.accept("ERROR CRÍTICO: No se encontraron mods válidos en el manifest del servidor.");
            logger.accept("Posibles causas:");
            logger.accept("  - El manifest está vacío o mal formado");
            logger.accept("  - Faltan campos 'name' o 'url' en los mods");
            logger.accept("  - Problemas con el formato JSON");
            logger.accept("\nNo se realizarán cambios en los archivos locales.");
            throw new IOException("No se encontraron mods válidos en el manifest del servidor");
        }

        // 3. Delete outdated files
        logger.accept("--- Verificando archivos antiguos ---");
        File[] localFiles = modsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (localFiles != null) {
            for (File archivo : localFiles) {
                boolean existInRemote = false;
                for (ModInfo mod : modRemotes) {
                    if (mod.name().equals(archivo.getName())) {
                        existInRemote = true;
                        break;
                    }
                }
                if (!existInRemote) {
                    logger.accept("Eliminando obsoleto: " + archivo.getName());
                    boolean deleted = archivo.delete();
                    if (!deleted) {
                        logger.accept("Advertencia: No se pudo eliminar " + archivo.getName());
                    }
                }
            }
        }

        // 4. Download new/updated files
        logger.accept("--- Verificando actualizaciones ---");
        int total = modRemotes.size();
        int modProcesses = 0;

        for (ModInfo mod : modRemotes) {
            // Verify if the thread was interrupted during the previous iteration
            if (Thread.currentThread().isInterrupted()) {
                logger.accept("Actualización cancelada por el usuario");
                return;
            }

            File folderDestination = new File(modsFolder, mod.name());
            modProcesses++;
            progressUpdater.accept(modProcesses, total);

            try {
                if (!folderDestination.exists()) {
                    if (modInfoCallback != null) {
                        modInfoCallback.accept(mod);
                    }
                    logger.accept("Descargando: " + mod.name());
                    downloadService.downloadFile(mod.url(), folderDestination);
                    logger.accept("Completado: " + mod.name());
                } else {
                    if (modInfoCallback != null) {
                        modInfoCallback.accept(mod);
                    }
                    logger.accept("OK: " + mod.name() + " (ya existe)");
                }

                // Download configuration file if it exists
                if (mod.configUrl() != null && mod.configName() != null && !mod.configUrl().isEmpty() && !mod.configName().isEmpty()) {
                    File configDestination = new File(configFolder, mod.configName());
                    try {
                        logger.accept("Descargando config: " + mod.configName());
                        downloadService.downloadFile(mod.configUrl(), configDestination);
                        logger.accept("Config completado: " + mod.configName());
                    } catch (IOException configException) {
                        logger.accept("Advertencia: No se pudo descargar el archivo de configuración " + mod.configName() + ": " + configException.getMessage());
                    }
                }
            } catch (IOException e) {
                if (modInfoCallback != null) {
                    modInfoCallback.accept(mod);
                }
                logger.accept("Error en " + mod.name() + ": " + e.getMessage());
            }
        }
        logger.accept("\n--- ¡ACTUALIZACIÓN COMPLETADA! ---");
        logger.accept("Puedes cerrar esta ventana y jugar.");
    }
}
