package hopsync.core.sync;

import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.ModEntry;
import hopsync.core.util.CancellationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static hopsync.core.sync.Fixtures.config;
import static hopsync.core.sync.Fixtures.manifest;
import static hopsync.core.sync.Fixtures.mod;
import static hopsync.core.sync.Fixtures.recordOf;
import static hopsync.core.sync.Fixtures.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncExecutorTest {

    @TempDir
    Path root;
    Path mods;
    Path configs;

    /** Serves contents by URL; URLs in {@link #failing} throw instead. */
    final Map<String, String> remote = new HashMap<>();
    final Set<String> failing = ConcurrentHashMap.newKeySet();
    final List<String> events = Collections.synchronizedList(new ArrayList<>());

    final FileDownloader fakeDownloader = (url, destination, sha512, size, onBytes, cancelled) -> {
        if (cancelled.isCancelled()) throw new CancellationException();
        if (failing.contains(url)) throw new IOException("boom");
        String content = remote.get(url);
        Files.writeString(destination, content, StandardCharsets.UTF_8);
        onBytes.accept(content.getBytes(StandardCharsets.UTF_8).length);
    };

    final List<ProgressSnapshot> snapshots = new CopyOnWriteArrayList<>();
    final Map<String, SyncFailure> failures = new ConcurrentHashMap<>();

    final SyncExecutor.Listener listener = new SyncExecutor.Listener() {
        public void modStarted(String file) { events.add("start " + file); }
        public void modFinished(String file, boolean success, SyncFailure failure) {
            events.add((success ? "ok " : "fail ") + file);
            if (failure != null) failures.put(file, failure);
        }
        public void modRemoved(String file) { events.add("removed " + file); }
        public void progress(ProgressSnapshot snapshot) {
            snapshots.add(snapshot);
            events.add("progress " + snapshot.percent());
        }
        public void modProgress(String file, int percent) { events.add("file " + file + " " + percent); }
        public void log(String message) { }
    };

    private static CancellationToken cancelled() {
        CancellationToken token = new CancellationToken();
        token.cancel();
        return token;
    }

    @BeforeEach
    void setUp() throws IOException {
        mods = Files.createDirectories(root.resolve("mods"));
        configs = Files.createDirectories(root.resolve("config"));
    }

    private ModEntry served(String file, String content) {
        ModEntry entry = mod(file, content);
        remote.put(entry.url(), content);
        return entry;
    }

    private SyncExecutor.Result run(SyncPlan plan, LocalState previous, CancellationToken cancel) {
        return new SyncExecutor(fakeDownloader, mods, configs, 3).execute(plan, previous, listener, cancel);
    }

    private SyncPlan plan(hopsync.core.manifest.Manifest manifest, LocalState state) throws IOException {
        return new SyncPlanner(mods, configs).plan(manifest, state);
    }

    @Test
    void appliesDownloadsRemovalsAndConfigs() throws IOException {
        write(mods, "keep.jar", "K");
        write(mods, "upd.jar", "old");
        FileRecord removed = recordOf(write(mods, "removed.jar", "R"));
        write(mods, "personal.jar", "P");
        LocalState previous = new LocalState("previous", Map.of("removed.jar", removed), Map.of());
        var manifest = manifest(
            List.of(served("keep.jar", "K"), served("upd.jar", "new"), served("new.jar", "N")),
            List.of(config("sub/c.toml", "C", false)));
        remote.put(manifest.configs().get(0).url(), "C");

        SyncExecutor.Result result = run(plan(manifest, previous), previous, CancellationToken.none());

        assertFalse(result.cancelled());
        assertEquals(2, result.succeeded());
        assertEquals(0, result.failed());
        assertEquals("new", Files.readString(mods.resolve("upd.jar")));
        assertEquals("N", Files.readString(mods.resolve("new.jar")));
        assertFalse(Files.exists(mods.resolve("removed.jar")));
        assertEquals("P", Files.readString(mods.resolve("personal.jar")));
        assertEquals("C", Files.readString(configs.resolve("sub/c.toml")));

        LocalState state = result.state();
        assertEquals(Fixtures.CONTENT_HASH, state.contentHash());
        assertEquals(Set.of("keep.jar", "upd.jar", "new.jar"), state.mods().keySet());
        assertEquals(Fixtures.sha512("new"), state.mods().get("upd.jar").sha512());
        assertEquals("1.0", state.mods().get("new.jar").version(), "installed files record their version");
        assertEquals(Set.of("sub/c.toml"), state.configs().keySet());
        assertTrue(events.contains("removed removed.jar"));
        assertTrue(events.contains("progress 100"));
    }

    @Test
    void progressSnapshotsAddUpToEveryFileAndByte() throws IOException {
        write(mods, "upd.jar", "old");
        FileRecord removed = recordOf(write(mods, "removed.jar", "R"));
        LocalState previous = new LocalState("previous", Map.of("removed.jar", removed), Map.of());
        var manifest = manifest(List.of(served("upd.jar", "new"), served("new.jar", "NN")),
            List.of(config("c.toml", "CCCC", false)));
        remote.put(manifest.configs().get(0).url(), "CCCC");
        SyncPlan plan = plan(manifest, previous);

        run(plan, previous, CancellationToken.none());

        ProgressSnapshot last = snapshots.getLast();
        assertEquals(new ProgressSnapshot(4, 4, plan.downloadBytes(), plan.downloadBytes()), last,
            "2 downloads + 1 removal + 1 config, every byte counted");
        for (int i = 1; i < snapshots.size(); i++) {
            assertTrue(snapshots.get(i).doneFiles() >= snapshots.get(i - 1).doneFiles(), "files never go back");
        }
        assertTrue(events.contains("file new.jar 100"), "each mod reports its own progress");
    }

    @Test
    void failureReasonReachesTheListener() throws IOException {
        var manifest = manifest(List.of(served("a.jar", "A")), List.of());
        failing.add(manifest.mods().get(0).url());

        SyncExecutor.Result result = run(plan(manifest, LocalState.empty()), LocalState.empty(), CancellationToken.none());

        assertEquals(1, result.failed());
        assertNotNull(failures.get("a.jar"));
        assertEquals(1, snapshots.getLast().doneFiles(), "a failed file still counts as processed");
    }

    @Test
    void failedDownloadKeepsOldFileAndRecordAndPreviousContentHash() throws IOException {
        FileRecord old = recordOf(write(mods, "a.jar", "old"));
        LocalState previous = new LocalState("previous", Map.of("a.jar", old), Map.of());
        ModEntry a = served("a.jar", "new");
        ModEntry b = served("b.jar", "B");
        failing.add(a.url());

        SyncExecutor.Result result = run(plan(manifest(List.of(a, b), List.of()), previous), previous, CancellationToken.none());

        assertEquals(1, result.failed());
        assertEquals(1, result.succeeded());
        assertEquals("old", Files.readString(mods.resolve("a.jar")));
        assertEquals(old, result.state().mods().get("a.jar"));
        assertEquals("previous", result.state().contentHash());
        assertTrue(events.contains("fail a.jar"));
    }

    @Test
    void cancellationSkipsRemovalsAndConfigsAndKeepsTheirRecords() throws IOException {
        FileRecord removable = recordOf(write(mods, "removable.jar", "R"));
        LocalState previous = new LocalState(null, Map.of("removable.jar", removable), Map.of());
        var manifest = manifest(List.of(served("a.jar", "A")), List.of(config("c.toml", "C", true)));

        SyncExecutor.Result result = run(plan(manifest, previous), previous, cancelled());

        assertTrue(result.cancelled());
        assertTrue(Files.exists(mods.resolve("removable.jar")));
        assertFalse(Files.exists(configs.resolve("c.toml")));
        assertEquals(removable, result.state().mods().get("removable.jar"));
        assertNull(result.state().contentHash());
    }

    @Test
    void cancellingMidwayStopsRemainingDownloads() throws IOException {
        List<ModEntry> entries = new ArrayList<>();
        for (int i = 0; i < 20; i++) entries.add(served("m" + i + ".jar", "content " + i));
        AtomicInteger downloads = new AtomicInteger();
        CancellationToken cancel = new CancellationToken();
        FileDownloader cancelAfterTwo = (url, destination, sha512, size, onBytes, cancelled) -> {
            if (cancelled.isCancelled()) throw new CancellationException();
            fakeDownloader.download(url, destination, sha512, size, onBytes, cancelled);
            if (downloads.incrementAndGet() >= 2) cancel.cancel();
        };

        SyncExecutor.Result result = new SyncExecutor(cancelAfterTwo, mods, configs, 1)
            .execute(plan(manifest(entries, List.of()), LocalState.empty()), LocalState.empty(), listener, cancel);

        assertTrue(result.cancelled());
        assertEquals(2, result.succeeded());
        assertEquals(2, result.state().mods().size());
    }

    @Test
    void keepsRecordOfEditedConfig() throws IOException {
        Path file = write(configs, "a.toml", "v1");
        FileRecord installed = recordOf(file);
        Files.writeString(file, "player edit");
        LocalState previous = new LocalState(null, Map.of(), Map.of("a.toml", installed));

        SyncExecutor.Result result = run(plan(manifest(List.of(), List.of(config("a.toml", "v2", false))), previous),
            previous, CancellationToken.none());

        assertEquals("player edit", Files.readString(file));
        assertEquals(installed, result.state().configs().get("a.toml"));
    }

    @Test
    void deletesStalePartialDownloads() throws IOException {
        write(mods, "x.jar" + FileDownloader.PART_SUFFIX, "partial");
        write(configs, "deep/y.toml" + FileDownloader.PART_SUFFIX, "partial");
        write(mods, "unrelated.part", "not ours");

        run(plan(manifest(List.of(), List.of()), LocalState.empty()), LocalState.empty(), CancellationToken.none());

        assertFalse(Files.exists(mods.resolve("x.jar" + FileDownloader.PART_SUFFIX)));
        assertFalse(Files.exists(configs.resolve("deep/y.toml" + FileDownloader.PART_SUFFIX)));
        assertTrue(Files.exists(mods.resolve("unrelated.part")));
    }
}
