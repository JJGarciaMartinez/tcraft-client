package hopsync.desktop.kit;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIDefaults;

/**
 * The HopSync look and feel: FlatLaf Dark with the palette, shapes and fonts from
 * {@code HopSyncDark.properties}, raised buttons and sunken text fields.
 */
public class HopSyncDark extends FlatDarkLaf {
    private static final System.Logger LOG = System.getLogger(HopSyncDark.class.getName());

    public static final String NAME = "HopSync Dark";

    /**
     * Registers the fonts and installs the theme. Must run on the EDT before the first window is created.
     *
     * @return false if the theme could not be installed and Swing kept its default look and feel
     */
    public static boolean setup() {
        if (Fonts.registerBundled()) {
            FlatLaf.setPreferredFontFamily(Fonts.UI_FAMILY);
            FlatLaf.setPreferredSemiboldFontFamily(Fonts.UI_SEMIBOLD_FAMILY);
            FlatLaf.setPreferredMonospacedFontFamily(Fonts.MONO_FAMILY);
        }
        if (!FlatLaf.setup(new HopSyncDark())) {
            LOG.log(System.Logger.Level.WARNING, "Could not install the HopSync theme, using the default look and feel");
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();
        defaults.put("ButtonUI", RaisedButtonUI.class.getName());
        // Lazy: borders read their colors and widths from UIManager, which is only ready after setup
        UIDefaults.LazyValue sunken = table -> new SunkenBorder();
        defaults.put("TextField.border", sunken);
        defaults.put("FormattedTextField.border", sunken);
        defaults.put("PasswordField.border", sunken);
        return defaults;
    }
}
