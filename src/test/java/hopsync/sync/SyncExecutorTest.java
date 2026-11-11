package hopsync.sync;

import hopsync.model.ModEntry;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static hopsync.sync.Fixtures.config;
import static hopsync.sync.Fixtures.manifest;
import static hopsync.sync.Fixtures.mod;
import static hopsync.sync.Fixtures.recordOf;
import static hopsync.sync.Fixtures.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        if (cancelled.getAsBoolean()) throw new CancellationException();
        if (failing.contains(url)) throw new IOException("boom");
        String content = remote.get(url);
        Files.writeString(destination, content, StandardCharsets.UTF_8);
        onBytes.accept(content.getBytes(StandardCharsets.UTF_8).length);
    };

    final SyncExecutor.Listener listener = new SyncExecutor.Listener() {
        public void modStarted(String file) { events.add("start " + file); }
        public void modFinished(String file, boolean success, String error) { events.add((success ? "ok " : "fail ") + file); }
        public void modRemoved(String file) { events.add("removed " + file); }
        public void progress(int percent) { events.add("progress " + percent); }
        public void log(String message) { }
    };

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

    private SyncExecutor.Result run(SyncPlan plan, LocalState previous, java.util.function.BooleanSupplier cancel) {
        return new SyncExecutor(fakeDownloader, mods, configs, 3).execute(plan, previous, listener, cancel);
    }

    private SyncPlan plan(hopsync.model.Manifest manifest, LocalState state) throws IOException {
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

        SyncExecutor.Result result = run(plan(manifest, previous), previous, () -> false);

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
        assertEquals(Set.of("sub/c.toml"), state.configs().keySet());
        assertTrue(events.contains("removed removed.jar"));
        assertTrue(events.contains("progress 100"));
    }

    @Test
    void failedDownloadKeepsOldFileAndRecordAndPreviousContentHash() throws IOException {
        FileRecord old = recordOf(write(mods, "a.jar", "old"));
        LocalState previous = new LocalState("previous", Map.of("a.jar", old), Map.of());
        ModEntry a = served("a.jar", "new");
        ModEntry b = served("b.jar", "B");
        failing.add(a.url());

        SyncExecutor.Result result = run(plan(manifest(List.of(a, b), List.of()), previous), previous, () -> false);

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

        SyncExecutor.Result result = run(plan(manifest, previous), previous, () -> true);

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
        AtomicBoolean cancel = new AtomicBoolean();
        FileDownloader cancelAfterTwo = (url, destination, sha512, size, onBytes, cancelled) -> {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            fakeDownloader.download(url, destination, sha512, size, onBytes, cancelled);
            if (downloads.incrementAndGet() >= 2) cancel.set(true);
        };

        SyncExecutor.Result result = new SyncExecutor(cancelAfterTwo, mods, configs, 1)
            .execute(plan(manifest(entries, List.of()), LocalState.empty()), LocalState.empty(), listener, cancel::get);

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
            previous, () -> false);

        assertEquals("player edit", Files.readString(file));
        assertEquals(installed, result.state().configs().get("a.toml"));
    }

    @Test
    void deletesStalePartialDownloads() throws IOException {
        write(mods, "x.jar" + FileDownloader.PART_SUFFIX, "partial");
        write(configs, "deep/y.toml" + FileDownloader.PART_SUFFIX, "partial");
        write(mods, "unrelated.part", "not ours");

        run(plan(manifest(List.of(), List.of()), LocalState.empty()), LocalState.empty(), () -> false);

        assertFalse(Files.exists(mods.resolve("x.jar" + FileDownloader.PART_SUFFIX)));
        assertFalse(Files.exists(configs.resolve("deep/y.toml" + FileDownloader.PART_SUFFIX)));
        assertTrue(Files.exists(mods.resolve("unrelated.part")));
    }
}
