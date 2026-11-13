package hopsync.desktop;

import hopsync.core.event.EventBus;
import hopsync.core.event.SyncEvent;
import hopsync.core.pack.PackRegistry;
import hopsync.core.session.ApplyQueue;
import hopsync.core.session.PackSessions;
import hopsync.core.usecase.ModUpdateService;
import hopsync.desktop.config.AppConfig;
import hopsync.platform.http.DownloadService;
import hopsync.platform.manifest.ManifestService;
import hopsync.platform.settings.SettingsStore;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Composition root: builds every service once, wiring dependencies through constructors. Objects that
 * belong to one pack always come from {@link PackSessions}.
 */
public final class AppContainer {
    private static final System.Logger SYNC_LOG = System.getLogger("hopsync.sync");

    private final AppConfig config;
    private final EventBus eventBus = new EventBus();
    private final SettingsStore settingsStore;
    private final PackRegistry packRegistry;
    private final ManifestService manifestService;
    private final PackSessions packSessions;
    private final ExecutorService backgroundExecutor =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("hopsync-update-", 0).factory());

    public AppContainer(AppConfig config, SettingsStore settingsStore) {
        this.config = config;
        this.settingsStore = settingsStore;
        packRegistry = new PackRegistry(settingsStore);
        DownloadService downloadService = new DownloadService(config.network());
        manifestService = new ManifestService(downloadService, settingsStore.getFile().resolveSibling("manifests"));
        ModUpdateService modUpdateService = new ModUpdateService(manifestService, downloadService,
            packRegistry::parallelDownloads);
        ApplyQueue applyQueue = new ApplyQueue(backgroundExecutor, packRegistry::chainQueue);
        packSessions = new PackSessions(packRegistry, modUpdateService, applyQueue, eventBus::publish,
            backgroundExecutor);
        // Progress details from the sync engine go to the log file, where support can read them
        eventBus.subscribe(event -> {
            if (event instanceof SyncEvent.LogMessage log) {
                SYNC_LOG.log(System.Logger.Level.INFO, "[" + log.packId() + "] " + log.message());
            }
        });
    }

    public static AppContainer forCurrentUser() {
        return new AppContainer(AppConfig.load(), SettingsStore.forCurrentUser());
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public PackRegistry packRegistry() {
        return packRegistry;
    }

    public PackSessions packSessions() {
        return packSessions;
    }

    public ManifestService manifestService() {
        return manifestService;
    }

    /** Virtual threads for background work: checks, applies, listing files, fetching manifests. */
    public Executor backgroundExecutor() {
        return backgroundExecutor;
    }

    /** Manifest suggested to new players, from app.properties (may be empty). */
    public String presetManifestUrl() {
        return config.presetManifestUrl();
    }

    /** Where the settings are saved, to name it when saving fails. */
    public Path settingsFile() {
        return settingsStore.getFile();
    }
}
