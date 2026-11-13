package hopsync.platform.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ManifestParser;
import hopsync.core.port.ManifestSource;
import hopsync.core.util.AtomicFiles;
import hopsync.platform.http.DownloadService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Downloads and validates the modpack manifest. Every manifest that parses is also kept on disk, so the
 * window can show a pack's mods at startup before the network answers.
 */
public class ManifestService implements ManifestSource {
    private static final System.Logger LOG = System.getLogger(ManifestService.class.getName());

    private final DownloadService downloadService;
    private final Path cacheDir;

    /**
     * @param cacheDir folder for the last manifest of each link, created when first needed
     */
    public ManifestService(DownloadService downloadService, Path cacheDir) {
        this.downloadService = downloadService;
        this.cacheDir = cacheDir;
    }

    @Override
    public Manifest fetchManifest(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new SyncException(new SyncFailure.NotConfigured());
        }
        if (!url.startsWith("https://")) {
            throw new SyncException(new SyncFailure.InvalidUrl(url));
        }
        String text = downloadService.getUrlString(url);
        Manifest manifest = ManifestParser.parse(text);
        try {
            AtomicFiles.writeString(cacheFile(url), text);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not cache the manifest of " + url, e);
        }
        return manifest;
    }

    /**
     * The last manifest fetched from {@code url}, or empty when there is none or it no longer parses.
     */
    public Optional<Manifest> cached(String url) {
        try {
            return Optional.of(ManifestParser.parse(Files.readString(cacheFile(url), StandardCharsets.UTF_8)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Ignoring unreadable cached manifest of " + url, e);
            return Optional.empty();
        }
    }

    /** Deletes the cached manifest of a link no pack uses any more. */
    public void forget(String url) {
        try {
            Files.deleteIfExists(cacheFile(url));
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not delete the cached manifest of " + url, e);
        }
    }

    /** One file per link, named by a hash so any URL makes a safe file name. */
    Path cacheFile(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            return cacheDir.resolve(HexFormat.of().formatHex(hash, 0, 16) + ".json");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
