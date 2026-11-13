package hopsync.platform.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ManifestParser;
import hopsync.core.port.ManifestSource;
import hopsync.platform.http.DownloadService;
import hopsync.platform.settings.SettingsService;

import java.io.IOException;

/**
 * Downloads and validates the modpack manifest.
 */
public class ManifestService implements ManifestSource {
    private final DownloadService downloadService;
    private final SettingsService settingsService;

    public ManifestService(DownloadService downloadService, SettingsService settingsService) {
        this.downloadService = downloadService;
        this.settingsService = settingsService;
    }

    /** Fetches the manifest of the configured pack. */
    @Override
    public Manifest fetchManifest() throws IOException {
        String url = settingsService.getManifestUrl();
        if (url == null || url.isBlank()) {
            throw new SyncException(new SyncFailure.NotConfigured());
        }
        return fetchManifest(url);
    }

    public Manifest fetchManifest(String url) throws IOException {
        if (!url.startsWith("https://")) {
            throw new SyncException(new SyncFailure.InvalidUrl(url));
        }
        return ManifestParser.parse(downloadService.getUrlString(url));
    }
}
