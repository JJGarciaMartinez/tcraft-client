package service;

import config.AppConfig;
import model.ModInfo;
import util.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModUpdater {

    private final FileSystemService fileSystemService;
    private final DownloadService downloadService;
    private final Consumer<String> logger;
    private final BiConsumer<Integer, Integer> progressUpdater;
    private final Consumer<ModInfo> modInfoCallback;

    public ModUpdater(Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater) {
        this(logger, progressUpdater, null);
    }

    public ModUpdater(Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater, Consumer<ModInfo> modInfoCallback) {
        this.fileSystemService = new FileSystemService();
        this.downloadService = new DownloadService();
        this.logger = logger;
        this.progressUpdater = progressUpdater;
        this.modInfoCallback = modInfoCallback;
    }

    public void initUpdate() throws Exception {
        logger.accept("Iniciando actualizador...");

        // 1. Detectar carpeta
        File carpetaMods = fileSystemService.getModsFolder();
        logger.accept("Carpeta detectada: " + carpetaMods);

        if (!carpetaMods.exists()) {
            boolean created = carpetaMods.mkdirs();
            if (!created) {
                throw new IOException("No se pudo crear la carpeta de mods: " + carpetaMods);
            }
            logger.accept("Carpeta creada.");
        }

        // 2. Descargar lista (Manifest)
        logger.accept("Obteniendo lista de mods del servidor...");
        String jsonContent = downloadService.getUrlString(AppConfig.URL_MANIFEST);
        List<ModInfo> modsRemotos = JsonParser.parsearJsonSimple(jsonContent);

        logger.accept("Encontrados " + modsRemotos.size() + " mods en el servidor.");

        // Validar que la lista no esté vacía
        if (modsRemotos.isEmpty()) {
            logger.accept("ERROR CRÍTICO: No se encontraron mods válidos en el manifest del servidor.");
            logger.accept("Posibles causas:");
            logger.accept("  - El manifest está vacío o mal formado");
            logger.accept("  - Faltan campos 'name' o 'url' en los mods");
            logger.accept("  - Problemas con el formato JSON");
            logger.accept("\nNo se realizarán cambios en los archivos locales.");
            throw new IOException("No se encontraron mods válidos en el manifest del servidor");
        }

        // 3. Limpieza de antiguos
        logger.accept("--- Verificando archivos antiguos ---");
        File[] archivosLocales = carpetaMods.listFiles((dir, name) -> name.endsWith(".jar"));
        if (archivosLocales != null) {
            for (File archivo : archivosLocales) {
                boolean existeEnRemoto = false;
                for (ModInfo mod : modsRemotos) {
                    if (mod.name().equals(archivo.getName())) {
                        existeEnRemoto = true;
                        break;
                    }
                }
                if (!existeEnRemoto) {
                    logger.accept("Eliminando obsoleto: " + archivo.getName());
                    boolean deleted = archivo.delete();
                    if (!deleted) {
                        logger.accept("Advertencia: No se pudo eliminar " + archivo.getName());
                    }
                }
            }
        }

        // 4. Descargar nuevos
        logger.accept("--- Verificando actualizaciones ---");
        int total = modsRemotos.size();
        int procesados = 0;

        for (ModInfo mod : modsRemotos) {
            // Verificar si el hilo ha sido interrumpido
            if (Thread.currentThread().isInterrupted()) {
                logger.accept("Actualización cancelada por el usuario");
                return;
            }

            File archivoDestino = new File(carpetaMods, mod.name());
            procesados++;
            progressUpdater.accept(procesados, total);

            try {
                if (!archivoDestino.exists()) {
                    // Notificar ModInfo completo ANTES del log para crear la tarjeta primero
                    if (modInfoCallback != null) {
                        modInfoCallback.accept(mod);
                    }
                    logger.accept("Descargando: " + mod.name());
                    downloadService.downloadFile(mod.url(), archivoDestino);
                    logger.accept("Completado: " + mod.name());
                } else {
                    // Notificar ModInfo completo ANTES del log
                    if (modInfoCallback != null) {
                        modInfoCallback.accept(mod);
                    }
                    logger.accept("OK: " + mod.name() + " (ya existe)");
                }
            } catch (IOException e) {
                // Notificar ModInfo ANTES del log de error
                if (modInfoCallback != null) {
                    modInfoCallback.accept(mod);
                }
                logger.accept("Error en " + mod.name() + ": " + e.getMessage());
                // Continuar con el siguiente mod en lugar de fallar completamente
            }
        }

        logger.accept("\n--- ¡ACTUALIZACIÓN COMPLETADA! ---");
        logger.accept("Puedes cerrar esta ventana y jugar.");
    }
}
