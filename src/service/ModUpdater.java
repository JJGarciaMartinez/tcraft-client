package service;

import config.AppConfig;
import model.ModInfo;
import util.JsonParser;

import java.io.File;
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

    public void initUpdate() {
        try {
            logger.accept("Iniciando actualizador...");

            // 1. Detectar carpeta
            File carpetaMods = fileSystemService.getModsFolder();
            logger.accept("Carpeta detectada: " + carpetaMods.getAbsolutePath());

            if (!carpetaMods.exists()) {
                carpetaMods.mkdirs();
                logger.accept("Carpeta creada.");
            }

            // 2. Descargar lista (Manifest)
            logger.accept("Obteniendo lista de mods del servidor...");
            String jsonContent = downloadService.getUrlString(AppConfig.URL_MANIFEST);
            List<ModInfo> modsRemotos = JsonParser.parsearJsonSimple(jsonContent);

            logger.accept("Encontrados " + modsRemotos.size() + " mods en el servidor.");

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
                File archivoDestino = new File(carpetaMods, mod.getName());
                procesados++;
                progressUpdater.accept(procesados, total);

                if (!archivoDestino.exists()) {
                    logger.accept("Descargando: " + mod.getName());
                    downloadService.downloadFile(mod.getUrl(), archivoDestino);
                } else {
                    logger.accept("OK: " + mod.getName() + " (ya existe)");
                }
            }

            logger.accept("\n--- ¡ACTUALIZACIÓN COMPLETADA! ---");
            logger.accept("Puedes cerrar esta ventana y jugar.");

        } catch (Exception e) {
            logger.accept("ERROR CRÍTICO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
