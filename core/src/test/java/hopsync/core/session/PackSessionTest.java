package hopsync.core.session;

import hopsync.core.event.EventBus;
import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.core.pack.PackSettings;
import hopsync.core.port.ManifestSource;
import hopsync.core.port.PackSettingsRepository;
import hopsync.core.sync.FileDownloader;
import hopsync.core.sync.FolderLock;
import hopsync.core.usecase.ModUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * The whole update lifecycle of a pack against fake ports: no network, temporary game folders.
 */
class PackSessionTest {

    @TempDir
    Path game;
    final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    final BlockingQueue<SyncEvent> events = new LinkedBlockingQueue<>();
    final List<SyncEvent> all = new CopyOnWriteArrayList<>();
    final EventBus bus = new EventBus();
    final Map<String, String> remote = new java.util.concurrent.ConcurrentHashMap<>();
    Manifest served = manifest(List.of(), List.of());
    FileDownloader downloader = (url, destination, sha512, size, onBytes, cancel) -> {
        Files.writeString(destination, remote.get(url), StandardCharsets.UTF_8);
        onBytes.accept(size);
    };
    PackRegistry registry;
    ApplyQueue queue;
    Pack pack;
    PackSession coordinator;

    @BeforeEach
    void setUp() throws IOException {
        bus.subscribe(events::add);
        bus.subscribe(all::add);
        registry = new PackRegistry(new InMemorySettings());
        queue = new ApplyQueue(executor, () -> true);
        pack = registry.add("https://example.org/manifest.json", game);
        coordinator = session(pack, service(url -> served));
    }

    private ModUpdateService service(ManifestSource manifests) {
        return new ModUpdateService(manifests, (url, d, sha, size, onBytes, cancel) ->
            downloader.download(url, d, sha, size, onBytes, cancel), () -> 4);
    }

    private PackSession session(Pack target, ModUpdateService service) {
        return new PackSession(target.id(), registry, service, queue, bus::publish, executor);
    }

    static final class InMemorySettings implements PackSettingsRepository {
        PackSettings saved = PackSettings.EMPTY;

        @Override
        public PackSettings load() {
            return saved;
        }

        @Override
        public void save(PackSettings settings) {
            saved = settings;
        }
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
    private <T extends SyncEvent> T next(Class<T> type) throws InterruptedException {
        while (true) {
            SyncEvent event = events.poll(5, TimeUnit.SECONDS);
            if (event == null) fail("No " + type.getSimpleName() + " event");
            if (type.isInstance(event)) return type.cast(event);
            if (event instanceof SyncEvent.UpdateFailed failed) fail("Update failed: " + failed.failure().describe());
        }
    }

    @Test
    void emptyPlanIsUpToDate() throws InterruptedException {
        coordinator.startUpdate();

        next(SyncEvent.UpToDate.class);
        assertEquals(UpdateState.Phase.COMPLETED, coordinator.getPhase());
        assertTrue(Files.exists(game.resolve(".hopsync/state.json")));
    }

    @Test
    void appliesTheConfirmedPlan() throws InterruptedException, IOException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());

        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);
        assertFalse(Files.exists(game.resolve("mods/a.jar")), "nothing is written before confirming");
        coordinator.confirmPlan();

        SyncEvent.UpdateCompleted done = next(SyncEvent.UpdateCompleted.class);
        assertEquals(1, done.successCount());
        assertEquals(0, done.failCount());
        assertEquals("A", Files.readString(game.resolve("mods/a.jar")));
        assertFalse(coordinator.isRunning());
    }

    @Test
    void cancellingTheReviewWritesNothing() throws InterruptedException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());

        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);
        coordinator.cancelUpdate();

        next(SyncEvent.UpdateCancelled.class);
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
        next(SyncEvent.PlanReady.class);
        coordinator.confirmPlan();
        assertTrue(downloading.await(5, TimeUnit.SECONDS));
        coordinator.cancelUpdate();

        next(SyncEvent.UpdateCancelled.class);
        assertFalse(Files.exists(game.resolve("mods/a.jar")));
    }

    @Test
    void failuresArriveTyped() throws InterruptedException {
        coordinator = session(pack, service(url -> {
            throw new SyncException(new SyncFailure.Network("example.org", "timeout"));
        }));

        coordinator.startUpdate();

        SyncEvent.UpdateFailed failed = awaitFailure();
        assertEquals(new SyncFailure.Network("example.org", "timeout"), failed.failure());
        assertEquals(UpdateState.Phase.FAILED, coordinator.getPhase());
    }

    @Test
    void anotherProcessApplyingToTheFolderIsReported() throws Exception {
        served = manifest(List.of(serve("a.jar", "A")), List.of());
        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);

        FolderLock other = FolderLock.acquire(game.resolve(".hopsync/apply.lock"), game);
        try {
            coordinator.confirmPlan();
            assertInstanceOf(SyncFailure.FolderInUse.class, awaitFailure().failure());
        } finally {
            other.close();
        }
        assertFalse(Files.exists(game.resolve("mods/a.jar")));
    }

    @Test
    void everyEventNamesItsPackAndTheNameIsRemembered() throws InterruptedException {
        served = manifest(List.of(serve("a.jar", "A")), List.of());

        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);
        coordinator.confirmPlan();
        next(SyncEvent.UpdateCompleted.class);

        assertTrue(all.stream().allMatch(event -> event.packId().equals(pack.id())));
        assertEquals(served.pack().name(), registry.find(pack.id()).orElseThrow().name());
    }

    @Test
    void secondPackWaitsForTheFirstToFinishApplying(@TempDir Path otherGame) throws Exception {
        served = manifest(List.of(serve("a.jar", "A")), List.of());
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstDownloading = new CountDownLatch(1);
        downloader = (url, destination, sha512, size, onBytes, cancel) -> {
            if (destination.startsWith(game)) {
                firstDownloading.countDown();
                await(release);
            }
            Files.writeString(destination, remote.get(url), StandardCharsets.UTF_8);
            onBytes.accept(size);
        };
        Pack other = registry.add("https://example.org/other.json", otherGame);
        PackSession second = session(other, service(url -> served));

        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);
        coordinator.confirmPlan();
        assertTrue(firstDownloading.await(5, TimeUnit.SECONDS));

        second.startUpdate();
        assertEquals(other.id(), next(SyncEvent.PlanReady.class).packId());
        second.confirmPlan();
        assertEquals(other.id(), next(SyncEvent.Queued.class).packId());
        assertEquals(UpdateState.Phase.QUEUED, second.getPhase());
        assertFalse(Files.exists(otherGame.resolve("mods/a.jar")), "the queued pack writes nothing yet");

        release.countDown();
        assertEquals(pack.id(), next(SyncEvent.UpdateCompleted.class).packId());
        assertEquals(other.id(), next(SyncEvent.ApplyStarted.class).packId());
        assertEquals(other.id(), next(SyncEvent.UpdateCompleted.class).packId());
        assertEquals("A", Files.readString(otherGame.resolve("mods/a.jar")));
    }

    @Test
    void cancellingAQueuedPackTakesItOutOfTheQueue(@TempDir Path otherGame) throws Exception {
        served = manifest(List.of(serve("a.jar", "A")), List.of());
        CountDownLatch release = new CountDownLatch(1);
        downloader = (url, destination, sha512, size, onBytes, cancel) -> {
            await(release);
            Files.writeString(destination, remote.get(url), StandardCharsets.UTF_8);
            onBytes.accept(size);
        };
        PackSession second = session(registry.add("https://example.org/other.json", otherGame), service(url -> served));

        coordinator.startUpdate();
        next(SyncEvent.PlanReady.class);
        coordinator.confirmPlan();
        next(SyncEvent.ApplyStarted.class);
        second.startUpdate();
        next(SyncEvent.PlanReady.class);
        second.confirmPlan();
        next(SyncEvent.Queued.class);

        second.cancelUpdate();
        assertEquals(second.packId(), next(SyncEvent.UpdateCancelled.class).packId());
        assertEquals(UpdateState.Phase.CANCELLED, second.getPhase());

        release.countDown();
        next(SyncEvent.UpdateCompleted.class);
        assertFalse(Files.exists(otherGame.resolve("mods/a.jar")));
    }

    /** Blocks a fake download until the test releases it. */
    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("never released");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private SyncEvent.UpdateFailed awaitFailure() throws InterruptedException {
        while (true) {
            SyncEvent event = events.poll(5, TimeUnit.SECONDS);
            assertNotNull(event, "No UpdateFailed event");
            if (event instanceof SyncEvent.UpdateFailed failed) return failed;
        }
    }
}
