package service;

import config.AppProperties;
import model.ModInfo;
import util.JsonParser;

import java.io.IOException;
import java.util.List;

/**
 * Handles fetching and parsing the mod manifest from the server.
 */
public class ManifestService {
    private final DownloadService downloadService;

    public ManifestService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public List<ModInfo> fetchManifest() throws IOException {
        String url = AppProperties.getManifestUrl();
        String jsonContent = downloadService.getUrlString(url);
        List<ModInfo> mods = JsonParser.jsonSimpleParse(jsonContent);

        if (mods.isEmpty()) {
            throw new IOException("No se encontraron mods válidos en el manifest del servidor");
        }

        return mods;
    }
}
