package hopsync.core.manifest;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;

/**
 * Thrown when a manifest cannot be used: invalid JSON, a broken format or an unsupported version.
 */
public class ManifestException extends SyncException {
    private static final long serialVersionUID = 1L;

    public ManifestException(String detail) {
        super(new SyncFailure.ManifestInvalid(detail));
    }

    public ManifestException(String detail, Throwable cause) {
        super(new SyncFailure.ManifestInvalid(detail), cause);
    }

    public ManifestException(SyncFailure failure) {
        super(failure);
    }
}
