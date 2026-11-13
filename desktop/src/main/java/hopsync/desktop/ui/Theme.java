package hopsync.desktop.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import hopsync.desktop.config.AppProperties;

import java.awt.Color;
import java.util.Map;

/**
 * Applies a dark look and feel to every Swing component, including dialogs and the file chooser,
 * using the app's palette from app.properties.
 */
public final class Theme {

    private Theme() {
    }

    /**
     * Must run before the first window is created.
     */
    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
            "@background", hex(AppProperties.getBackgroundMedium()),
            "@foreground", hex(AppProperties.getTextPrimary()),
            "@accentColor", hex(AppProperties.getAccentSuccess()),
            "Component.borderColor", hex(AppProperties.getBorderColor()),
            "TextComponent.selectAllOnFocusPolicy", "never"
        ));
        if (!FlatDarkLaf.setup()) {
            System.err.println("Warning: could not install the dark theme, using the default look and feel");
        }
    }

    private static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }
}
