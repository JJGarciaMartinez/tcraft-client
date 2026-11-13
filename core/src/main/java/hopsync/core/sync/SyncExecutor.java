package hopsync.core.sync;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.ModEntry;
import hopsync.core.sync.SyncPlan.ConfigAction;
import hopsync.core.sync.SyncPlan.ModAction;
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

    /** Progress callbacks; may be invoked from worker threads. */
    public interface Listener {
        void modStarted(String file);

        void modFinished(String file, boolean success, String error);

        void modRemoved(String file);

        /** Called when the overall percentage changes. */
        void progress(int percent);

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

    public Result execute(SyncPlan plan, LocalState previous, Listener listener, BooleanSupplier cancelRequested) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        BooleanSupplier isCancelled = () -> cancelled.get() || cancelRequested.getAsBoolean();
        deleteStaleParts(listener);

        Map<String, FileRecord> mods = new ConcurrentHashMap<>(plan.keptMods());
        Map<String, FileRecord> configs = new ConcurrentHashMap<>(plan.keptConfigs());
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        Progress progress = new Progress(plan.downloadBytes(), listener);

        downloadMods(plan.modDownloads(), mods, succeeded, failed, progress, listener, isCancelled, cancelled);

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

        removeMods(plan.removals(), previous, mods, listener);
        installConfigs(plan.configs(), previous, configs, failed, progress, listener, isCancelled);

        boolean complete = failed.get() == 0 && !isCancelled.getAsBoolean();
        String contentHash = complete ? plan.manifest().contentHash() : previous.contentHash();
        return new Result(new LocalState(contentHash, mods, configs), succeeded.get(), failed.get(),
            isCancelled.getAsBoolean());
    }

    private void downloadMods(List<ModAction> downloads, Map<String, FileRecord> mods, AtomicInteger succeeded,
                              AtomicInteger failed, Progress progress, Listener listener,
                              BooleanSupplier isCancelled, AtomicBoolean cancelled) {
        if (downloads.isEmpty()) {
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(parallelism, downloads.size()));
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
                        downloader.download(mod.url(), target, mod.sha512(), mod.size(), progress::add, isCancelled);
                        mods.put(mod.file(), recordOf(target, mod.sha512()));
                        succeeded.incrementAndGet();
                        listener.modFinished(mod.file(), true, null);
                    } catch (CancellationException e) {
                        cancelled.set(true);
                    } catch (IOException | RuntimeException e) {
                        failed.incrementAndGet();
                        listener.modFinished(mod.file(), false, e.getMessage());
                        listener.log("Error en " + mod.file() + ": " + e.getMessage());
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
                            Listener listener) {
        for (String file : removals) {
            try {
                Files.deleteIfExists(SafePaths.resolveInside(modsDir, file));
                listener.modRemoved(file);
                listener.log("Eliminado: " + file);
            } catch (IOException e) {
                mods.put(file, previous.mods().get(file));
                listener.log("Advertencia: no se pudo eliminar " + file + ": " + e.getMessage());
            }
        }
    }

    private void installConfigs(List<ConfigAction> actions, LocalState previous, Map<String, FileRecord> configs,
                                AtomicInteger failed, Progress progress, Listener listener,
                                BooleanSupplier isCancelled) {
        for (ConfigAction action : actions) {
            ConfigEntry config = action.config();
            FileRecord old = previous.configs().get(config.path());
            switch (action.change()) {
                case UP_TO_DATE -> { }
                case KEEP_EDITED -> {
                    // Remember the version HopSync installed, so later updates still see the player's edit
                    if (old != null) configs.put(config.path(), old);
                    listener.log("Config conservada (modificada por ti): " + config.path());
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
                            isCancelled);
                        configs.put(config.path(), recordOf(target, config.sha512()));
                        listener.log("Config instalada: " + config.path());
                    } catch (CancellationException e) {
                        if (old != null) configs.put(config.path(), old);
                    } catch (IOException | RuntimeException e) {
                        failed.incrementAndGet();
                        if (old != null) configs.put(config.path(), old);
                        listener.log("Advertencia: no se pudo instalar " + config.path() + ": " + e.getMessage());
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
                listener.log("Advertencia: no se pudieron limpiar descargas incompletas en " + dir);
            }
        }
    }

    private static FileRecord recordOf(Path file, String verifiedSha512) throws IOException {
        return new FileRecord(verifiedSha512, Files.size(file), Files.getLastModifiedTime(file).toMillis());
    }

    private static void awaitTermination(ExecutorService pool) {
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Warning: download threads did not stop in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Progress {
        private final long total;
        private final Listener listener;
        private final AtomicLong done = new AtomicLong();
        private final AtomicInteger lastPercent = new AtomicInteger(-1);

        Progress(long total, Listener listener) {
            this.total = total;
            this.listener = listener;
        }

        void add(long bytes) {
            long current = done.addAndGet(bytes);
            int percent = total > 0 ? (int) Math.min(100, current * 100 / total) : 100;
            int last = lastPercent.get();
            if (percent != last && lastPercent.compareAndSet(last, percent)) {
                listener.progress(percent);
            }
        }
    }
}
