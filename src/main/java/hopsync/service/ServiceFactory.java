package hopsync.service;

import hopsync.event.EventBus;

/**
 * Factory for creating service instances with proper dependencies.
 * Provides a simple form of dependency injection without external frameworks.
 */
public class ServiceFactory {
    private static FileSystemService fileSystemService;
    private static DownloadService downloadService;
    private static ManifestService manifestService;
    private static ModUpdateService modUpdateService;
    private static EventBus eventBus;

    public static EventBus getEventBus() {
        if (eventBus == null) eventBus = new EventBus();
        return eventBus;
    }

    public static FileSystemService getFileSystemService() {
        if (fileSystemService == null) fileSystemService = new FileSystemService();
        return fileSystemService;
    }

    public static DownloadService getDownloadService() {
        if (downloadService == null) downloadService = new DownloadService();
        return downloadService;
    }

    public static ManifestService getManifestService() {
        if (manifestService == null) manifestService = new ManifestService(getDownloadService());
        return manifestService;
    }

    public static ModUpdateService getModUpdateService() {
        if (modUpdateService == null) {
            modUpdateService = new ModUpdateService(
                getFileSystemService(),
                getManifestService(),
                getDownloadService()
            );
        }
        return modUpdateService;
    }

    public static void reset() {
        fileSystemService = null;
        downloadService = null;
        manifestService = null;
        modUpdateService = null;
        eventBus = null;
    }
}