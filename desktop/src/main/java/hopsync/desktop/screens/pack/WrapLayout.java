package hopsync.desktop.screens.pack;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * A {@link FlowLayout} that reports the height of all its lines when the components wrap, so the parent
 * gives it room instead of cutting the second line.
 */
final class WrapLayout extends FlowLayout {

    WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return wrappedSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return wrappedSize(target, false);
    }

    private Dimension wrappedSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int available = target.getWidth() > 0 ? target.getWidth() - insets.left - insets.right - getHgap() * 2
                : Integer.MAX_VALUE;
            int width = 0;
            int height = 0;
            int lineWidth = 0;
            int lineHeight = 0;
            for (Component c : target.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                if (lineWidth > 0 && lineWidth + getHgap() + d.width > available) {
                    width = Math.max(width, lineWidth);
                    height += lineHeight + getVgap();
                    lineWidth = 0;
                    lineHeight = 0;
                }
                lineWidth += (lineWidth > 0 ? getHgap() : 0) + d.width;
                lineHeight = Math.max(lineHeight, d.height);
            }
            width = Math.max(width, lineWidth);
            height += lineHeight;
            return new Dimension(width + insets.left + insets.right + getHgap() * 2,
                height + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}
