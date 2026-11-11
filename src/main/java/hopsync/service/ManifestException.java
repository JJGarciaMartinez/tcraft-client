package hopsync.service;

import java.io.IOException;

/**
 * Thrown when a manifest is well-formed JSON but does not satisfy the manifest format.
 */
public class ManifestException extends IOException {
    public ManifestException(String message) {
        super(message);
    }
}
