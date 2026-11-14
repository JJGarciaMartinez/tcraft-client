package hopsync.desktop.kit;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.formdev.flatlaf.util.ColorFunctions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * The small mark in front of a status line. Never the only signal: it always goes next to text.
 */
public final class StatusMark extends FlatAbstractIcon {

    public enum Kind {
        /** Grey dot: unchecked, cancelled. */
        IDLE,
        /** Turquoise dot with a halo: up to date. */
        OK,
        /** Amber dot with a halo: changes waiting, applied with errors. */
        WARN,
        /** Rose dot with a halo: the update failed. */
        ERROR,
        /** Empty ring: waiting in the apply queue. */
        QUEUED,
        /** Open coral arc: checking or applying. */
        SPINNER,
        /** Turquoise ring filled up to the percent: downloading. */
        PROGRESS
    }

    private static final int SIZE = 12;

    private final Kind kind;
    private final int percent;

    private StatusMark(Kind kind, int percent) {
        super(SIZE, SIZE, null);
        this.kind = kind;
        this.percent = percent;
    }

    public static StatusMark of(Kind kind) {
        return new StatusMark(kind, 0);
    }

    public static StatusMark progress(int percent) {
        return new StatusMark(Kind.PROGRESS, Math.clamp(percent, 0, 100));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        switch (kind) {
            case IDLE -> dot(g, Palette.textFaint(), false);
            case OK -> dot(g, Palette.mine(), true);
            case WARN -> dot(g, Palette.update(), true);
            case ERROR -> dot(g, Palette.error(), true);
            case QUEUED -> {
                g.setColor(Palette.textSecondary());
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Float(3, 3, 6, 6));
            }
            case SPINNER -> {
                g.setColor(Palette.action());
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Float(1.5f, 1.5f, 9, 9, 90, 270, Arc2D.OPEN));
            }
            case PROGRESS -> {
                g.setColor(Palette.raised());
                g.fill(new Ellipse2D.Float(0, 0, SIZE, SIZE));
                g.setColor(Palette.mine());
                g.fill(new Arc2D.Float(0, 0, SIZE, SIZE, 90, -360f * percent / 100, Arc2D.PIE));
            }
        }
    }

    private static void dot(Graphics2D g, Color color, boolean halo) {
        if (halo) {
            g.setColor(ColorFunctions.fade(color, 0.18f));
            g.fill(new Ellipse2D.Float(0, 0, SIZE, SIZE));
        }
        g.setColor(color);
        g.fill(new Ellipse2D.Float(2, 2, 8, 8));
    }
}
