package hopsync.platform.http;

import hopsync.core.sync.FileDownloader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Scanner;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * The DownloadService class provides utilities for downloading content from the internet.
 * It supports downloading content as a string or saving it to a file after verifying
 * its size and hash, ensuring proper HTTP connection management and error handling.
 */
public class DownloadService implements FileDownloader {
    private final NetworkConfig network;

    public DownloadService(NetworkConfig network) {
        this.network = network;
    }


    /**
     * Creates an {@link HttpURLConnection} instance for the specified URL string.
     * This method validates the URL format, sets up the connection properties,
     * and ensures the HTTP response status is 200 OK before returning the connection.
     *
     * @param urlString the URL string to establish a connection to
     * @return an initialized {@link HttpURLConnection} instance if the connection is successful
     * @throws IOException if the URL is invalid, the connection cannot be established,
     *                     or the HTTP response status is not 200 OK
     */
    private HttpURLConnection createConnection(String urlString) throws IOException {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + urlString, e);
        }

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestProperty("User-Agent", network.userAgent());
        connection.setConnectTimeout(network.connectTimeoutMillis());
        connection.setReadTimeout(network.readTimeoutMillis());

        // Check HTTP response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error: " + responseCode + " for URL: " + urlString);
        }

        return connection;
    }

    /**
     * Fetches the content of a URL as a string.
     * This method establishes an HTTP connection to the provided URL,
     * determines the character set from the response headers (defaulting to UTF-8 if absent),
     * and reads the content returned by the server into a string.
     *
     * @param urlString the URL string to fetch content from
     * @return the content retrieved from the specified URL as a string
     * @throws IOException if an I/O error occurs during the connection or data retrieval
     */
    public String getUrlString(String urlString) throws IOException {
        HttpURLConnection connection = createConnection(urlString);

        // Get charset from the Content-Type header, default to UTF-8
        String charset = "UTF-8";
        String contentType = connection.getContentType();
        if (contentType != null) {
            String[] parts = contentType.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("charset=")) {
                    charset = part.substring("charset=".length());
                    break;
                }
            }
        }

        // Resolve the input stream to a string
        try (Scanner scanner = new Scanner(connection.getInputStream(), charset)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Downloads {@code urlString} to {@code destination} only if the content matches the expected
     * size and SHA-512. Data is written to a temporary {@link FileDownloader#PART_SUFFIX} file next to the destination
     * and moved into place at the end, so the destination is either the old file or the complete,
     * verified new one; never a partial download.
     *
     * @param onBytes   receives the number of bytes read after each chunk
     * @param cancelled polled between chunks; when it returns true the download stops
     * @throws java.util.concurrent.CancellationException if cancelled
     * @throws IOException on network errors or when the content does not match
     */
    @Override
    public void download(String urlString, Path destination, String expectedSha512, long expectedSize,
                         LongConsumer onBytes, BooleanSupplier cancelled) throws IOException {
        Path part = destination.resolveSibling(destination.getFileName() + PART_SUFFIX);
        HttpURLConnection connection = createConnection(urlString);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            long total = 0;
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(part)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                        throw new CancellationException("Descarga cancelada");
                    }
                    total += read;
                    if (total > expectedSize) {
                        throw new IOException("El archivo descargado es más grande de lo esperado (" + expectedSize + " bytes)");
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                    onBytes.accept(read);
                }
            }
            if (total != expectedSize) {
                throw new IOException("Tamaño incorrecto: " + total + " bytes, se esperaban " + expectedSize);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equals(expectedSha512)) {
                throw new IOException("El hash SHA-512 no coincide; el archivo está corrupto o fue modificado");
            }
            try {
                Files.move(part, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(part, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is required by every Java platform", e);
        } finally {
            connection.disconnect();
            Files.deleteIfExists(part);
        }
    }
}
