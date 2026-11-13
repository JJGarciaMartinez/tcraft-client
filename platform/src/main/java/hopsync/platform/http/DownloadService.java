package hopsync.platform.http;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.sync.FileDownloader;
import hopsync.core.util.AtomicFiles;
import hopsync.core.util.CancellationToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * HTTP access for the whole app: fetches small text documents (manifests) and downloads files with
 * size and SHA-512 verification. Transient failures (network errors, 5xx) are retried with backoff.
 */
public class DownloadService implements FileDownloader {
    private static final System.Logger LOG = System.getLogger(DownloadService.class.getName());

    /** Largest text document accepted; a manifest for hundreds of mods is well under 1 MB. */
    public static final long MAX_TEXT_BYTES = 5L * 1024 * 1024;
    static final int MAX_ATTEMPTS = 3;

    private final NetworkConfig network;
    private final Duration retryDelay;
    private final HttpClient client;
    private final Map<String, CachedText> textCache = new ConcurrentHashMap<>();

    private record CachedText(String etag, String body) {
    }

    public DownloadService(NetworkConfig network) {
        this(network, Duration.ofMillis(500));
    }

    /**
     * @param retryDelay wait before the first retry; doubles on each further attempt
     */
    DownloadService(NetworkConfig network, Duration retryDelay) {
        this.network = network;
        this.retryDelay = retryDelay;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(network.connectTimeoutMillis()))
            // NORMAL never follows an https → http redirect
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Fetches a text document, reusing the last copy when the server answers 304 Not Modified.
     */
    public String getUrlString(String url) throws IOException {
        URI uri = toUri(url);
        return withRetries(url, () -> {
            CachedText cached = textCache.get(url);
            HttpResponse<InputStream> response = send(uri, cached == null ? null : cached.etag(), CancellationToken.none());
            try (InputStream body = response.body()) {
                if (response.statusCode() == 304 && cached != null) {
                    return cached.body();
                }
                byte[] bytes = readLimited(body, url);
                String text = new String(bytes, charsetOf(response));
                response.headers().firstValue("ETag").ifPresentOrElse(
                    etag -> textCache.put(url, new CachedText(etag, text)),
                    () -> textCache.remove(url));
                return text;
            }
        });
    }

    @Override
    public void download(String url, Path destination, String expectedSha512, long expectedSize,
                         LongConsumer onBytes, CancellationToken cancel) throws IOException {
        URI uri = toUri(url);
        Path part = destination.resolveSibling(destination.getFileName() + PART_SUFFIX);
        String file = destination.getFileName().toString();
        try {
            withRetries(url, () -> {
                AtomicLong reported = new AtomicLong();
                try {
                    HttpResponse<InputStream> response = send(uri, null, cancel);
                    copyVerified(response.body(), part, file, expectedSha512, expectedSize, bytes -> {
                        reported.addAndGet(bytes);
                        onBytes.accept(bytes);
                    }, cancel, uri.getHost());
                    return null;
                } catch (IOException | RuntimeException e) {
                    // A retry starts from zero, so take back the progress this attempt reported
                    onBytes.accept(-reported.get());
                    throw e;
                }
            });
            AtomicFiles.move(part, destination);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    @FunctionalInterface
    private interface Attempt<T> {
        T run() throws IOException;
    }

    private <T> T withRetries(String url, Attempt<T> attempt) throws IOException {
        for (int n = 1; ; n++) {
            try {
                return attempt.run();
            } catch (SyncException e) {
                if (n >= MAX_ATTEMPTS || !isTransient(e.failure())) {
                    throw e;
                }
                LOG.log(System.Logger.Level.INFO, "Retrying " + url + " after: " + e.failure().describe());
                sleep(retryDelay.multipliedBy(1L << (n - 1)));
            }
        }
    }

    private static boolean isTransient(SyncFailure failure) {
        return failure instanceof SyncFailure.Network
            || failure instanceof SyncFailure.HttpStatus status && (status.status() >= 500 || status.status() == 429);
    }

    /**
     * Sends a GET and returns the response if it is 200 (or 304 when {@code etag} is given).
     */
    private HttpResponse<InputStream> send(URI uri, String etag, CancellationToken cancel) throws IOException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(network.readTimeoutMillis()))
            .header("User-Agent", network.userAgent())
            .GET();
        if (etag != null) {
            request.header("If-None-Match", etag);
        }
        CompletableFuture<HttpResponse<InputStream>> future =
            client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        HttpResponse<InputStream> response;
        CancellationToken.Registration registration = cancel.onCancel(() -> future.cancel(true));
        try {
            response = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted");
        } catch (ExecutionException e) {
            throw networkFailure(uri, e.getCause());
        } finally {
            registration.close();
        }
        int status = response.statusCode();
        if (status == 200 || status == 304 && etag != null) {
            return response;
        }
        response.body().close();
        throw new SyncException(new SyncFailure.HttpStatus(status, uri.toString()));
    }

    private void copyVerified(InputStream in, Path part, String file, String expectedSha512, long expectedSize,
                              LongConsumer onBytes, CancellationToken cancel, String host) throws IOException {
        MessageDigest digest = sha512();
        long total = 0;
        // Closing the body unblocks a read in progress when the player cancels
        CancellationToken.Registration registration = cancel.onCancel(() -> closeQuietly(in));
        try (InputStream body = in;
             OutputStream out = Files.newOutputStream(part);
             StallGuard guard = new StallGuard(Duration.ofMillis(network.readTimeoutMillis()), body)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while (true) {
                try {
                    read = body.read(buffer);
                } catch (IOException e) {
                    cancel.throwIfCancelled();
                    if (guard.stalled()) {
                        throw new SyncException(new SyncFailure.Network(host, "no data for " + network.readTimeoutMillis() + " ms"), e);
                    }
                    throw new SyncException(new SyncFailure.Network(host, String.valueOf(e.getMessage())), e);
                }
                if (read == -1) {
                    break;
                }
                cancel.throwIfCancelled();
                guard.touch();
                total += read;
                if (total > expectedSize) {
                    throw new SyncException(new SyncFailure.SizeMismatch(file, expectedSize, total));
                }
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
                onBytes.accept(read);
            }
        } finally {
            registration.close();
        }
        if (total != expectedSize) {
            throw new SyncException(new SyncFailure.SizeMismatch(file, expectedSize, total));
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(expectedSha512)) {
            throw new SyncException(new SyncFailure.HashMismatch(file));
        }
    }

    private static byte[] readLimited(InputStream in, String url) throws IOException {
        byte[] bytes = in.readNBytes((int) MAX_TEXT_BYTES + 1);
        if (bytes.length > MAX_TEXT_BYTES) {
            throw new SyncException(new SyncFailure.ResponseTooLarge(url, MAX_TEXT_BYTES));
        }
        return bytes;
    }

    private static SyncException networkFailure(URI uri, Throwable cause) {
        if (cause instanceof CancellationException e) {
            throw e;
        }
        String detail = cause.getClass().getSimpleName() + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
        return new SyncException(new SyncFailure.Network(uri.getHost(), detail), cause);
    }

    private static URI toUri(String url) throws SyncException {
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null) {
                throw new SyncException(new SyncFailure.InvalidUrl(url));
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new SyncException(new SyncFailure.InvalidUrl(url), e);
        }
    }

    private static Charset charsetOf(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type")
            .flatMap(type -> java.util.Arrays.stream(type.split(";"))
                .map(String::trim)
                .filter(part -> part.regionMatches(true, 0, "charset=", 0, 8))
                .map(part -> part.substring(8).replace("\"", ""))
                .findFirst())
            .filter(Charset::isSupported)
            .map(Charset::forName)
            .orElse(StandardCharsets.UTF_8);
    }

    private static MessageDigest sha512() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is required by every Java platform", e);
        }
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted");
        }
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            LOG.log(System.Logger.Level.DEBUG, "Ignoring error while aborting a download", e);
        }
    }

    /**
     * Closes the body when no bytes arrive for {@code limit}; HttpClient has no read timeout of its own
     * once the headers are in.
     */
    private static final class StallGuard implements AutoCloseable {
        private final AtomicLong lastProgress = new AtomicLong(System.nanoTime());
        private final Thread watcher;
        private volatile boolean stalled;
        private volatile boolean done;

        StallGuard(Duration limit, InputStream body) {
            long limitNanos = limit.toNanos();
            Duration poll = limit.dividedBy(4).compareTo(Duration.ofSeconds(1)) < 0 ? limit.dividedBy(4) : Duration.ofSeconds(1);
            watcher = Thread.ofVirtual().name("hopsync-stall-guard").start(() -> {
                while (!done) {
                    try {
                        Thread.sleep(poll);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!done && System.nanoTime() - lastProgress.get() > limitNanos) {
                        stalled = true;
                        closeQuietly(body);
                        return;
                    }
                }
            });
        }

        void touch() {
            lastProgress.set(System.nanoTime());
        }

        boolean stalled() {
            return stalled;
        }

        @Override
        public void close() {
            done = true;
            watcher.interrupt();
        }
    }
}
