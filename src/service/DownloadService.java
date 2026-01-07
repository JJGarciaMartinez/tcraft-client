package service;

import config.AppProperties;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * The DownloadService class provides utilities for downloading content from the internet.
 * It supports downloading content as a string or saving it directly to a file while
 * ensuring proper HTTP connection management and error handling.
 */
public class DownloadService {

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
        connection.setRequestProperty("User-Agent", AppProperties.getUserAgent());
        connection.setConnectTimeout(AppProperties.getConnectTimeout());
        connection.setReadTimeout(AppProperties.getReadTimeout());

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
     * Downloads a file from the specified URL and saves it to the provided destination file.
     * The method ensures that a partially downloaded file is deleted if an IOException occurs
     * during the download process.
     *
     * @param urlString the URL of the file to download
     * @param destination the file in which the downloaded content will be saved
     * @throws IOException if an I/O error occurs during the download process or while accessing the file system
     */
    public void downloadFile(String urlString, File destination) throws IOException {
        HttpURLConnection connection = createConnection(urlString);

        // Download the file
        try {
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Clean up partial download on failure
                if (destination.exists()) {
                    boolean deleted = destination.delete();
                    if (!deleted) {
                        // Log warning not to throw an exception.
                        System.err.println("Warning: Failed to delete partial file: " + destination.getAbsolutePath());
                    }
                }
                throw e;
            }
        } finally {
            connection.disconnect();
        }
    }
}
