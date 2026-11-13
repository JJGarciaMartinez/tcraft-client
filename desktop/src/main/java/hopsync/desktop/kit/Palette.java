package hopsync.desktop.kit;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Named colors of the HopSync theme for code that paints or tints by hand. The values live in
 * {@code HopSyncDark.properties}; the fallbacks only apply before the theme is installed.
 */
public final class Palette {

    private Palette() {
    }

    /** Window background. */
    public static Color background() { return color("HopSync.background", 0x141618); }
    /** Panels and cards on top of the background. */
    public static Color surface() { return color("HopSync.surface", 0x1B1E21); }
    /** Sunken areas: text fields, tracks. */
    public static Color well() { return color("HopSync.well", 0x0C0E0F); }
    public static Color edge() { return color("HopSync.edge", 0x3B4147); }

    public static Color text() { return color("HopSync.text", 0xECEEEB); }
    public static Color textSecondary() { return color("HopSync.textSecondary", 0x9AA1A6); }
    public static Color textFaint() { return color("HopSync.textFaint", 0x6A7277); }

    /** Installed, up to date, primary actions. */
    public static Color accent() { return color("HopSync.accent", 0x5DBB63); }
    /** Files the update will add. */
    public static Color added() { return color("HopSync.added", 0x6AA6FF); }
    /** Files the update will change or remove. */
    public static Color update() { return color("HopSync.update", 0xF0B44C); }
    public static Color error() { return color("HopSync.error", 0xEE6B6B); }

    private static Color color(String key, int fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : new Color(fallback);
    }
}
