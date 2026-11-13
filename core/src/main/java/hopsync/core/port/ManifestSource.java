package hopsync.core.port;

import hopsync.core.manifest.Manifest;

import java.io.IOException;

/**
 * Supplies the manifest of the pack being synced.
 */
@FunctionalInterface
public interface ManifestSource {
    Manifest fetchManifest() throws IOException;
}
