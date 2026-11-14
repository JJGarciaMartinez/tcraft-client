package hopsync.desktop.kit;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * Flat status label: tinted rounded background, colored text, no border and no hand cursor, so it
 * never looks clickable.
 */
public final class StatusTag extends JLabel {
    private static final float ARC = 10f;

    public enum Tone {
        NEUTRAL(Palette::textSecondary),
        MINE(Palette::mine),
        ADDED(Palette::added),
        UPDATE(Palette::update),
        ERROR(Palette::error);

        private final Supplier<Color> color;

        Tone(Supplier<Color> color) {
            this.color = color;
        }
    }

    private Tone tone;

    public StatusTag(String text, Tone tone) {
        super(text);
        setTone(tone);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        setFont(Fonts.medium(getFont()).deriveFont(getFont().getSize2D() - 1));
    }

    public void setTone(Tone tone) {
        this.tone = tone;
        setForeground(tone.color.get());
        repaint();
    }

    public Tone getTone() {
        return tone;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);
            g2.setColor(ColorFunctions.fade(tone.color.get(), 0.16f));
            g2.fill(FlatUIUtils.createComponentRectangle(0, 0, getWidth(), getHeight(), UIScale.scale(ARC)));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
