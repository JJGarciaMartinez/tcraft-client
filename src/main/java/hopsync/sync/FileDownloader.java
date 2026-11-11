package hopsync.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Downloads a file atomically, verifying its size and SHA-512 before it replaces {@code destination}.
 */
@FunctionalInterface
public interface FileDownloader {
    /** Suffix of in-progress downloads; leftovers from a crash are removed before each sync. */
    String PART_SUFFIX = ".hopsync-part";

    void download(String url, Path destination, String sha512, long size,
                  LongConsumer onBytes, BooleanSupplier cancelled) throws IOException;
}
