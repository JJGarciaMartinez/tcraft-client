package service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class DownloadService {

    public String getUrlString(String urlString) throws IOException {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IOException("Invalidate URL: " + urlString, e);
        }

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000);     // 30 seconds

        // Check HTTP response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error: " + responseCode + " para URL: " + urlString);
        }

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

        try (Scanner scanner = new Scanner(connection.getInputStream(), charset)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
        
        
    }

    public void downloadFile(String urlString, File destination) throws IOException {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + urlString, e);
        }
        
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000);     // 30 seconds
        
        try {
            // Check HTTP response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode + " for URL: " + urlString);
            }
            
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
