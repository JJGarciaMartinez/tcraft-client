package hopsync.desktop.kit;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.icons.FlatAbstractIcon;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Line icons drawn with Java2D on a 16 × 16 grid, so they stay sharp at any scale. Without an explicit
 * color they take the component's foreground (or the disabled text color).
 */
public final class Icons {

    public enum Glyph {
        CHECK, PLUS, UP, MINUS, X, WARN, SPINNER, PENDING, GEAR, FOLDER, COPY, SEARCH, REFRESH, CLOCK,
        SHIELD, USER, FILE, MORE, CHEVRON, CHEVRON_DOWN, EXTERNAL, TRASH
    }

    private static final int GRID = 16;
    private static final float STROKE = 1.5f;

    private Icons() {
    }

    public static Icon get(Glyph glyph) {
        return new GlyphIcon(glyph, GRID, null);
    }

    public static Icon get(Glyph glyph, int size, Color color) {
        return new GlyphIcon(glyph, size, color);
    }

    // Swing only greys out image icons by itself; this hands it the same glyph in the disabled color
    private static final class GlyphIcon extends FlatAbstractIcon implements FlatLaf.DisabledIconProvider {
        private final Glyph glyph;

        GlyphIcon(Glyph glyph, int size, Color color) {
            super(size, size, color);
            this.glyph = glyph;
        }

        @Override
        public Icon getDisabledIcon() {
            Color disabled = UIManager.getColor("Label.disabledForeground");
            return new GlyphIcon(glyph, width, disabled != null ? disabled : Palette.textFaint());
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            g.setColor(colorFor(c));
            g.scale(width / (double) GRID, height / (double) GRID);
            g.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Shape line : lines(glyph)) {
                g.draw(line);
            }
            for (Shape dot : dots(glyph)) {
                g.fill(dot);
            }
        }

        private Color colorFor(Component c) {
            if (c != null && !c.isEnabled()) {
                Color disabled = UIManager.getColor("Label.disabledForeground");
                if (disabled != null) {
                    return disabled;
                }
            }
            if (color != null) {
                return color;
            }
            return c != null ? c.getForeground() : Palette.text();
        }
    }

    private static Shape[] lines(Glyph glyph) {
        return switch (glyph) {
            case CHECK -> new Shape[] {polyline(3.5, 8.5, 6.5, 11.5, 12.5, 4.5)};
            case PLUS -> new Shape[] {line(8, 3, 8, 13), line(3, 8, 13, 8)};
            case UP -> new Shape[] {line(8, 13, 8, 3), polyline(4, 7, 8, 3, 12, 7)};
            case MINUS -> new Shape[] {line(3, 8, 13, 8)};
            case X -> new Shape[] {line(4, 4, 12, 12), line(12, 4, 4, 12)};
            case WARN -> new Shape[] {closed(8, 2.5, 14, 13, 2, 13), line(8, 6.5, 8, 9)};
            case SPINNER -> new Shape[] {new Arc2D.Double(2.5, 2.5, 11, 11, 90, 270, Arc2D.OPEN)};
            case PENDING -> new Shape[] {circle(8, 8, 5.5), polyline(8, 5, 8, 8)};
            case GEAR -> gear();
            case FOLDER -> new Shape[] {closed(2, 4, 6, 4, 7.5, 5.5, 14, 5.5, 14, 12.5, 2, 12.5)};
            case COPY -> new Shape[] {new RoundRectangle2D.Double(5.5, 5.5, 8, 8, 2, 2), polyline(2.5, 10.5, 2.5, 2.5, 10.5, 2.5)};
            case SEARCH -> new Shape[] {circle(7, 7, 4.5), line(10.5, 10.5, 13.5, 13.5)};
            case REFRESH -> new Shape[] {new Arc2D.Double(2.5, 2.5, 11, 11, 70, 290, Arc2D.OPEN), polyline(7.2, 0.8, 9.9, 2.8, 7.6, 5.2)};
            case CLOCK -> new Shape[] {circle(8, 8, 5.5), polyline(8, 5, 8, 8, 10.5, 9.5)};
            case SHIELD -> new Shape[] {shield()};
            case USER -> new Shape[] {circle(8, 5, 2.5), user()};
            case FILE -> new Shape[] {closed(4, 2, 9.5, 2, 12.5, 5, 12.5, 14, 4, 14), polyline(9.5, 2, 9.5, 5, 12.5, 5)};
            case MORE -> new Shape[0];
            case CHEVRON -> new Shape[] {polyline(6, 4, 10, 8, 6, 12)};
            case CHEVRON_DOWN -> new Shape[] {polyline(4, 6, 8, 10, 12, 6)};
            case EXTERNAL -> new Shape[] {polyline(12.5, 9, 12.5, 13, 3, 13, 3, 3.5, 7, 3.5), polyline(9, 3, 13, 3, 13, 7), line(13, 3, 7.5, 8.5)};
            case TRASH -> new Shape[] {line(2.5, 4.5, 13.5, 4.5), polyline(6, 4.5, 6, 2.5, 10, 2.5, 10, 4.5), polyline(4, 4.5, 5, 13.5, 11, 13.5, 12, 4.5)};
        };
    }

    private static Shape[] dots(Glyph glyph) {
        return switch (glyph) {
            case WARN -> new Shape[] {circle(8, 11.2, 0.9)};
            case MORE -> new Shape[] {circle(3.5, 8, 1.3), circle(8, 8, 1.3), circle(12.5, 8, 1.3)};
            default -> new Shape[0];
        };
    }

    private static Shape[] gear() {
        Shape[] shapes = new Shape[10];
        shapes[0] = circle(8, 8, 2.2);
        shapes[1] = circle(8, 8, 4.6);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            shapes[i + 2] = line(8 + 4.6 * Math.cos(angle), 8 + 4.6 * Math.sin(angle),
                8 + 6.4 * Math.cos(angle), 8 + 6.4 * Math.sin(angle));
        }
        return shapes;
    }

    private static Shape shield() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(8, 2);
        path.lineTo(13, 4);
        path.lineTo(13, 8);
        path.curveTo(13, 11, 10.5, 13, 8, 14);
        path.curveTo(5.5, 13, 3, 11, 3, 8);
        path.lineTo(3, 4);
        path.closePath();
        return path;
    }

    private static Shape user() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(3, 13.5);
        path.curveTo(3, 10.5, 5.2, 9, 8, 9);
        path.curveTo(10.8, 9, 13, 10.5, 13, 13.5);
        return path;
    }

    private static Shape line(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    private static Shape circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    private static Path2D polyline(double... points) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        return path;
    }

    private static Shape closed(double... points) {
        Path2D path = polyline(points);
        path.closePath();
        return path;
    }
}
