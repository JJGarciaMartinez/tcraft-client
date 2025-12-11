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

    public ModUpdater(Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater) {
        this.fileSystemService = new FileSystemService();
        this.downloadService = new DownloadService();
        this.logger = logger;
        this.progressUpdater = progressUpdater;
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
                    if (mod.getName().equals(archivo.getName())) {
                        existeEnRemoto = true;
                        break;
                    }
                }
                if (!existeEnRemoto) {
                    logger.accept("Eliminando obsoleto: " + archivo.getName());
                    archivo.delete();
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

            File archivoDestino = new File(carpetaMods, mod.getName());
            procesados++;
            progressUpdater.accept(procesados, total);

            try {
                if (!archivoDestino.exists()) {
                    logger.accept("Descargando: " + mod.getName());
                    downloadService.downloadFile(mod.getUrl(), archivoDestino);
                    logger.accept("Completado: " + mod.getName());
                } else {
                    logger.accept("OK: " + mod.getName() + " (ya existe)");
                }
            } catch (IOException e) {
                logger.accept("Error en " + mod.getName() + ": " + e.getMessage());
                // Continuar con el siguiente mod en lugar de fallar completamente
            }
        }

        logger.accept("\n--- ¡ACTUALIZACIÓN COMPLETADA! ---");
        logger.accept("Puedes cerrar esta ventana y jugar.");
    }
}
