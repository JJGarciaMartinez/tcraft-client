package hopsync.desktop.screens.library;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.LogoIcon;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The pack library on the left: every pack with its status, a button to check them all, "Añadir pack"
 * and the app settings. Selecting a pack (mouse, arrows) dispatches {@link UiAction.SelectPack}.
 */
public final class PackLibrary extends JPanel {
    private static final int WIDTH = 214;

    private final Consumer<UiAction> dispatch;
    private final DefaultListModel<PackUiState> model = new DefaultListModel<>();
    private final JList<PackUiState> list = new JList<>(model);
    private final PackCell cell = new PackCell();
    private final JLabel title = new JLabel();
    private boolean rendering;

    public PackLibrary(Consumer<UiAction> dispatch) {
        super(new BorderLayout());
        this.dispatch = dispatch;
        setBackground(Palette.sidebar());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Palette.line()));
        setPreferredSize(new Dimension(UIScale.scale(WIDTH), 0));
        setMinimumSize(getPreferredSize());

        add(header(), BorderLayout.NORTH);
        add(packList(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
    }

    private JPanel header() {
        JLabel logo = new JLabel("HopSync", new LogoIcon(), JLabel.LEFT);
        logo.putClientProperty(FlatClientProperties.STYLE, "font: bold +4");
        logo.setIconTextGap(UIScale.scale(8));
        JPanel brand = new JPanel(new BorderLayout());
        brand.setOpaque(false);
        brand.setPreferredSize(new Dimension(0, UIScale.scale(52)));
        brand.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.line()),
            BorderFactory.createEmptyBorder(0, 14, 0, 14)));
        brand.add(logo, BorderLayout.CENTER);

        title.putClientProperty(FlatClientProperties.STYLE, "font: semibold -2");
        title.setForeground(Palette.textFaint());
        JButton checkAll = new JButton(Icons.get(Icons.Glyph.REFRESH, 13, null));
        checkAll.setToolTipText(Messages.get("library.checkAll"));
        checkAll.getAccessibleContext().setAccessibleName(Messages.get("library.checkAll"));
        ButtonStyle.apply(checkAll, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
        checkAll.addActionListener(e -> dispatch.accept(new UiAction.CheckAll()));
        JPanel label = new JPanel(new BorderLayout());
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 8));
        label.add(title, BorderLayout.CENTER);
        label.add(checkAll, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(brand, BorderLayout.NORTH);
        header.add(label, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane packList() {
        list.setOpaque(false);
        list.setBackground(Palette.sidebar());
        list.setCellRenderer(cell);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(UIScale.scale(52));
        list.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.addListSelectionListener(e -> {
            PackUiState selected = list.getSelectedValue();
            if (!rendering && !e.getValueIsAdjusting() && selected != null) {
                dispatch.accept(new UiAction.SelectPack(selected.packId()));
            }
        });
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(list.locationToIndex(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }
        };
        list.addMouseMotionListener(hover);
        list.addMouseListener(hover);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JPanel footer() {
        JButton add = new DashedButton(Messages.get("library.add"), Icons.get(Icons.Glyph.PLUS));
        add.addActionListener(e -> dispatch.accept(new UiAction.AddPack()));
        JButton appSettings = new JButton(Messages.get("library.appSettings"), Icons.get(Icons.Glyph.GEAR));
        ButtonStyle.apply(appSettings, ButtonStyle.QUIET);
        appSettings.setHorizontalAlignment(JButton.LEFT);
        appSettings.addActionListener(e -> dispatch.accept(new UiAction.OpenAppSettings(Sheet.AppSettings.Section.GENERAL)));

        JPanel addRow = new JPanel(new GridLayout(1, 1));
        addRow.setOpaque(false);
        addRow.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        addRow.add(add);
        JPanel foot = new JPanel(new GridLayout(1, 1));
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.line()),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        foot.add(appSettings);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(addRow, BorderLayout.NORTH);
        footer.add(foot, BorderLayout.SOUTH);
        return footer;
    }

    public void render(UiState state) {
        rendering = true;
        try {
            title.setText(Messages.get("library.title", state.packs().size()).toUpperCase(Locale.ROOT));
            List<PackUiState> packs = new ArrayList<>(state.packs().values());
            cell.setNames(LibraryStatus.names(packs));
            if (!sameEntries(packs)) {
                model.clear();
                model.addAll(packs);
            }
            int selected = packs.stream().map(PackUiState::packId).toList().indexOf(state.selectedPackId());
            if (list.getSelectedIndex() != selected) {
                list.setSelectedIndex(selected);
            }
        } finally {
            rendering = false;
        }
    }

    private boolean sameEntries(List<PackUiState> packs) {
        if (model.size() != packs.size()) return false;
        for (int i = 0; i < packs.size(); i++) {
            if (!model.get(i).equals(packs.get(i))) return false;
        }
        return true;
    }

    private void setHover(int index) {
        if (cell.hoverIndex() != index) {
            cell.setHoverIndex(index);
            list.repaint();
        }
    }

    /** "Añadir pack": a dashed outline that turns green on hover, so it reads as a place to add more. */
    private static final class DashedButton extends JButton {
        private boolean hover;

        DashedButton(String text, javax.swing.Icon icon) {
            super(text, icon);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(true);
            setForeground(Palette.textSecondary());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, UIScale.scale(40)));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    setForeground(Palette.accent());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    setForeground(Palette.textSecondary());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float dash = UIScale.scale(5f);
                g2.setStroke(new BasicStroke(UIScale.scale(1.5f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10,
                    new float[] {dash, dash * 0.7f}, 0));
                g2.setColor(hover ? Palette.accent() : Palette.edge());
                int arc = UIScale.scale(10);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
