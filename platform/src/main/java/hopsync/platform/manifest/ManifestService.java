package hopsync.platform.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ManifestParser;
import hopsync.core.port.ManifestSource;
import hopsync.platform.http.DownloadService;

import java.io.IOException;

/**
 * Downloads and validates the modpack manifest.
 */
public class ManifestService implements ManifestSource {
    private final DownloadService downloadService;

    public ManifestService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Override
    public Manifest fetchManifest(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new SyncException(new SyncFailure.NotConfigured());
        }
        if (!url.startsWith("https://")) {
            throw new SyncException(new SyncFailure.InvalidUrl(url));
        }
        return ManifestParser.parse(downloadService.getUrlString(url));
    }
}
