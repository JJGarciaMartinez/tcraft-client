package util;

import config.AssetPaths;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * The FontLoader class provides utilities for loading and managing custom fonts,
 * specifically the Minecraft Mojangles font. It includes methods to load the font
 * from the assets directory and retrieve it in various styles and sizes.
 * <p>
 * This class ensures that the font is loaded only once and reused for later requests.
 * If the font fails to load, it falls back to a default system font (Arial).
 * <p>
 * The font is loaded as a classpath resource, which allows it to work both when running
 * from the IDE and when packaged in a JAR file.
 */
public class FontLoader {
    private static Font minecraftFont;

    /**
     * Gets the Minecraft Mojangles font, loading it from the assets directory if not already loaded.
     * The font is loaded as a classpath resource to support both IDE execution and JAR packaging.
     * @return The Minecraft Mojangles font, or Arial if loading fails
     */
    public static Font getMinecraftFont() {
        if (minecraftFont == null) {
            try {
                // Load font as a resource from the classpath (works in JAR and IDE)
                InputStream fontStream = FontLoader.class.getClassLoader().getResourceAsStream(AssetPaths.MINECRAFT_FONT);

                if (fontStream == null) {
                    throw new IOException("Font file not found in classpath: " + AssetPaths.MINECRAFT_FONT);
                }

                minecraftFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();

                // Register the font with the graphics environment
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(minecraftFont);

                System.out.println("✓ Fuente Minecraft cargada correctamente");
            } catch (FontFormatException | IOException e) {
                System.err.println("Error al cargar la fuente Minecraft: " + e.getMessage());
                System.err.println("  Usando fuente Arial como alternativa");
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

