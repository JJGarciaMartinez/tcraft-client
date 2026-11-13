package hopsync.desktop;

import hopsync.core.event.EventBus;
import hopsync.core.event.UpdateEvent;
import hopsync.core.session.UpdateCoordinator;
import hopsync.core.usecase.ModUpdateService;
import hopsync.desktop.config.AppConfig;
import hopsync.platform.fs.FileSystemService;
import hopsync.platform.http.DownloadService;
import hopsync.platform.manifest.ManifestService;
import hopsync.platform.settings.SettingsService;
import hopsync.platform.settings.SettingsStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Composition root: builds every service once, wiring dependencies through constructors.
 */
public final class AppContainer {
    private static final System.Logger SYNC_LOG = System.getLogger("hopsync.sync");

    private final EventBus eventBus = new EventBus();
    private final SettingsService settingsService;
    private final FileSystemService fileSystemService;
    private final ManifestService manifestService;
    private final UpdateCoordinator updateCoordinator;
    private final ExecutorService backgroundExecutor =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("hopsync-update-", 0).factory());

    public AppContainer(AppConfig config, SettingsStore settingsStore) {
        settingsService = new SettingsService(settingsStore, config.presetManifestUrl());
        fileSystemService = new FileSystemService(settingsService);
        DownloadService downloadService = new DownloadService(config.network());
        manifestService = new ManifestService(downloadService, settingsService);
        ModUpdateService modUpdateService = new ModUpdateService(fileSystemService, manifestService, downloadService);
        updateCoordinator = new UpdateCoordinator(eventBus, modUpdateService, backgroundExecutor);
        // Progress details from the sync engine go to the log file, where support can read them
        eventBus.subscribe(event -> {
            if (event instanceof UpdateEvent.LogMessage log) {
                SYNC_LOG.log(System.Logger.Level.INFO, log.message());
            }
        });
    }

    public static AppContainer forCurrentUser() {
        return new AppContainer(AppConfig.load(), SettingsStore.forCurrentUser());
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public FileSystemService fileSystemService() {
        return fileSystemService;
    }

    public ManifestService manifestService() {
        return manifestService;
    }

    public UpdateCoordinator updateCoordinator() {
        return updateCoordinator;
    }
}
