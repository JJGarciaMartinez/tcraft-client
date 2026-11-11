package hopsync.service;

import hopsync.event.UpdateEvent;
import hopsync.model.Manifest;
import hopsync.sync.FileDownloader;
import hopsync.sync.LocalState;
import hopsync.sync.StateStore;
import hopsync.sync.SyncExecutor;
import hopsync.sync.SyncPlan;
import hopsync.sync.SyncPlanner;

import java.io.File;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Runs a sync in two steps: {@link #prepare} fetches the manifest and computes what would change
 * without touching any file, and {@link #apply} performs that plan and records the result.
 */
public class ModUpdateService {
    private static final int PARALLEL_DOWNLOADS = 4;

    private final FileSystemService fileSystemService;
    private final ManifestService manifestService;
    private final FileDownloader downloader;

    public ModUpdateService(FileSystemService fileSystemService, ManifestService manifestService,
                            FileDownloader downloader) {
        this.fileSystemService = fileSystemService;
        this.manifestService = manifestService;
        this.downloader = downloader;
    }

    public SyncPlan prepare(Consumer<UpdateEvent> eventPublisher) throws IOException {
        ensureFoldersExist(eventPublisher);

        eventPublisher.accept(new UpdateEvent.LogMessage("Obteniendo el manifest del pack..."));
        Manifest manifest = manifestService.fetchManifest();
        eventPublisher.accept(new UpdateEvent.LogMessage("Pack: " + manifest.pack().name() + " " + manifest.pack().version()
            + " - " + manifest.mods().size() + " mods, " + manifest.configs().size() + " configs."));

        eventPublisher.accept(new UpdateEvent.LogMessage("Comparando con los archivos instalados..."));
        SyncPlanner planner = new SyncPlanner(fileSystemService.getModsFolder().toPath(),
            fileSystemService.getConfigFolder().toPath());
        return planner.plan(manifest, stateStore().load());
    }

    public SyncExecutor.Result apply(SyncPlan plan, Consumer<UpdateEvent> eventPublisher, BooleanSupplier cancelled)
            throws IOException {
        StateStore store = stateStore();
        LocalState previous = store.load();
        SyncExecutor executor = new SyncExecutor(downloader, fileSystemService.getModsFolder().toPath(),
            fileSystemService.getConfigFolder().toPath(), PARALLEL_DOWNLOADS);

        SyncExecutor.Result result = executor.execute(plan, previous, new SyncExecutor.Listener() {
            @Override
            public void modStarted(String file) {
                eventPublisher.accept(new UpdateEvent.ModDownloading(file));
            }

            @Override
            public void modFinished(String file, boolean success, String error) {
                eventPublisher.accept(new UpdateEvent.ModCompleted(file, success));
            }

            @Override
            public void modRemoved(String file) {
                eventPublisher.accept(new UpdateEvent.ModRemoved(file));
            }

            @Override
            public void progress(int percent) {
                eventPublisher.accept(new UpdateEvent.ProgressChanged(percent));
            }

            @Override
            public void log(String message) {
                eventPublisher.accept(new UpdateEvent.LogMessage(message));
            }
        }, cancelled);

        store.save(result.state());
        return result;
    }

    private StateStore stateStore() {
        return new StateStore(fileSystemService.getStateFile().toPath());
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
