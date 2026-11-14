package hopsync.desktop.kit;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Registers the bundled fonts (Rubik for the wordmark and titles, Figtree for the UI, JetBrains Mono for
 * versions and paths) so Swing can use them by family name. All are OFL; the licenses ship next to the files.
 */
public final class Fonts {
    private static final System.Logger LOG = System.getLogger(Fonts.class.getName());

    public static final String UI_FAMILY = "Figtree";
    public static final String UI_MEDIUM_FAMILY = "Figtree Medium";
    public static final String UI_SEMIBOLD_FAMILY = "Figtree SemiBold";
    public static final String MONO_FAMILY = "JetBrains Mono";
    /** Rubik 600: the HopSync wordmark, the pack name and sheet titles. */
    public static final String DISPLAY_FAMILY = "Rubik SemiBold";
    /** Rubik 700, registered under the plain family name with the bold style. */
    public static final String DISPLAY_BOLD_FAMILY = "Rubik";

    private static final String FONT_DIR = "assets/font/";
    private static final List<String> BUNDLED = List.of(
        "Figtree-Regular.ttf", "Figtree-Medium.ttf", "Figtree-SemiBold.ttf", "Figtree-Bold.ttf",
        "JetBrainsMono-Regular.ttf", "JetBrainsMono-Medium.ttf",
        "Rubik-SemiBold.ttf", "Rubik-Bold.ttf");

    private Fonts() {
    }

    /**
     * @return true when every bundled font was registered; otherwise the theme keeps the system font
     */
    public static boolean registerBundled() {
        boolean all = true;
        for (String file : BUNDLED) {
            all &= register(FONT_DIR + file);
        }
        return all;
    }

    /**
     * Medium weight of the UI font, or the default font when Figtree is missing.
     */
    public static Font medium(Font base) {
        Font medium = new Font(UI_MEDIUM_FAMILY, Font.PLAIN, base.getSize());
        return medium.getFamily().equals(UI_MEDIUM_FAMILY) ? medium : base;
    }

    /**
     * Rubik SemiBold {@code delta} points larger than {@code base}, or a bold {@code base} when Rubik is missing.
     */
    public static Font display(Font base, int delta) {
        int size = base.getSize() + delta;
        Font display = new Font(DISPLAY_FAMILY, Font.PLAIN, size);
        return display.getFamily().equals(DISPLAY_FAMILY) ? display : base.deriveFont(Font.BOLD, size);
    }

    static boolean register(String resource) {
        try (InputStream in = Fonts.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LOG.log(System.Logger.Level.WARNING, "Font not found in the JAR: " + resource);
                return false;
            }
            // false only means a font with that name is already installed, which serves as well
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
            return true;
        } catch (IOException | FontFormatException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not load font " + resource, e);
            return false;
        }
    }
}
