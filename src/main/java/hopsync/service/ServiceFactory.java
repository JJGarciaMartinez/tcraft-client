package hopsync.service;

import hopsync.config.SettingsStore;
import hopsync.event.EventBus;

/**
 * Factory for creating service instances with proper dependencies.
 * Provides a simple form of dependency injection without external frameworks.
 */
public class ServiceFactory {
    private static SettingsService settingsService;
    private static FileSystemService fileSystemService;
    private static DownloadService downloadService;
    private static ManifestService manifestService;
    private static ModUpdateService modUpdateService;
    private static EventBus eventBus;

    public static synchronized EventBus getEventBus() {
        if (eventBus == null) eventBus = new EventBus();
        return eventBus;
    }

    public static synchronized SettingsService getSettingsService() {
        if (settingsService == null) settingsService = new SettingsService(SettingsStore.forCurrentUser());
        return settingsService;
    }

    public static synchronized FileSystemService getFileSystemService() {
        if (fileSystemService == null) fileSystemService = new FileSystemService(getSettingsService());
        return fileSystemService;
    }

    public static synchronized DownloadService getDownloadService() {
        if (downloadService == null) downloadService = new DownloadService();
        return downloadService;
    }

    public static synchronized ManifestService getManifestService() {
        if (manifestService == null) manifestService = new ManifestService(getDownloadService(), getSettingsService());
        return manifestService;
    }

    public static synchronized ModUpdateService getModUpdateService() {
        if (modUpdateService == null) {
            modUpdateService = new ModUpdateService(
                getFileSystemService(),
                getManifestService(),
                getDownloadService()
            );
        }
        return modUpdateService;
    }

    public static synchronized void reset() {
        settingsService = null;
        fileSystemService = null;
        downloadService = null;
        manifestService = null;
        modUpdateService = null;
        eventBus = null;
    }
}