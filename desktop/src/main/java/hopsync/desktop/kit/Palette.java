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
    public static Color background() { return color("HopSync.background", 0x141317); }
    /** Panels and cards on top of the background. */
    public static Color surface() { return color("HopSync.surface", 0x1B1A20); }
    /** Sunken areas: text fields, tracks. */
    public static Color well() { return color("HopSync.well", 0x0D0C10); }
    public static Color edge() { return color("HopSync.edge", 0x3A3843); }
    /** Separators between areas. */
    public static Color line() { return color("HopSync.line", 0x2D2B34); }
    public static Color sidebar() { return color("HopSync.sidebar", 0x111015); }
    public static Color statusBar() { return color("HopSync.statusBar", 0x100F13); }
    /** Face of raised elements that are not buttons, such as the selected pack. */
    public static Color raised() { return color("HopSync.raised", 0x25232B); }
    /** Background of the list row under the mouse or expanded. */
    public static Color rowHover() { return color("HopSync.rowHover", 0x19181D); }
    /** Background of a row's expanded details. */
    public static Color rowDetail() { return color("HopSync.rowDetail", 0x17161B); }

    public static Color text() { return color("HopSync.text", 0xEEECF2); }
    public static Color textSecondary() { return color("HopSync.textSecondary", 0x9D9AA6); }
    public static Color textFaint() { return color("HopSync.textFaint", 0x6E6B78); }

    /** The box of the mark: brand titles and the toast. */
    public static Color brand() { return color("HopSync.brand", 0xE9E6EF); }
    /** What the player already has: installed, up to date, progress, links. */
    public static Color mine() { return color("HopSync.mine", 0x2EC4B6); }
    /** What arrives: the primary action. */
    public static Color action() { return color("HopSync.action", 0xFF6B4A); }
    /** Files the update will add. */
    public static Color added() { return color("HopSync.added", 0xFF6B4A); }
    /** Files the update will change. */
    public static Color update() { return color("HopSync.update", 0xF2B24A); }
    /** Errors and files the update will remove. */
    public static Color error() { return color("HopSync.error", 0xF2668B); }

    private static Color color(String key, int fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : new Color(fallback);
    }
}
