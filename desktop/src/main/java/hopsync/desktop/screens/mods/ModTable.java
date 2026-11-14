package hopsync.desktop.screens.mods;

import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.UiAction;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * The mod list as one {@link JList}: rows are painted by a renderer instead of being live components, so
 * a pack of hundreds of mods scrolls smoothly. Clicks are hit-tested here: a mod row expands or collapses,
 * "Ver todos" lines run their action and the link of an expanded row opens the mod's page.
 */
final class ModTable extends JScrollPane {
    private final DefaultListModel<ModRow> model = new DefaultListModel<>();
    private final JList<ModRow> list = new JList<>(model) {
        // Rows always take the visible width; a long detail line is cut instead of scrolling sideways
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
    };
    private final ModRowRenderer renderer = new ModRowRenderer();

    ModTable(Consumer<UiAction> dispatch) {
        setViewportView(list);
        setBorder(null);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setUnitIncrement(UIScale.scale(ModRowRenderer.ROW_HEIGHT));
        list.setBackground(Palette.background());
        list.setCellRenderer(renderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = indexAt(e.getPoint());
                list.setCursor(index >= 0 && clickable(model.get(index))
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                if (renderer.hoverIndex() != index) {
                    renderer.setHoverIndex(index);
                    list.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setHoverIndex(-1);
                list.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = indexAt(e.getPoint());
                if (index < 0) return;
                switch (model.get(index)) {
                    case ModRow.Mod row when row.statusAction() != null && onStatusButton(index, row, e.getPoint()) ->
                        dispatch.accept(row.statusAction());
                    case ModRow.Mod row when row.expandable() -> dispatch.accept(new UiAction.ToggleRow(row.key()));
                    case ModRow.More row -> dispatch.accept(row.action());
                    case ModRow.Detail row when row.url() != null && onLink(index, row, e.getPoint()) ->
                        dispatch.accept(new UiAction.OpenModPage(row.url()));
                    default -> { }
                }
            }
        };
        list.addMouseListener(mouse);
        list.addMouseMotionListener(mouse);
    }

    void setRows(List<ModRow> rows, String expandedKey) {
        renderer.setExpandedKey(expandedKey);
        if (!sameRows(rows)) {
            model.clear();
            model.addAll(rows);
        } else {
            list.repaint();
        }
    }

    /** The index under the point, or -1 below the last row (JList otherwise returns the last one). */
    private int indexAt(Point point) {
        int index = list.locationToIndex(point);
        return index >= 0 && list.getCellBounds(index, index).contains(point) ? index : -1;
    }

    private boolean onStatusButton(int index, ModRow.Mod row, Point point) {
        Rectangle cell = list.getCellBounds(index, index);
        return renderer.statusBounds(row, cell.width).contains(point.x - cell.x, point.y - cell.y);
    }

    private boolean onLink(int index, ModRow.Detail row, Point point) {
        Rectangle cell = list.getCellBounds(index, index);
        Rectangle link = renderer.linkBounds(row, cell.width);
        return link.contains(point.x - cell.x, point.y - cell.y);
    }

    private static boolean clickable(ModRow row) {
        return row instanceof ModRow.Mod mod && mod.expandable() || row instanceof ModRow.More
            || row instanceof ModRow.Detail detail && detail.url() != null;
    }

    private boolean sameRows(List<ModRow> rows) {
        if (model.size() != rows.size()) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (!model.get(i).equals(rows.get(i))) return false;
        }
        return true;
    }
}
