package hopsync.desktop.screens.mods;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Fonts;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.StatusTag;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Locale;

/**
 * Draws each kind of {@link ModRow}. Mod rows use the design's columns: icon, name, version, author, size,
 * status tag and chevron; the name takes the remaining width.
 */
final class ModRowRenderer implements ListCellRenderer<ModRow> {
    static final int ROW_HEIGHT = 34;

    private final ModLine mod = new ModLine();
    private final DetailLine detail = new DetailLine();
    private final JLabel group = text(true);
    private final JLabel more = text(false);
    private final JLabel message = text(false);
    private final JLabel banner = new JLabel();
    private int hoverIndex = -1;
    private String expandedKey;

    ModRowRenderer() {
        group.putClientProperty(FlatClientProperties.STYLE, "font: semibold -2");
        group.setForeground(Palette.textSecondary());
        group.setBorder(BorderFactory.createEmptyBorder(12, 20, 4, 20));
        more.setForeground(Palette.textSecondary());
        more.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        message.setForeground(Palette.textSecondary());
        message.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        banner.setIcon(Icons.get(Icons.Glyph.SHIELD, 16, Palette.accent()));
        banner.setIconTextGap(UIScale.scale(8));
        banner.setForeground(Palette.textSecondary());
        banner.setOpaque(true);
        banner.setBackground(ColorFunctions.fade(Palette.accent(), 0.06f));
        banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(10, 16, 4, 16, Palette.background()),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    void setHoverIndex(int index) {
        hoverIndex = index;
    }

    int hoverIndex() {
        return hoverIndex;
    }

    void setExpandedKey(String key) {
        expandedKey = key;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ModRow> list, ModRow row, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        return switch (row) {
            case ModRow.Mod m -> mod.show(m, index == hoverIndex, m.key().equals(expandedKey));
            case ModRow.Detail d -> detail.show(d, index == hoverIndex);
            case ModRow.Group g -> {
                group.setText(g.note() == null ? g.title().toUpperCase(Locale.ROOT)
                    : "<html>" + escape(g.title().toUpperCase(Locale.ROOT)) + "&nbsp;&nbsp;<span style='color:"
                        + hex(Palette.textFaint()) + "'>" + escape(g.note()) + "</span></html>");
                yield group;
            }
            case ModRow.More m -> {
                more.setText(m.text());
                more.setForeground(index == hoverIndex ? Palette.text() : Palette.textSecondary());
                yield more;
            }
            case ModRow.Message m -> {
                message.setText(m.text());
                yield message;
            }
            case ModRow.Banner b -> {
                banner.setText(b.html());
                yield banner;
            }
        };
    }

    /** Where a mod row's status button is drawn, in cell coordinates, for hit-testing clicks. */
    Rectangle statusBounds(ModRow.Mod row, int width) {
        mod.show(row, false, false);
        mod.setSize(width, mod.getPreferredSize().height);
        layoutTree(mod);
        return SwingUtilities.convertRectangle(mod.retry.getParent(), mod.retry.getBounds(), mod);
    }

    /** Where the detail row's link is drawn, in cell coordinates, for hit-testing clicks. */
    Rectangle linkBounds(ModRow.Detail row, int width) {
        detail.show(row, false);
        detail.setSize(width, detail.getPreferredSize().height);
        layoutTree(detail);
        return SwingUtilities.convertRectangle(detail.link.getParent(), detail.link.getBounds(), detail);
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) layoutTree(inner);
        }
    }

    private static JLabel text(boolean bold) {
        JLabel label = new JLabel();
        label.setOpaque(false);
        if (bold) label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }

    static Color toneColor(ModRow.IconTone tone) {
        return switch (tone) {
            case FAINT -> Palette.textFaint();
            case ACCENT -> Palette.accent();
            case ADDED -> Palette.added();
            case UPDATE -> Palette.update();
            case ERROR -> Palette.error();
        };
    }

    private static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A 34 px mod row with fixed columns. */
    private static final class ModLine extends JPanel {
        private final JLabel icon = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel version = new JLabel();
        private final JLabel author = new JLabel();
        private final JLabel size = new JLabel();
        private final JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        private final StatusTag tag = new StatusTag("", StatusTag.Tone.NEUTRAL);
        private final MiniBar bar = new MiniBar();
        final JButton retry = new JButton(Messages.get("mods.retry"));
        private final JLabel chevron = new JLabel();

        ModLine() {
            super(new Columns());
            setOpaque(true);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorFunctions.fade(Palette.line(), 0.45f)));
            // FlatLaf styles have no medium weight; the bundled Figtree Medium gives the design's 500
            name.setFont(Fonts.medium(name.getFont()));
            version.setForeground(Palette.textSecondary());
            version.setFont(new Font(Fonts.MONO_FAMILY, Font.PLAIN, Math.round(UIScale.scale(11.5f))));
            author.setForeground(Palette.textFaint());
            size.setForeground(Palette.textFaint());
            size.setHorizontalAlignment(JLabel.RIGHT);
            status.setOpaque(false);
            ButtonStyle.apply(retry, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
            status.add(tag);
            status.add(bar);
            status.add(retry);
            for (JComponent part : new JComponent[] {icon, name, version, author, size, status, chevron}) {
                add(part);
            }
        }

        ModLine show(ModRow.Mod row, boolean hovered, boolean expanded) {
            icon.setIcon(Icons.get(row.icon(), 16, toneColor(row.iconTone())));
            name.setText(row.name());
            name.setForeground(Palette.text());
            name.setToolTipText(row.name());
            version.setText(row.oldVersion() == null ? row.version()
                : "<html><span style='color:" + hex(Palette.textFaint()) + "'>" + escape(row.oldVersion())
                    + "</span> → " + escape(row.version()) + "</html>");
            author.setText(row.author());
            author.setForeground(row.look() == ModRow.Look.ERROR ? Palette.error() : Palette.textFaint());
            size.setText(row.size());
            tag.setVisible(row.tag() != null && row.progress() == null);
            if (row.tag() != null) {
                tag.setText(row.tag().text());
                tag.setTone(row.tag().tone());
            }
            bar.setVisible(row.progress() != null);
            bar.percent = row.progress() == null ? 0 : row.progress();
            retry.setVisible(row.statusAction() != null);
            chevron.setIcon(row.expandable() && (hovered || expanded)
                ? Icons.get(expanded ? Icons.Glyph.CHEVRON_DOWN : Icons.Glyph.CHEVRON, 14,
                    hovered ? Palette.text() : Palette.textFaint())
                : null);
            setBackground(switch (row.look()) {
                case CURRENT -> ColorFunctions.fade(Palette.accent(), 0.07f);
                case ERROR -> ColorFunctions.fade(Palette.error(), 0.06f);
                case NORMAL -> hovered || expanded ? Palette.rowHover() : Palette.background();
            });
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, UIScale.scale(ROW_HEIGHT));
        }
    }

    /** The download of one file: a small sunken track filling with the accent color. */
    private static final class MiniBar extends JComponent {
        int percent;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(UIScale.scale(60), UIScale.scale(6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();
                g2.setColor(Palette.well());
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.setColor(Palette.accent());
                g2.fillRoundRect(0, 0, getWidth() * percent / 100, h, h, h);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Lays out icon 18 · name (rest) · version 128 · author 104 · size 72 · status 96 · chevron 14. Size and
     * status grow when their content needs it (a long config tag), taking the room from the name.
     */
    private static final class Columns implements LayoutManager {
        private static final int[] FIXED = {18, 0, 128, 104, 72, 96, 14};
        private static final boolean[] GROWS = {false, false, false, false, true, true, false};
        private static final int GAP = 10;
        private static final int LEFT = 20;
        private static final int RIGHT = 14;

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int height = parent.getHeight() - insets.top - insets.bottom;
            int[] widths = new int[FIXED.length];
            int fixed = 0;
            for (int i = 0; i < FIXED.length; i++) {
                widths[i] = UIScale.scale(FIXED[i]);
                if (GROWS[i]) {
                    widths[i] = Math.max(widths[i], parent.getComponent(i).getPreferredSize().width);
                }
                fixed += widths[i];
            }
            int gaps = UIScale.scale(GAP) * (FIXED.length - 1);
            int nameWidth = Math.max(0, parent.getWidth() - UIScale.scale(LEFT + RIGHT) - fixed - gaps);
            int x = UIScale.scale(LEFT);
            for (int i = 0; i < FIXED.length; i++) {
                int width = FIXED[i] == 0 ? nameWidth : widths[i];
                Component child = parent.getComponent(i);
                int childHeight = Math.min(height, child.getPreferredSize().height);
                child.setBounds(x, insets.top + (height - childHeight) / 2, width, childHeight);
                x += width + UIScale.scale(GAP);
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(UIScale.scale(600), UIScale.scale(ROW_HEIGHT));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    /** The expanded part of a mod row: description, file, full version, size and a link to its page. */
    private static final class DetailLine extends JPanel {
        private final JLabel description = new JLabel();
        private final JLabel facts = new JLabel();
        final JLabel link = new JLabel();

        DetailLine() {
            super(new BorderLayout(UIScale.scale(20), 0));
            setOpaque(true);
            setBackground(Palette.rowDetail());
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.line()),
                BorderFactory.createEmptyBorder(10, 48, 14, 20)));
            description.setForeground(Palette.text());
            facts.setForeground(Palette.textFaint());
            facts.putClientProperty(FlatClientProperties.STYLE, "font: -1");
            link.setForeground(new Color(0x9FDCA3));
            link.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
            link.setIcon(Icons.get(Icons.Glyph.EXTERNAL, 13, new Color(0x9FDCA3)));
            link.setHorizontalTextPosition(JLabel.LEFT);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(description);
            text.add(Box.createVerticalStrut(UIScale.scale(6)));
            text.add(facts);
            add(text, BorderLayout.CENTER);
            JPanel go = new JPanel(new GridBagLayout());
            go.setOpaque(false);
            go.add(link);
            add(go, BorderLayout.EAST);
        }

        DetailLine show(ModRow.Detail row, boolean hovered) {
            description.setText("<html>" + escape(row.description()) + "</html>");
            facts.setText("<html>" + fact("mods.detail.file", row.file()) + "&nbsp;&nbsp;&nbsp;&nbsp;"
                + fact("mods.detail.version", row.version()) + "&nbsp;&nbsp;&nbsp;&nbsp;"
                + fact("mods.detail.size", row.size()) + "</html>");
            link.setVisible(row.url() != null);
            link.setText(row.linkLabel() == null ? "" : "<html><u>" + escape(row.linkLabel()) + "</u></html>");
            return this;
        }

        private static String fact(String key, String value) {
            return escape(Messages.get(key)) + " <span style='color:"
                + hex(Palette.textSecondary()) + "'>" + escape(value) + "</span>";
        }
    }
}
