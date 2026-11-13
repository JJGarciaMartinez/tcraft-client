package hopsync.core.failure;

import java.io.IOException;

/**
 * An I/O failure with a known cause. Extends IOException so it flows through existing I/O error handling.
 */
public class SyncException extends IOException {
    private static final long serialVersionUID = 1L;

    private final transient SyncFailure failure;

    public SyncException(SyncFailure failure) {
        super(failure.describe());
        this.failure = failure;
    }

    public SyncException(SyncFailure failure, Throwable cause) {
        super(failure.describe(), cause);
        this.failure = failure;
    }

    public SyncFailure failure() {
        return failure;
    }
}
