package util;

import config.AssetPaths;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * The FontLoader class provides utilities for loading and managing custom fonts,
 * specifically the Minecraft Mojangles font. It includes methods to load the font
 * from the assets directory and retrieve it in various styles and sizes.
 * <p>
 * This class ensures that the font is loaded only once and reused for later requests.
 * If the font fails to load, it falls back to a default system font (Arial).
 */
public class FontLoader {
    private static Font minecraftFont;

    /**
     * Gets the Minecraft Mojangles font, loading it from the assets directory if not already loaded.
     * @return The Minecraft Mojangles font
     */
    public static Font getMinecraftFont() {
        if (minecraftFont == null) {
            try {
                File fontFile = new File(AssetPaths.MINECRAFT_FONT);
                minecraftFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);

                // Register the font with the graphics environment
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(minecraftFont);
            } catch (FontFormatException | IOException e) {
                System.err.println("Error al cargar la fuente Minecraft: " + e.getMessage());
                minecraftFont = new Font("Arial", Font.PLAIN, 12);
            }
        }
        return minecraftFont;
    }

    /**
     * Gets the Minecraft font with a specific size
     * @param size The size of the font
     * @return The font with the specified size
     */
    public static Font getMinecraftFont(float size) {
        return getMinecraftFont().deriveFont(size);
    }
}

