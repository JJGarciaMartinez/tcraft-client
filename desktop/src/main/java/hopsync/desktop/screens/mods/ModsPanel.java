package hopsync.desktop.screens.mods;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.SegmentedControl;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.desktop.store.UiState.Filter;
import hopsync.desktop.store.UiState.Tab;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

/**
 * The selected pack's files: tabs (Pack, Tus mods, Configs) with their counts, the "Solo cambios / Todos"
 * filter when there is a plan, a search box (one search per tab) and the list.
 */
public final class ModsPanel extends JPanel {
    private final SegmentedControl<Tab> tabs;
    private final SegmentedControl<Filter> filter;
    private final JTextField search = new JTextField();
    private final ModTable table;
    private boolean rendering;

    public ModsPanel(Consumer<UiAction> dispatch) {
        super(new BorderLayout());
        setBackground(Palette.background());
        tabs = new SegmentedControl<>(List.of(Tab.values()), tab -> dispatch.accept(new UiAction.SelectTab(tab)));
        filter = new SegmentedControl<>(List.of(Filter.values()), value -> dispatch.accept(new UiAction.SetFilter(value)));
        filter.setLabel(Filter.CHANGES, Messages.get("mods.filter.changes"));
        filter.setLabel(Filter.ALL, Messages.get("mods.filter.all"));

        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, Messages.get("mods.search"));
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.get(Icons.Glyph.SEARCH, 14, Palette.textFaint()));
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.getAccessibleContext().setAccessibleName(Messages.get("mods.search"));
        search.setPreferredSize(new Dimension(UIScale.scale(210), search.getPreferredSize().height));
        search.setMaximumSize(search.getPreferredSize());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                if (!rendering) dispatch.accept(new UiAction.SetQuery(search.getText()));
            }
        });

        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.line()),
            BorderFactory.createEmptyBorder(9, 16, 9, 16)));
        bar.add(tabs);
        bar.add(Box.createHorizontalGlue());
        bar.add(filter);
        bar.add(Box.createHorizontalStrut(UIScale.scale(10)));
        bar.add(search);

        table = new ModTable(dispatch);
        add(bar, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    public void render(PackUiState pack, UiState state) {
        rendering = true;
        try {
            ModRowModel model = ModRowModel.of(pack, state);
            tabs.setLabel(Tab.PACK, label("mods.tab.pack", model.packCount()));
            tabs.setLabel(Tab.OWN_MODS, label("mods.tab.own", model.ownCount()));
            tabs.setLabel(Tab.CONFIGS, label("mods.tab.configs", model.configCount()));
            tabs.setSelected(state.tab());
            filter.setVisible(model.showsFilter());
            filter.setSelected(state.filter());
            if (!search.getText().equals(state.query())) {
                search.setText(state.query());
            }
            table.setRows(model.rows(), state.expandedRow());
        } finally {
            rendering = false;
        }
    }

    /** "Pack 77" with the count fainter, as in the design. */
    private static String label(String key, int count) {
        return "<html>" + ModRowRenderer.escape(Messages.get(key)) + " <span style='color:"
            + String.format("#%06X", Palette.textFaint().getRGB() & 0xFFFFFF) + "'>" + count + "</span></html>";
    }
}
