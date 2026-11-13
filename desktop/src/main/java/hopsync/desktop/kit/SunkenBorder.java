package hopsync.desktop.kit;

import com.formdev.flatlaf.ui.FlatTextBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Text field border that looks pressed into the window: FlatLaf's outline and focus ring plus a soft
 * shadow along the inner top edge.
 */
public class SunkenBorder extends FlatTextBorder {
    private static final float SHADOW_HEIGHT = 4f;

    private final Color innerShadow = UIManager.getColor("HopSync.innerShadow");

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        super.paintBorder(c, g, x, y, width, height);
        if (innerShadow == null || !(c instanceof JComponent jc) || !c.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);
            float inset = FlatUIUtils.getBorderFocusWidth(jc) + UIScale.scale(getBorderWidth(c));
            float arc = Math.max(0, FlatUIUtils.getBorderArc(jc) - inset);
            float top = y + inset;
            g2.clip(FlatUIUtils.createComponentRectangle(x + inset, top, width - inset * 2, height - inset * 2, arc));
            g2.setPaint(new GradientPaint(0, top, innerShadow,
                0, top + UIScale.scale(SHADOW_HEIGHT), new Color(innerShadow.getRGB() & 0xFFFFFF, true)));
            g2.fillRect(x, (int) top, width, Math.round(UIScale.scale(SHADOW_HEIGHT)) + 1);
        } finally {
            g2.dispose();
        }
    }
}
