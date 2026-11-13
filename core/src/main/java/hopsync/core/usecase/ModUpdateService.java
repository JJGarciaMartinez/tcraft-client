package hopsync.core.usecase;

import hopsync.core.event.UpdateEvent;
import hopsync.core.manifest.Manifest;
import hopsync.core.port.GameFolders;
import hopsync.core.port.ManifestSource;
import hopsync.core.sync.FileDownloader;
import hopsync.core.sync.LocalState;
import hopsync.core.sync.StateStore;
import hopsync.core.sync.SyncExecutor;
import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlanner;

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

    private final GameFolders folders;
    private final ManifestSource manifestSource;
    private final FileDownloader downloader;

    public ModUpdateService(GameFolders folders, ManifestSource manifestSource,
                            FileDownloader downloader) {
        this.folders = folders;
        this.manifestSource = manifestSource;
        this.downloader = downloader;
    }

    public SyncPlan prepare(Consumer<UpdateEvent> eventPublisher) throws IOException {
        ensureFoldersExist(eventPublisher);

        eventPublisher.accept(new UpdateEvent.LogMessage("Obteniendo el manifest del pack..."));
        Manifest manifest = manifestSource.fetchManifest();
        eventPublisher.accept(new UpdateEvent.LogMessage("Pack: " + manifest.pack().name() + " " + manifest.pack().version()
            + " - " + manifest.mods().size() + " mods, " + manifest.configs().size() + " configs."));

        eventPublisher.accept(new UpdateEvent.LogMessage("Comparando con los archivos instalados..."));
        SyncPlanner planner = new SyncPlanner(folders.getModsFolder().toPath(),
            folders.getConfigFolder().toPath());
        return planner.plan(manifest, stateStore().load());
    }

    public SyncExecutor.Result apply(SyncPlan plan, Consumer<UpdateEvent> eventPublisher, BooleanSupplier cancelled)
            throws IOException {
        StateStore store = stateStore();
        LocalState previous = store.load();
        SyncExecutor executor = new SyncExecutor(downloader, folders.getModsFolder().toPath(),
            folders.getConfigFolder().toPath(), PARALLEL_DOWNLOADS);

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
        return new StateStore(folders.getStateFile().toPath());
    }

    private void ensureFoldersExist(Consumer<UpdateEvent> eventPublisher) throws IOException {
        File modsFolder = folders.getModsFolder();
        File configFolder = folders.getConfigFolder();

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
