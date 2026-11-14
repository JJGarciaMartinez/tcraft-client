package hopsync.core.pack;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.port.PackSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PackRegistryTest {
    private static final String URL = "https://example.org/manifest.json";

    @TempDir
    Path dir;
    final InMemorySettings repository = new InMemorySettings();
    final PackRegistry registry = new PackRegistry(repository);

    @Test
    void firstPackBecomesActiveAndEverythingIsSaved() throws IOException {
        Pack first = registry.add(URL, dir.resolve("a"));
        Pack second = registry.add(URL, dir.resolve("b"));

        assertEquals(first, registry.active().orElseThrow());
        assertEquals(2, repository.saved.packs().size());

        registry.select(second.id());
        assertEquals(second.id(), repository.saved.activeId());
    }

    @Test
    void sameFolderIsRejected() throws IOException {
        registry.add(URL, dir.resolve("a"));
        assertFolderRejected(dir.resolve("a"));
        assertFolderRejected(dir.resolve("b/../a"));
    }

    @Test
    void copiesOfOnePackNeedTheirOwnFolders() throws IOException {
        Pack first = registry.add(URL, dir.resolve("a"));
        Pack copy = registry.add(URL, dir.resolve("b"));

        assertEquals(2, registry.packs().size());
        SyncException e = assertThrows(SyncException.class, () -> registry.update(copy.withGameDir(first.gameDir())));
        assertInstanceOf(SyncFailure.FolderUsedByPack.class, e.failure());
        registry.update(copy.withGameDir(dir.resolve("c")));
        assertEquals(dir.resolve("c"), registry.find(copy.id()).orElseThrow().gameDir());
    }

    @Test
    void nestedFoldersAreRejectedBothWays() throws IOException {
        registry.add(URL, dir.resolve("instances/one"));
        assertFolderRejected(dir.resolve("instances/one/sub"));
        assertFolderRejected(dir.resolve("instances"));
        assertDoesNotThrow(() -> registry.add(URL, dir.resolve("instances/one-other")));
    }

    @Test
    void symlinkedFolderIsRejected() throws IOException {
        Path real = Files.createDirectories(dir.resolve("real"));
        Path link = dir.resolve("link");
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symlinks not available: " + e);
        }
        registry.add(URL, real);
        assertFolderRejected(link);
        assertFolderRejected(link.resolve("not-yet-created"));
    }

    @Test
    void tellsWhichPackUsesAFolder() throws IOException {
        Pack pack = registry.add(URL, dir.resolve("a"));

        assertEquals(pack, registry.packUsing(dir.resolve("a/sub"), null).orElseThrow());
        assertTrue(registry.packUsing(dir.resolve("a"), pack.id()).isEmpty(), "a pack may keep its own folder");
        assertTrue(registry.packUsing(dir.resolve("b"), null).isEmpty());
    }

    @Test
    void preferencesAreSavedAndClamped() throws IOException {
        registry.updatePreferences(false, false, 40);

        assertEquals(new PackSettings(List.of(), null, false, false, 8), repository.saved);
    }

    @Test
    void updatingAPackMayKeepItsOwnFolder() throws IOException {
        Pack pack = registry.add(URL, dir.resolve("a"));
        registry.update(pack.withManifestUrl("https://example.org/other.json"));
        assertEquals("https://example.org/other.json", registry.find(pack.id()).orElseThrow().manifestUrl());
    }

    @Test
    void removingAPackLeavesItsFilesAndSelectsAnother() throws IOException {
        Path folder = Files.createDirectories(dir.resolve("a/mods"));
        Files.writeString(folder.resolve("x.jar"), "jar");
        Pack removed = registry.add(URL, dir.resolve("a"));
        Pack kept = registry.add(URL, dir.resolve("b"));

        registry.remove(removed.id());

        assertEquals(kept, registry.active().orElseThrow());
        assertTrue(Files.exists(folder.resolve("x.jar")));
    }

    @Test
    void rememberedNameIsSavedOnlyWhenItChanges() throws IOException {
        Pack pack = registry.add(URL, dir.resolve("a"));
        registry.rememberName(pack.id(), "Demo");
        assertEquals("Demo", repository.saved.find(pack.id()).orElseThrow().name());

        int saves = repository.saves;
        registry.rememberName(pack.id(), "Demo");
        assertEquals(saves, repository.saves);
    }

    private void assertFolderRejected(Path folder) {
        SyncException e = assertThrows(SyncException.class, () -> registry.add(URL, folder));
        assertInstanceOf(SyncFailure.FolderUsedByPack.class, e.failure());
    }

    static final class InMemorySettings implements PackSettingsRepository {
        PackSettings saved = PackSettings.EMPTY;
        int saves;

        @Override
        public PackSettings load() {
            return saved;
        }

        @Override
        public void save(PackSettings settings) {
            saved = settings;
            saves++;
        }
    }
}
