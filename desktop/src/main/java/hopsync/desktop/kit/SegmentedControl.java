package hopsync.desktop.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A choice between a few options: a sunken track (you choose here) with the chosen segment raised.
 */
public final class SegmentedControl<T> extends JPanel {
    private static final int ARC = 9;

    private final Map<T, JToggleButton> segments = new LinkedHashMap<>();
    private final ButtonGroup group = new ButtonGroup();
    private boolean updating;

    public SegmentedControl(List<T> values, Consumer<T> onSelect) {
        super(new FlowLayout(FlowLayout.LEFT, UIScale.scale(2), 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        for (T value : values) {
            JToggleButton segment = new Segment();
            segment.addActionListener(e -> {
                if (!updating) onSelect.accept(value);
            });
            group.add(segment);
            segments.put(value, segment);
            add(segment);
        }
    }

    /** @param html plain text, or HTML to show a count in a fainter color */
    public void setLabel(T value, String html) {
        JToggleButton segment = segments.get(value);
        if (!html.equals(segment.getText())) {
            segment.setText(html);
        }
    }

    /** Shows the selection without notifying, so rendering a state never dispatches. */
    public void setSelected(T value) {
        updating = true;
        try {
            segments.get(value).setSelected(true);
        } finally {
            updating = false;
        }
    }

    /** The track hugs its segments, even in layouts that stretch to the maximum size. */
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = UIScale.scale(ARC);
            g2.setColor(Palette.well());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            // Inner shadow along the top edge: the track is pressed into the surface
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 140), 0, UIScale.scale(4), new Color(0, 0, 0, 0)));
            g2.fillRoundRect(0, 0, getWidth(), UIScale.scale(6), arc, arc);
            g2.setColor(new Color(0x23272A));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        } finally {
            g2.dispose();
        }
    }

    /** One option: plain text on the track, a raised key when chosen. */
    private static final class Segment extends JToggleButton {
        private boolean hover;

        Segment() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(true);
            setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 11));
            putClientProperty(FlatClientProperties.STYLE, "font: semibold -1");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addChangeListener(e -> setForeground(isSelected() || hover ? Palette.text() : Palette.textSecondary()));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            setForeground(Palette.textSecondary());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = UIScale.scale(6);
                int w = getWidth();
                int h = getHeight();
                if (isSelected()) {
                    int bevel = UIScale.scale(2);
                    g2.setColor(new Color(0x08090A));
                    g2.fillRoundRect(0, bevel, w, h - bevel, arc, arc);
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x33393E), 0, h, Palette.raised()));
                    g2.fillRoundRect(0, 0, w, h - bevel, arc, arc);
                    g2.setColor(Palette.edge());
                    g2.drawRoundRect(0, 0, w - 1, h - bevel - 1, arc, arc);
                } else if (hover) {
                    g2.setColor(new Color(255, 255, 255, 10));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
