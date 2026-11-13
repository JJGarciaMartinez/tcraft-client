package hopsync.platform.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.platform.http.DownloadService;
import hopsync.platform.http.NetworkConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestServiceTest {
    private final ManifestService manifests = new ManifestService(new DownloadService(NetworkConfig.DEFAULT));

    @Test
    void manifestUrlMustBeHttps() {
        SyncException e = assertThrows(SyncException.class, () -> manifests.fetchManifest("http://example.org/m.json"));
        assertEquals(new SyncFailure.InvalidUrl("http://example.org/m.json"), e.failure());
    }

    @Test
    void missingUrlMeansNoPack() {
        SyncException e = assertThrows(SyncException.class, () -> manifests.fetchManifest(""));
        assertEquals(new SyncFailure.NotConfigured(), e.failure());
    }
}
