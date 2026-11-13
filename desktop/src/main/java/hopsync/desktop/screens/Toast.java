package hopsync.desktop.screens;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.UiState;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Short notice above the status bar ("IP copiada"). Shows each new {@link UiState.Toast} once and hides
 * itself after {@value #VISIBLE_MS} ms.
 */
final class Toast extends JLabel {
    private static final int VISIBLE_MS = 1700;

    private final JLayeredPane layer;
    private final Timer hide = new Timer(VISIBLE_MS, e -> setVisible(false));
    private long shownId;

    Toast(JLayeredPane layer) {
        this.layer = layer;
        hide.setRepeats(false);
        setOpaque(false);
        setVisible(false);
        setForeground(Palette.background());
        putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setIconTextGap(UIScale.scale(8));
        layer.add(this, JLayeredPane.POPUP_LAYER);
    }

    void render(UiState.Toast toast) {
        if (toast == null || toast.id() == shownId) return;
        shownId = toast.id();
        setText(toast.text());
        setIcon(Icons.get(Icons.Glyph.CHECK, 14, Palette.background()));
        Dimension size = getPreferredSize();
        setBounds((layer.getWidth() - size.width) / 2, layer.getHeight() - size.height - UIScale.scale(44),
            size.width, size.height);
        setVisible(true);
        hide.restart();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Palette.text());
            int arc = UIScale.scale(8);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
