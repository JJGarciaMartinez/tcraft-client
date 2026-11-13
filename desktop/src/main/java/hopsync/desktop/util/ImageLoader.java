package hopsync.desktop.util;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Utility class for loading image resources from the classpath.
 * This allows images to be loaded both when running from the IDE and when packaged in a JAR file.
 */
public class ImageLoader {
    private static final System.Logger LOG = System.getLogger(ImageLoader.class.getName());


    /**
     * Loads an ImageIcon from the classpath.
     *
     * @param path The path to the image resource (e.g., "assets/image.png")
     * @return An ImageIcon containing the loaded image, or null if the image cannot be loaded
     */
    public static ImageIcon loadImageIcon(String path) {
        try {
            URL resourceUrl = ImageLoader.class.getClassLoader().getResource(path);

            if (resourceUrl == null) {
                LOG.log(System.Logger.Level.WARNING, "Image not found in the classpath: " + path);
                return null;
            }

            return new ImageIcon(resourceUrl);
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Could not load image " + path, e);
            return null;
        }
    }

    /**
     * Loads an ImageIcon from the classpath and scales it to the specified dimensions.
     * Use -1 for width or height to maintain an aspect ratio.
     *
     * @param path The path to the image resource
     * @param width The target width (-1 to maintain an aspect ratio)
     * @param height The target height (-1 to maintain an aspect ratio)
     * @return A scaled ImageIcon, or null if the image cannot be loaded
     */
    public static ImageIcon loadScaledImageIcon(String path, int width, int height) {
        ImageIcon originalIcon = loadImageIcon(path);

        if (originalIcon == null) {
            return null;
        }

        // Calculate dimensions if -1 is used (maintain an aspect ratio)
        int targetWidth = width;
        int targetHeight = height;

        if (width == -1 && height == -1) {
            // Both -1, return original
            return originalIcon;
        } else if (width == -1) {
            // Calculate width based on height
            double ratio = (double) originalIcon.getIconWidth() / originalIcon.getIconHeight();
            targetWidth = (int) (height * ratio);
        } else if (height == -1) {
            // Calculate height based on width
            double ratio = (double) originalIcon.getIconHeight() / originalIcon.getIconWidth();
            targetHeight = (int) (width * ratio);
        }

        Image scaledImage = originalIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
}