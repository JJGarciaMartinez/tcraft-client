package hopsync.service;

import hopsync.model.Manifest;

import java.io.IOException;

/**
 * Downloads and validates the modpack manifest.
 */
public class ManifestService {
    private final DownloadService downloadService;
    private final SettingsService settingsService;

    public ManifestService(DownloadService downloadService, SettingsService settingsService) {
        this.downloadService = downloadService;
        this.settingsService = settingsService;
    }

    /** Fetches the manifest of the configured pack. */
    public Manifest fetchManifest() throws IOException {
        String url = settingsService.getManifestUrl();
        if (url == null || url.isBlank()) {
            throw new IOException("No hay ningún pack configurado. Añade la URL del manifest en Ajustes.");
        }
        return fetchManifest(url);
    }

    public Manifest fetchManifest(String url) throws IOException {
        if (!url.startsWith("https://")) {
            throw new IOException("La URL del manifest debe empezar por https://");
        }
        return ManifestParser.parse(downloadService.getUrlString(url));
    }
}
