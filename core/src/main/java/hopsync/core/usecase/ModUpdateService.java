package hopsync.core.usecase;

import hopsync.core.event.UpdateEvent;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.port.GameFolders;
import hopsync.core.port.ManifestSource;
import hopsync.core.sync.FileDownloader;
import hopsync.core.sync.FolderLock;
import hopsync.core.sync.LocalState;
import hopsync.core.sync.StateStore;
import hopsync.core.sync.SyncExecutor;
import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlanner;
import hopsync.core.util.CancellationToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Runs a sync in two steps: {@link #prepare} fetches the manifest and computes what would change
 * without touching any file, and {@link #apply} performs that plan and records the result.
 */
public class ModUpdateService {
    private static final int PARALLEL_DOWNLOADS = 4;
    private static final String LOCK_FILE = "apply.lock";

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

        eventPublisher.accept(new UpdateEvent.LogMessage("Fetching the pack manifest"));
        Manifest manifest = manifestSource.fetchManifest();
        eventPublisher.accept(new UpdateEvent.LogMessage("Pack " + manifest.pack().name() + " " + manifest.pack().version()
            + ": " + manifest.mods().size() + " mods, " + manifest.configs().size() + " configs"));

        eventPublisher.accept(new UpdateEvent.LogMessage("Comparing with the installed files"));
        SyncPlanner planner = new SyncPlanner(folders.getModsFolder().toPath(),
            folders.getConfigFolder().toPath());
        return planner.plan(manifest, stateStore().load());
    }

    /**
     * Applies the plan while holding the game folder lock, then records the resulting state.
     *
     * @throws SyncException with {@link SyncFailure.FolderInUse} when another HopSync process is applying
     */
    public SyncExecutor.Result apply(SyncPlan plan, Consumer<UpdateEvent> eventPublisher, CancellationToken cancel)
            throws IOException {
        Path stateFile = folders.getStateFile().toPath();
        FolderLock lock = FolderLock.acquire(stateFile.resolveSibling(LOCK_FILE),
            folders.getModsFolder().toPath().toAbsolutePath().getParent());
        try {
            StateStore store = new StateStore(stateFile);
            LocalState previous = store.load();
            SyncExecutor executor = new SyncExecutor(downloader, folders.getModsFolder().toPath(),
                folders.getConfigFolder().toPath(), PARALLEL_DOWNLOADS);

            SyncExecutor.Result result = executor.execute(plan, previous, new SyncExecutor.Listener() {
                @Override
                public void modStarted(String file) {
                    eventPublisher.accept(new UpdateEvent.ModDownloading(file));
                }

                @Override
                public void modFinished(String file, boolean success, SyncFailure failure) {
                    eventPublisher.accept(new UpdateEvent.ModCompleted(file, success, failure));
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
            }, cancel);

            store.save(result.state());
            return result;
        } finally {
            lock.close();
        }
    }

    private StateStore stateStore() {
        return new StateStore(folders.getStateFile().toPath());
    }

    private void ensureFoldersExist(Consumer<UpdateEvent> eventPublisher) throws IOException {
        for (File folder : new File[] {folders.getModsFolder(), folders.getConfigFolder()}) {
            eventPublisher.accept(new UpdateEvent.LogMessage("Using folder " + folder));
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new SyncException(new SyncFailure.FolderUnavailable(folder.toString()));
            }
        }
    }
}
