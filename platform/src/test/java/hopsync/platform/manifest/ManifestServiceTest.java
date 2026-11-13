package hopsync.platform.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.platform.http.DownloadService;
import hopsync.platform.http.NetworkConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestServiceTest {
    private static final String URL = "https://example.org/pack/manifest.json";
    private static final String MANIFEST = """
        {"format_version": 2,
         "pack": {"name": "Pack", "version": "1.0.0", "minecraft": "1.21.1", "loader": "neoforge"},
         "content_hash": "%s",
         "mods": [{"id": "m", "name": "Mod", "version": "1.0", "author": "me", "description": "d",
                   "file": "mod.jar", "url": "https://cdn.modrinth.com/data/x/mod.jar", "sha512": "%s",
                   "size": 10, "source": "modrinth"}],
         "configs": []}
        """.formatted("b".repeat(64), "a".repeat(128));

    @TempDir
    Path dir;

    private ManifestService manifests() {
        return new ManifestService(new DownloadService(NetworkConfig.DEFAULT), dir.resolve("manifests"));
    }

    @Test
    void manifestUrlMustBeHttps() {
        SyncException e = assertThrows(SyncException.class, () -> manifests().fetchManifest("http://example.org/m.json"));
        assertEquals(new SyncFailure.InvalidUrl("http://example.org/m.json"), e.failure());
    }

    @Test
    void missingUrlMeansNoPack() {
        SyncException e = assertThrows(SyncException.class, () -> manifests().fetchManifest(""));
        assertEquals(new SyncFailure.NotConfigured(), e.failure());
    }

    @Test
    void cachedManifestIsReadBackAndCanBeForgotten() throws IOException {
        ManifestService manifests = manifests();
        assertEquals(Optional.empty(), manifests.cached(URL));

        Path file = manifests.cacheFile(URL);
        Files.createDirectories(file.getParent());
        Files.writeString(file, MANIFEST);
        Manifest cached = manifests.cached(URL).orElseThrow();
        assertEquals("Pack", cached.pack().name());
        assertEquals(1, cached.mods().size());

        manifests.forget(URL);
        assertFalse(Files.exists(file));
    }

    @Test
    void brokenCacheIsIgnored() throws IOException {
        ManifestService manifests = manifests();
        Path file = manifests.cacheFile(URL);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{not json");

        assertTrue(manifests.cached(URL).isEmpty());
    }

    @Test
    void eachLinkHasItsOwnSafeFileName() {
        ManifestService manifests = manifests();
        Path a = manifests.cacheFile(URL);
        Path b = manifests.cacheFile("https://example.org/other/../../etc/manifest.json");

        assertNotEquals(a, b);
        assertEquals(dir.resolve("manifests"), b.getParent());
        assertTrue(b.getFileName().toString().matches("[0-9a-f]{32}\\.json"));
    }
}
