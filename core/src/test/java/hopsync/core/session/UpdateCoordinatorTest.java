package hopsync.core.session;

import hopsync.core.event.EventBus;
import hopsync.core.event.UpdateEvent;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.port.GameFolders;
import hopsync.core.sync.FileDownloader;
import hopsync.core.sync.FolderLock;
import hopsync.core.usecase.ModUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static hopsync.core.sync.Fixtures.manifest;
import static hopsync.core.sync.Fixtures.mod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The whole update lifecycle against fake ports: no network, a temporary game folder.
 */
class UpdateCoordinatorTest {

    @TempDir
    Path game;
    final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    final BlockingQueue<UpdateEvent> events = new LinkedBlockingQueue<>();
    final EventBus bus = new EventBus();
    final Map<String, String> remote = new java.util.concurrent.ConcurrentHashMap<>();
    Manifest served = manifest(List.of(), List.of());
    FileDownloader downloader = (url, destination, sha512, size, onBytes, cancel) -> {
        Files.writeString(destination, remote.get(url), StandardCharsets.UTF_8);
        onBytes.accept(size);
    };
    UpdateCoordinator coordinator;

    final GameFolders folders = new GameFolders() {
        public File getModsFolder() { return game.resolve("mods").toFile(); }
        public File getConfigFolder() { return game.resolve("config").toFile(); }
        public File getStateFile() { return game.resolve(".hopsync/state.json").toFile(); }
    };

    @BeforeEach
    void setUp() {
        bus.subscribe(events::add);
        coordinator = new UpdateCoordinator(bus,
            new ModUpdateService(folders, () -> served, (url, d, sha, size, onBytes, cancel) ->
                downloader.download(url, d, sha, size, onBytes, cancel)),
            executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ModEntry serve(String file, String content) {
        ModEntry entry = mod(file, content);
        remote.put(entry.url(), content);
        return entry;
    }

    /** Waits for the next event of {@code type}, skipping logs and progress. */
    private <T extends UpdateEvent> T next(Class<T> type) throws InterruptedException {
        while (true) {
            UpdateEvent event = events.poll(5, TimeUnit.SECONDS);
            if (event == null) fail("No " + type.getSimpleName() + " event");
            if (type.isInstance(event)) return type.cast(event);
            if (event instanceof UpdateEvent.UpdateFailed failed) fail("Update failed: " + failed.failure().describe());
        }
    }

    @Test
    void emptyPlanIsUpToDate() throws InterruptedException {
        coordinator.startUpdate();

        next(UpdateEvent.UpToDate.class);
        assertEquals(UpdateState.Phase.COMPLETED, coordinator.getPhase());
        assertTrue(Files.exists(game.resolve(".hopsync/state.json")));
    }

    @Test
    void appliesTheConfirmedPlan() throws InterruptedException, IOException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());

        coordinator.startUpdate();
        next(UpdateEvent.PlanReady.class);
        assertFalse(Files.exists(game.resolve("mods/a.jar")), "nothing is written before confirming");
        coordinator.confirmPlan();

        UpdateEvent.UpdateCompleted done = next(UpdateEvent.UpdateCompleted.class);
        assertEquals(1, done.successCount());
        assertEquals(0, done.failCount());
        assertEquals("A", Files.readString(game.resolve("mods/a.jar")));
        assertFalse(coordinator.isRunning());
    }

    @Test
    void cancellingTheReviewWritesNothing() throws InterruptedException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());

        coordinator.startUpdate();
        next(UpdateEvent.PlanReady.class);
        coordinator.cancelUpdate();

        next(UpdateEvent.UpdateCancelled.class);
        assertEquals(UpdateState.Phase.CANCELLED, coordinator.getPhase());
        assertFalse(Files.exists(game.resolve("mods/a.jar")));
    }

    @Test
    void cancellingWhileDownloadingStopsTheDownload() throws InterruptedException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());
        CountDownLatch downloading = new CountDownLatch(1);
        downloader = (url, destination, sha512, size, onBytes, cancel) -> {
            CountDownLatch cancelled = new CountDownLatch(1);
            cancel.onCancel(cancelled::countDown);
            downloading.countDown();
            try {
                if (!cancelled.await(5, TimeUnit.SECONDS)) throw new IOException("never cancelled");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new CancellationException();
        };

        coordinator.startUpdate();
        next(UpdateEvent.PlanReady.class);
        coordinator.confirmPlan();
        assertTrue(downloading.await(5, TimeUnit.SECONDS));
        coordinator.cancelUpdate();

        next(UpdateEvent.UpdateCancelled.class);
        assertFalse(Files.exists(game.resolve("mods/a.jar")));
    }

    @Test
    void failuresArriveTyped() throws InterruptedException {
        coordinator = new UpdateCoordinator(bus, new ModUpdateService(folders, () -> {
            throw new SyncException(new SyncFailure.Network("example.org", "timeout"));
        }, downloader), executor);

        coordinator.startUpdate();

        UpdateEvent.UpdateFailed failed = awaitFailure();
        assertEquals(new SyncFailure.Network("example.org", "timeout"), failed.failure());
        assertEquals(UpdateState.Phase.FAILED, coordinator.getPhase());
    }

    @Test
    void anotherProcessApplyingToTheFolderIsReported() throws Exception {
        served = manifest(List.of(serve("a.jar", "A")), List.of());
        coordinator.startUpdate();
        next(UpdateEvent.PlanReady.class);

        FolderLock other = FolderLock.acquire(game.resolve(".hopsync/apply.lock"), game);
        try {
            coordinator.confirmPlan();
            assertInstanceOf(SyncFailure.FolderInUse.class, awaitFailure().failure());
        } finally {
            other.close();
        }
        assertFalse(Files.exists(game.resolve("mods/a.jar")));
    }

    private UpdateEvent.UpdateFailed awaitFailure() throws InterruptedException {
        while (true) {
            UpdateEvent event = events.poll(5, TimeUnit.SECONDS);
            assertNotNull(event, "No UpdateFailed event");
            if (event instanceof UpdateEvent.UpdateFailed failed) return failed;
        }
    }
}
