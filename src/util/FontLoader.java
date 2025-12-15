package util;

import config.AssetPaths;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Utilidad para cargar fuentes personalizadas desde la carpeta assets/font
 */
public class FontLoader {
    private static Font minecraftFont;

    /**
     * Carga la fuente Minecraft Mojangles desde assets/font
     * @return La fuente cargada o Arial como fallback
     */
    public static Font getMinecraftFont() {
        if (minecraftFont == null) {
            try {
                File fontFile = new File(AssetPaths.MINECRAFT_FONT);
                minecraftFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);

                // Registrar la fuente en el GraphicsEnvironment
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
     * Obtiene la fuente Minecraft con un tamaño específico
     * @param size El tamaño de la fuente
     * @return La fuente con el tamaño especificado
     */
    public static Font getMinecraftFont(float size) {
        return getMinecraftFont().deriveFont(size);
    }

    /**
     * Obtiene la fuente Minecraft con un estilo y tamaño específicos
     * @param style El estilo (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size El tamaño de la fuente
     * @return La fuente con el estilo y tamaño especificados
     */
    public static Font getMinecraftFont(int style, float size) {
        return getMinecraftFont().deriveFont(style, size);
    }
}

