package hopsync.platform.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.util.CancellationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadServiceTest {
    private static final byte[] PAYLOAD = new byte[300_000];

    static {
        for (int i = 0; i < PAYLOAD.length; i++) PAYLOAD[i] = (byte) (i * 31);
    }

    @TempDir
    Path dir;
    HttpServer server;
    ExecutorService serverThreads;
    String baseUrl;
    final AtomicInteger flakyCalls = new AtomicInteger();
    final AtomicInteger downCalls = new AtomicInteger();
    final AtomicInteger missingCalls = new AtomicInteger();
    final AtomicInteger textCalls = new AtomicInteger();
    final DownloadService service = new DownloadService(new NetworkConfig(2_000, 2_000, "test"), Duration.ofMillis(5));

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        serverThreads = Executors.newCachedThreadPool();
        server.setExecutor(serverThreads);
        server.createContext("/file", exchange -> send(exchange, 200, PAYLOAD));
        server.createContext("/missing", exchange -> {
            missingCalls.incrementAndGet();
            send(exchange, 404, null);
        });
        server.createContext("/flaky", exchange -> {
            if (flakyCalls.incrementAndGet() <= 2) send(exchange, 503, null);
            else send(exchange, 200, PAYLOAD);
        });
        server.createContext("/down", exchange -> {
            downCalls.incrementAndGet();
            send(exchange, 503, null);
        });
        server.createContext("/text", exchange -> {
            textCalls.incrementAndGet();
            if ("\"v1\"".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                send(exchange, 304, null);
            } else {
                exchange.getResponseHeaders().add("ETag", "\"v1\"");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                send(exchange, 200, "{\"pack\": \"Demo ñ\"}".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.createContext("/huge", exchange -> send(exchange, 200, new byte[(int) DownloadService.MAX_TEXT_BYTES + 1]));
        server.createContext("/stall", exchange -> {
            exchange.sendResponseHeaders(200, PAYLOAD.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(PAYLOAD, 0, 1_000);
                out.flush();
                Thread.sleep(3_000);
            } catch (InterruptedException | IOException e) {
                // the client gave up, which is what the test expects
            }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        serverThreads.shutdownNow();
    }

    private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
        if (body != null) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }

    private static String sha512(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(data));
    }

    private Path part(Path target) {
        return target.resolveSibling(target.getFileName() + DownloadService.PART_SUFFIX);
    }

    private SyncFailure failureOf(ThrowingRunnable action) {
        return assertThrows(SyncException.class, action::run).failure();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    void downloadsAndVerifies() throws Exception {
        Path target = dir.resolve("mod.jar");
        AtomicLong reported = new AtomicLong();

        service.download(baseUrl + "/file", target, sha512(PAYLOAD), PAYLOAD.length, reported::addAndGet, CancellationToken.none());

        assertArrayEquals(PAYLOAD, Files.readAllBytes(target));
        assertEquals(PAYLOAD.length, reported.get());
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void wrongHashKeepsTheOldFile() throws Exception {
        Path target = Files.writeString(dir.resolve("mod.jar"), "old version");

        SyncFailure failure = failureOf(() ->
            service.download(baseUrl + "/file", target, "0".repeat(128), PAYLOAD.length, n -> { }, CancellationToken.none()));

        assertEquals(new SyncFailure.HashMismatch("mod.jar"), failure);
        assertEquals("old version", Files.readString(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void wrongSizeIsRejected() throws Exception {
        Path target = dir.resolve("mod.jar");

        assertInstanceOf(SyncFailure.SizeMismatch.class, failureOf(() -> service.download(baseUrl + "/file", target,
            sha512(PAYLOAD), PAYLOAD.length - 1, n -> { }, CancellationToken.none())));
        assertInstanceOf(SyncFailure.SizeMismatch.class, failureOf(() -> service.download(baseUrl + "/file", target,
            sha512(PAYLOAD), PAYLOAD.length + 1, n -> { }, CancellationToken.none())));

        assertFalse(Files.exists(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void cancellationLeavesNothingBehind() throws Exception {
        Path target = dir.resolve("mod.jar");
        CancellationToken cancel = new CancellationToken();

        assertThrows(CancellationException.class, () -> service.download(baseUrl + "/file", target,
            sha512(PAYLOAD), PAYLOAD.length, n -> cancel.cancel(), cancel));

        assertFalse(Files.exists(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void clientErrorsFailWithoutRetrying() {
        Path target = dir.resolve("mod.jar");

        SyncFailure failure = failureOf(() ->
            service.download(baseUrl + "/missing", target, "0".repeat(128), 1, n -> { }, CancellationToken.none()));

        assertEquals(new SyncFailure.HttpStatus(404, baseUrl + "/missing"), failure);
        assertEquals(1, missingCalls.get());
        assertFalse(Files.exists(target));
    }

    @Test
    void serverErrorsAreRetried() throws Exception {
        Path target = dir.resolve("mod.jar");
        AtomicLong reported = new AtomicLong();

        service.download(baseUrl + "/flaky", target, sha512(PAYLOAD), PAYLOAD.length, reported::addAndGet, CancellationToken.none());

        assertEquals(3, flakyCalls.get());
        assertArrayEquals(PAYLOAD, Files.readAllBytes(target));
        assertEquals(PAYLOAD.length, reported.get());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        SyncFailure failure = failureOf(() -> service.getUrlString(baseUrl + "/down"));

        assertEquals(new SyncFailure.HttpStatus(503, baseUrl + "/down"), failure);
        assertEquals(DownloadService.MAX_ATTEMPTS, downCalls.get());
    }

    @Test
    void unreachableHostIsANetworkFailure() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }

        SyncFailure failure = failureOf(() -> service.getUrlString("http://localhost:" + closedPort + "/manifest.json"));

        assertInstanceOf(SyncFailure.Network.class, failure);
    }

    @Test
    void stalledDownloadIsANetworkFailureAndRollsBackProgress() {
        DownloadService impatient = new DownloadService(new NetworkConfig(2_000, 300, "test"), Duration.ofMillis(5));
        Path target = dir.resolve("mod.jar");
        AtomicLong reported = new AtomicLong();

        SyncFailure failure = failureOf(() -> impatient.download(baseUrl + "/stall", target, sha512(PAYLOAD),
            PAYLOAD.length, reported::addAndGet, CancellationToken.none()));

        assertInstanceOf(SyncFailure.Network.class, failure);
        assertEquals(0, reported.get());
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void textReusesTheCachedCopyWhenNotModified() throws Exception {
        String first = service.getUrlString(baseUrl + "/text");
        String second = service.getUrlString(baseUrl + "/text");

        assertEquals("{\"pack\": \"Demo ñ\"}", first);
        assertEquals(first, second);
        assertEquals(2, textCalls.get());
    }

    @Test
    void oversizedTextIsRejected() {
        SyncFailure failure = failureOf(() -> service.getUrlString(baseUrl + "/huge"));

        assertEquals(new SyncFailure.ResponseTooLarge(baseUrl + "/huge", DownloadService.MAX_TEXT_BYTES), failure);
    }
}
