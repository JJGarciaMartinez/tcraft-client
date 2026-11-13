package hopsync.core.manifest;

import java.io.IOException;

/**
 * Thrown when a manifest is well-formed JSON but does not satisfy the manifest format.
 */
public class ManifestException extends IOException {
    private static final long serialVersionUID = 1L;

    public ManifestException(String message) {
        super(message);
    }
}
