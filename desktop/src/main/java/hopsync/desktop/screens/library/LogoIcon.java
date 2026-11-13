package hopsync.desktop.screens.library;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The HopSync mark: a small grass block.
 */
final class LogoIcon extends FlatAbstractIcon {
    private static final int SIZE = 18;

    LogoIcon() {
        super(SIZE, SIZE, null);
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.clip(new RoundRectangle2D.Float(0, 0, SIZE, SIZE, 6, 6));
        g.setColor(new Color(0x6E4B33));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(0x5DBB63));
        g.fillRect(0, 0, SIZE, 7);
        g.setColor(new Color(0, 0, 0, 64));
        g.fillRect(0, SIZE - 3, SIZE, 3);
    }
}
