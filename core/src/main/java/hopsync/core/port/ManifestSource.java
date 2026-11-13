package hopsync.core.port;

import hopsync.core.manifest.Manifest;

import java.io.IOException;

/**
 * Supplies the manifest published at a pack's link.
 */
@FunctionalInterface
public interface ManifestSource {
    Manifest fetchManifest(String url) throws IOException;
}
