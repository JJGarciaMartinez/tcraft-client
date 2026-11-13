package hopsync.core.usecase;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackPaths;
import hopsync.core.port.ManifestSource;
import hopsync.core.sync.FileDownloader;
import hopsync.core.sync.FolderLock;
import hopsync.core.sync.LocalState;
import hopsync.core.sync.StateStore;
import hopsync.core.sync.SyncExecutor;
import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlanner;
import hopsync.core.util.CancellationToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Runs a sync of one pack in two steps: {@link #prepare} fetches the manifest and computes what would
 * change without touching any file, and {@link #apply} performs that plan and records the result.
 */
public class ModUpdateService {
    private final ManifestSource manifestSource;
    private final FileDownloader downloader;
    private final IntSupplier parallelDownloads;

    /**
     * @param parallelDownloads read on every apply, so a changed preference applies to the next update
     */
    public ModUpdateService(ManifestSource manifestSource, FileDownloader downloader, IntSupplier parallelDownloads) {
        this.manifestSource = manifestSource;
        this.downloader = downloader;
        this.parallelDownloads = parallelDownloads;
    }

    public SyncPlan prepare(Pack pack, Consumer<SyncEvent> eventPublisher) throws IOException {
        PackPaths paths = pack.paths();
        ensureFoldersExist(pack, paths, eventPublisher);

        log(pack, eventPublisher, "Fetching the pack manifest from " + pack.manifestUrl());
        Manifest manifest = manifestSource.fetchManifest(pack.manifestUrl());
        log(pack, eventPublisher, "Pack " + manifest.pack().name() + " " + manifest.pack().version()
            + ": " + manifest.mods().size() + " mods, " + manifest.configs().size() + " configs");

        log(pack, eventPublisher, "Comparing with the installed files");
        SyncPlanner planner = new SyncPlanner(paths.mods(), paths.config());
        return planner.plan(manifest, new StateStore(paths.stateFile()).load());
    }

    /**
     * Applies the plan while holding the game folder lock, then records the resulting state.
     *
     * @throws SyncException with {@link SyncFailure.FolderInUse} when another HopSync process is applying
     */
    public SyncExecutor.Result apply(Pack pack, SyncPlan plan, Consumer<SyncEvent> eventPublisher,
                                     CancellationToken cancel) throws IOException {
        PackPaths paths = pack.paths();
        String id = pack.id();
        FolderLock lock = FolderLock.acquire(paths.applyLock(), paths.gameDir());
        try {
            StateStore store = new StateStore(paths.stateFile());
            LocalState previous = store.load();
            SyncExecutor executor = new SyncExecutor(downloader, paths.mods(), paths.config(),
                parallelDownloads.getAsInt());

            SyncExecutor.Result result = executor.execute(plan, previous, new SyncExecutor.Listener() {
                @Override
                public void modStarted(String file) {
                    eventPublisher.accept(new SyncEvent.ModDownloading(id, file));
                }

                @Override
                public void modFinished(String file, boolean success, SyncFailure failure) {
                    eventPublisher.accept(new SyncEvent.ModCompleted(id, file, success, failure));
                }

                @Override
                public void modRemoved(String file) {
                    eventPublisher.accept(new SyncEvent.ModRemoved(id, file));
                }

                @Override
                public void progress(int percent) {
                    eventPublisher.accept(new SyncEvent.ProgressChanged(id, percent));
                }

                @Override
                public void log(String message) {
                    eventPublisher.accept(new SyncEvent.LogMessage(id, message));
                }
            }, cancel);

            store.save(result.state());
            return result;
        } finally {
            lock.close();
        }
    }

    private void ensureFoldersExist(Pack pack, PackPaths paths, Consumer<SyncEvent> eventPublisher) throws IOException {
        for (Path folder : new Path[] {paths.mods(), paths.config()}) {
            log(pack, eventPublisher, "Using folder " + folder);
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                throw new SyncException(new SyncFailure.FolderUnavailable(folder.toString()));
            }
        }
    }

    private static void log(Pack pack, Consumer<SyncEvent> eventPublisher, String message) {
        eventPublisher.accept(new SyncEvent.LogMessage(pack.id(), message));
    }
}
