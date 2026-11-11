package hopsync.service;

import hopsync.config.AppProperties;
import hopsync.model.Manifest;

import java.io.IOException;

/**
 * Downloads and validates the modpack manifest.
 */
public class ManifestService {
    private final DownloadService downloadService;

    public ManifestService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public Manifest fetchManifest() throws IOException {
        String json = downloadService.getUrlString(AppProperties.getManifestUrl());
        return ManifestParser.parse(json);
    }
}
