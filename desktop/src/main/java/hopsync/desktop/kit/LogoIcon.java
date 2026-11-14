package hopsync.desktop.kit;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The HopSync mark: a small grass block. Drawn at any size, so the library header, the window icon and the
 * installer icons in {@code packaging/} all come from the same shape.
 */
public final class LogoIcon extends FlatAbstractIcon {
    private static final int SIZE = 18;
    private static final int[] WINDOW_SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};

    public LogoIcon() {
        super(SIZE, SIZE, null);
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        paint(g, SIZE);
    }

    /** The mark as images of the usual icon sizes, for {@code Window.setIconImages}. */
    public static List<Image> windowImages() {
        return IntStream.of(WINDOW_SIZES).mapToObj(LogoIcon::image).map(Image.class::cast).toList();
    }

    /** The mark on a transparent square of {@code size} pixels. */
    public static BufferedImage image(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paint(g, size);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Proportions of the 18 px design: 6 px corners, 7 px of grass, a 3 px shadow at the bottom. */
    private static void paint(Graphics2D g, float size) {
        float unit = size / SIZE;
        g.clip(new RoundRectangle2D.Float(0, 0, size, size, 6 * unit, 6 * unit));
        g.setColor(new Color(0x6E4B33));
        g.fillRect(0, 0, Math.round(size), Math.round(size));
        g.setColor(new Color(0x5DBB63));
        g.fillRect(0, 0, Math.round(size), Math.round(7 * unit));
        g.setColor(new Color(0, 0, 0, 64));
        g.fillRect(0, Math.round(size - 3 * unit), Math.round(size), Math.round(3 * unit));
    }
}
