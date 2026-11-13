package hopsync.desktop.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Button that looks physically raised: a face with a light top edge sitting on a darker bevel. While
 * pressed the face and its label drop onto the bevel; disabled buttons are flat. Colors per state come
 * from FlatLaf ({@code Button.*} keys and the style classes of {@link ButtonStyle}).
 */
public class RaisedButtonUI extends FlatButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new RaisedButtonUI();
    }

    protected RaisedButtonUI() {
        // Not shared: style classes and per-button colors are kept on the UI instance
        super(false);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (isRaised(c)) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    protected void paintBackground(Graphics g, JComponent c) {
        if (!isRaised(c)) {
            super.paintBackground(g, c);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);
            float focusWidth = FlatUIUtils.getBorderFocusWidth(c);
            float arc = FlatUIUtils.getBorderArc(c);
            float bevel = bevelWidth();
            float x = focusWidth;
            float width = c.getWidth() - focusWidth * 2;
            float fullHeight = c.getHeight() - focusWidth * 2;
            Color face = getBackground(c);
            if (face == null) {
                return;
            }

            if (!c.isEnabled()) {
                g2.setColor(face);
                g2.fill(FlatUIUtils.createComponentRectangle(x, focusWidth, width, fullHeight, arc));
                return;
            }

            float height = fullHeight - bevel;
            float top = focusWidth;
            if (isPressed(c)) {
                top += bevel;
            } else {
                g2.setColor(bevelColor(c));
                g2.fill(FlatUIUtils.createComponentRectangle(x, top + bevel, width, height, arc));
            }
            g2.setPaint(new GradientPaint(0, top, face, 0, top + height, ColorFunctions.darken(face, 0.04f)));
            g2.fill(FlatUIUtils.createComponentRectangle(x, top, width, height, arc));

            Color highlight = UIManager.getColor("HopSync.highlight");
            if (highlight != null) {
                g2.setColor(highlight);
                g2.fill(new Rectangle2D.Float(x + arc / 2, top, width - arc, UIScale.scale(1f)));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        // The label follows the face: centered on it at rest, lowered with it while pressed
        int shift = isRaised(c) && c.isEnabled() ? Math.round(bevelWidth() / 2) * (isPressed(c) ? 1 : -1) : 0;
        if (shift == 0) {
            super.paint(g, c);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(0, shift);
            super.paint(g2, c);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);
        if (size != null && isRaised(c)) {
            size.height += Math.round(bevelWidth());
        }
        return size;
    }

    /**
     * Quiet buttons, toolbar buttons and buttons without a filled content area keep FlatLaf's flat look.
     */
    static boolean isRaised(JComponent c) {
        return c instanceof JButton button
            && button.isContentAreaFilled()
            && !ButtonStyle.QUIET.isAppliedTo(button)
            && !FlatClientProperties.BUTTON_TYPE_BORDERLESS.equals(c.getClientProperty(FlatClientProperties.BUTTON_TYPE))
            && !(c.getParent() instanceof JToolBar);
    }

    private static boolean isPressed(JComponent c) {
        ButtonModel model = ((AbstractButton) c).getModel();
        return model.isArmed() && model.isPressed();
    }

    private static float bevelWidth() {
        return UIScale.scale((float) UIManager.getInt("HopSync.bevelWidth"));
    }

    private static Color bevelColor(JComponent c) {
        JButton button = (JButton) c;
        String key = "HopSync.bevel";
        if (ButtonStyle.DANGER.isAppliedTo(button)) {
            key += ".danger";
        } else if (ButtonStyle.PRIMARY.isAppliedTo(button) || button.isDefaultButton()) {
            key += ".primary";
        }
        Color color = UIManager.getColor(key);
        return color != null ? color : Palette.well();
    }
}
