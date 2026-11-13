package hopsync.desktop.kit;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.Cursor;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Button roles of the HopSync theme, applied as FlatLaf style classes ({@code [style]Button.<name>} in
 * {@code HopSyncDark.properties}). Buttons without a style are secondary.
 */
public enum ButtonStyle {
    /** The one main action of a screen. */
    PRIMARY("primary"),
    SECONDARY("secondary"),
    /** Flat until hovered; for minor actions next to others. */
    QUIET("quiet"),
    /** Stops or removes something. */
    DANGER("danger"),
    /** Smaller text and padding; combines with the others. */
    SMALL("small");

    private final String styleClass;

    ButtonStyle(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Replaces the button's styles, e.g. {@code ButtonStyle.apply(button, PRIMARY, SMALL)}.
     */
    public static void apply(AbstractButton button, ButtonStyle... styles) {
        button.putClientProperty(FlatClientProperties.STYLE_CLASS,
            Arrays.stream(styles).map(style -> style.styleClass).collect(Collectors.joining(" ")));
        boolean quiet = QUIET.isAppliedTo(button);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
            quiet ? FlatClientProperties.BUTTON_TYPE_BORDERLESS : null);
        button.setCursor(quiet ? null : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    boolean isAppliedTo(JComponent c) {
        return c.getClientProperty(FlatClientProperties.STYLE_CLASS) instanceof String classes
            && Arrays.asList(classes.split(" ")).contains(styleClass);
    }
}
