package hopsync.core.sync;

import hopsync.core.failure.SyncFailure;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.ModEntry;
import hopsync.core.sync.SyncPlan.ConfigAction;
import hopsync.core.sync.SyncPlan.ModAction;
import hopsync.core.util.CancellationToken;
import hopsync.core.util.SafePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Applies a {@link SyncPlan}: downloads mods in parallel, removes mods that left the pack and
 * installs configs, then returns the new {@link LocalState}. Removals and configs only run when
 * the downloads were not cancelled. Whatever happens, the returned state describes the files
 * on disk, so it can always be saved.
 */
public class SyncExecutor {
    private static final System.Logger LOG = System.getLogger(SyncExecutor.class.getName());

    /** Progress callbacks; may be invoked from worker threads. */
    public interface Listener {
        int FILE_PROGRESS_STEP = 5;

        void modStarted(String file);

        /**
         * @param failure why the mod could not be installed, or {@code null} on success
         */
        void modFinished(String file, boolean success, SyncFailure failure);

        void modRemoved(String file);

        /** Called when a file finishes or the downloaded share changes by at least 0.1 %. */
        void progress(ProgressSnapshot snapshot);

        /** Download progress of one mod, in steps of {@value #FILE_PROGRESS_STEP} %. */
        void modProgress(String file, int percent);

        void log(String message);
    }

    public record Result(LocalState state, int succeeded, int failed, boolean cancelled) {
    }

    private final FileDownloader downloader;
    private final Path modsDir;
    private final Path configDir;
    private final int parallelism;

    public SyncExecutor(FileDownloader downloader, Path modsDir, Path configDir, int parallelism) {
        this.downloader = downloader;
        this.modsDir = modsDir;
        this.configDir = configDir;
        this.parallelism = parallelism;
    }

    public Result execute(SyncPlan plan, LocalState previous, Listener listener, CancellationToken cancel) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        BooleanSupplier isCancelled = () -> cancelled.get() || cancel.isCancelled();
        deleteStaleParts(listener);

        Map<String, FileRecord> mods = new ConcurrentHashMap<>(plan.keptMods());
        Map<String, FileRecord> configs = new ConcurrentHashMap<>(plan.keptConfigs());
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        Progress progress = new Progress(plan.downloadBytes(),
            plan.modDownloads().size() + plan.removals().size() + plan.configDownloads().size(), listener);

        downloadMods(plan.modDownloads(), mods, succeeded, failed, progress, listener, cancel, isCancelled, cancelled);

        // A mod whose update failed keeps its old file, so it stays managed under its old record
        for (ModAction action : plan.modDownloads()) {
            String file = action.mod().file();
            FileRecord old = previous.mods().get(file);
            if (!mods.containsKey(file) && old != null) {
                mods.put(file, old);
            }
        }

        if (isCancelled.getAsBoolean()) {
            plan.removals().forEach(file -> mods.put(file, previous.mods().get(file)));
            keepPreviousConfigRecords(plan, previous, configs);
            return new Result(new LocalState(previous.contentHash(), mods, configs),
                succeeded.get(), failed.get(), true);
        }

        removeMods(plan.removals(), previous, mods, progress, listener);
        installConfigs(plan.configs(), previous, configs, failed, progress, listener, cancel, isCancelled);

        boolean complete = failed.get() == 0 && !isCancelled.getAsBoolean();
        String contentHash = complete ? plan.manifest().contentHash() : previous.contentHash();
        return new Result(new LocalState(contentHash, mods, configs), succeeded.get(), failed.get(),
            isCancelled.getAsBoolean());
    }

    private void downloadMods(List<ModAction> downloads, Map<String, FileRecord> mods, AtomicInteger succeeded,
                              AtomicInteger failed, Progress progress, Listener listener,
                              CancellationToken cancel, BooleanSupplier isCancelled, AtomicBoolean cancelled) {
        if (downloads.isEmpty()) {
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(parallelism, downloads.size()),
            Thread.ofVirtual().name("hopsync-download-", 0).factory());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (ModAction action : downloads) {
                futures.add(pool.submit(() -> {
                    ModEntry mod = action.mod();
                    if (isCancelled.getAsBoolean()) {
                        return;
                    }
                    listener.modStarted(mod.file());
                    try {
                        Path target = SafePaths.resolveInside(modsDir, mod.file());
                        FileProgress fileProgress = new FileProgress(mod.file(), mod.size(), listener);
                        downloader.download(mod.url(), target, mod.sha512(), mod.size(), bytes -> {
                            progress.add(bytes);
                            fileProgress.add(bytes);
                        }, cancel);
                        mods.put(mod.file(), recordOf(target, mod.sha512()).withVersion(mod.version()));
                        succeeded.incrementAndGet();
                        listener.modFinished(mod.file(), true, null);
                        progress.fileDone();
                    } catch (CancellationException e) {
                        cancelled.set(true);
                    } catch (IOException | RuntimeException e) {
                        failed.incrementAndGet();
                        SyncFailure failure = SyncFailures.from(e, mod.file());
                        listener.modFinished(mod.file(), false, failure);
                        listener.log("Failed to install " + mod.file() + ": " + failure.describe());
                        progress.fileDone();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            cancelled.set(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unexpected download task failure", e.getCause());
        } finally {
            pool.shutdownNow();
            awaitTermination(pool);
        }
    }

    private void removeMods(List<String> removals, LocalState previous, Map<String, FileRecord> mods,
                            Progress progress, Listener listener) {
        for (String file : removals) {
            try {
                Files.deleteIfExists(SafePaths.resolveInside(modsDir, file));
                listener.modRemoved(file);
                listener.log("Removed " + file);
            } catch (IOException e) {
                mods.put(file, previous.mods().get(file));
                listener.log("Could not remove " + file + ": " + SyncFailures.from(e, file).describe());
            }
            progress.fileDone();
        }
    }

    private void installConfigs(List<ConfigAction> actions, LocalState previous, Map<String, FileRecord> configs,
                                AtomicInteger failed, Progress progress, Listener listener,
                                CancellationToken cancel, BooleanSupplier isCancelled) {
        for (ConfigAction action : actions) {
            ConfigEntry config = action.config();
            FileRecord old = previous.configs().get(config.path());
            switch (action.change()) {
                case UP_TO_DATE -> { }
                case KEEP_EDITED -> {
                    // Remember the version HopSync installed, so later updates still see the player's edit
                    if (old != null) configs.put(config.path(), old);
                    listener.log("Kept config edited by the player: " + config.path());
                }
                case INSTALL, REPLACE -> {
                    if (isCancelled.getAsBoolean()) {
                        if (old != null) configs.put(config.path(), old);
                        continue;
                    }
                    try {
                        Path target = SafePaths.resolveInside(configDir, config.path());
                        Files.createDirectories(target.getParent());
                        downloader.download(config.url(), target, config.sha512(), config.size(), progress::add,
                            cancel);
                        configs.put(config.path(), recordOf(target, config.sha512()));
                        listener.log("Installed config " + config.path());
                        progress.fileDone();
                    } catch (CancellationException e) {
                        if (old != null) configs.put(config.path(), old);
                    } catch (IOException | RuntimeException e) {
                        failed.incrementAndGet();
                        if (old != null) configs.put(config.path(), old);
                        listener.log("Could not install config " + config.path() + ": " + SyncFailures.from(e, config.path()).describe());
                        progress.fileDone();
                    }
                }
            }
        }
    }

    private static void keepPreviousConfigRecords(SyncPlan plan, LocalState previous, Map<String, FileRecord> configs) {
        for (ConfigAction action : plan.configs()) {
            FileRecord old = previous.configs().get(action.config().path());
            if (!configs.containsKey(action.config().path()) && old != null) {
                configs.put(action.config().path(), old);
            }
        }
    }

    private void deleteStaleParts(Listener listener) {
        for (Path dir : List.of(modsDir, configDir)) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(dir, dir.equals(modsDir) ? 1 : 8)) {
                for (Path part : files.filter(p -> p.getFileName().toString().endsWith(FileDownloader.PART_SUFFIX)).toList()) {
                    Files.deleteIfExists(part);
                }
            } catch (IOException e) {
                listener.log("Could not clean up incomplete downloads in " + dir);
            }
        }
    }

    private static FileRecord recordOf(Path file, String verifiedSha512) throws IOException {
        return new FileRecord(verifiedSha512, Files.size(file), Files.getLastModifiedTime(file).toMillis());
    }

    private static void awaitTermination(ExecutorService pool) {
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                LOG.log(System.Logger.Level.WARNING, "Download threads did not stop in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Totals of the whole apply; reports a snapshot when a file finishes or the bytes move by 0.1 %. */
    private static final class Progress {
        private final long totalBytes;
        private final int totalFiles;
        private final Listener listener;
        private final AtomicLong bytes = new AtomicLong();
        private final AtomicInteger files = new AtomicInteger();
        private final AtomicInteger lastPermille = new AtomicInteger(-1);

        Progress(long totalBytes, int totalFiles, Listener listener) {
            this.totalBytes = totalBytes;
            this.totalFiles = totalFiles;
            this.listener = listener;
        }

        /** {@code delta} may be negative when a download restarts after a failed attempt. */
        void add(long delta) {
            long current = bytes.addAndGet(delta);
            int permille = totalBytes > 0 ? Math.clamp(current * 1000 / totalBytes, 0, 1000) : 1000;
            int last = lastPermille.get();
            if (permille != last && lastPermille.compareAndSet(last, permille)) {
                report();
            }
        }

        void fileDone() {
            files.incrementAndGet();
            report();
        }

        private void report() {
            listener.progress(new ProgressSnapshot(files.get(), totalFiles, Math.max(0, bytes.get()), totalBytes));
        }
    }

    /** One mod's download, reported in steps so a row can show its own bar without flooding the UI. */
    private static final class FileProgress {
        private final String file;
        private final long size;
        private final Listener listener;
        private final AtomicLong bytes = new AtomicLong();
        private final AtomicInteger lastStep = new AtomicInteger(-1);

        FileProgress(String file, long size, Listener listener) {
            this.file = file;
            this.size = size;
            this.listener = listener;
        }

        void add(long delta) {
            long current = bytes.addAndGet(delta);
            int percent = size > 0 ? Math.clamp(current * 100 / size, 0, 100) : 100;
            int step = percent - percent % Listener.FILE_PROGRESS_STEP;
            int last = lastStep.get();
            if (step != last && lastStep.compareAndSet(last, step)) {
                listener.modProgress(file, step);
            }
        }
    }
}
