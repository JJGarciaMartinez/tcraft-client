package hopsync.platform.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadServiceTest {
    private static final byte[] PAYLOAD = new byte[300_000];

    static {
        for (int i = 0; i < PAYLOAD.length; i++) PAYLOAD[i] = (byte) (i * 31);
    }

    @TempDir
    Path dir;
    HttpServer server;
    String baseUrl;
    final DownloadService service = new DownloadService(NetworkConfig.DEFAULT);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, PAYLOAD.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(PAYLOAD);
            }
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static String sha512(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(data));
    }

    private Path part(Path target) {
        return target.resolveSibling(target.getFileName() + DownloadService.PART_SUFFIX);
    }

    @Test
    void downloadsAndVerifies() throws Exception {
        Path target = dir.resolve("mod.jar");
        AtomicLong reported = new AtomicLong();

        service.download(baseUrl + "/file", target, sha512(PAYLOAD), PAYLOAD.length, reported::addAndGet, () -> false);

        assertArrayEquals(PAYLOAD, Files.readAllBytes(target));
        assertEquals(PAYLOAD.length, reported.get());
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void wrongHashKeepsTheOldFile() throws Exception {
        Path target = Files.writeString(dir.resolve("mod.jar"), "old version");

        assertThrows(IOException.class, () ->
            service.download(baseUrl + "/file", target, "0".repeat(128), PAYLOAD.length, n -> { }, () -> false));

        assertEquals("old version", Files.readString(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void wrongSizeIsRejected() throws Exception {
        Path target = dir.resolve("mod.jar");

        assertThrows(IOException.class, () ->
            service.download(baseUrl + "/file", target, sha512(PAYLOAD), PAYLOAD.length - 1, n -> { }, () -> false));
        assertThrows(IOException.class, () ->
            service.download(baseUrl + "/file", target, sha512(PAYLOAD), PAYLOAD.length + 1, n -> { }, () -> false));

        assertFalse(Files.exists(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void cancellationLeavesNothingBehind() throws Exception {
        Path target = dir.resolve("mod.jar");
        AtomicLong read = new AtomicLong();

        assertThrows(CancellationException.class, () -> service.download(baseUrl + "/file", target,
            sha512(PAYLOAD), PAYLOAD.length, read::addAndGet, () -> read.get() > 0));

        assertFalse(Files.exists(target));
        assertFalse(Files.exists(part(target)));
    }

    @Test
    void httpErrorsFail() {
        Path target = dir.resolve("mod.jar");
        assertThrows(IOException.class, () ->
            service.download(baseUrl + "/missing", target, "0".repeat(128), 1, n -> { }, () -> false));
        assertFalse(Files.exists(target));
    }
}
