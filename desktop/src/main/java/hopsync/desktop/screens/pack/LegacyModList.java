package hopsync.desktop.screens.pack;

import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.ui.ModListPanel;

import java.awt.Color;
import java.util.List;

/**
 * Draws the selected pack's mods on the card list until the mod table replaces it. Compares with what it
 * drew last: the cards are rebuilt only when the set of mods changes, otherwise only changed statuses.
 */
public final class LegacyModList {
    private final ModListPanel list = new ModListPanel();
    private String renderedPack;
    private List<LegacyRows.Row> renderedRows;

    public ModListPanel component() {
        return list;
    }

    public void render(PackUiState pack) {
        List<LegacyRows.Row> rows = LegacyRows.of(pack);
        if (!pack.packId().equals(renderedPack) || !sameCards(renderedRows, rows)) {
            list.clear();
            if (rows.isEmpty()) {
                list.showEmpty();
            }
            for (LegacyRows.Row row : rows) {
                list.addModSync(row.info());
                show(row);
            }
        } else {
            for (int i = 0; i < rows.size(); i++) {
                if (!rows.get(i).equals(renderedRows.get(i))) {
                    show(rows.get(i));
                }
            }
        }
        renderedPack = pack.packId();
        renderedRows = rows;
    }

    private static boolean sameCards(List<LegacyRows.Row> before, List<LegacyRows.Row> rows) {
        if (before == null || before.size() != rows.size()) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (!before.get(i).info().equals(rows.get(i).info())) return false;
        }
        return true;
    }

    private void show(LegacyRows.Row row) {
        list.setModStatus(row.info().file(), row.type(), row.text(), color(row.tone()));
    }

    private static Color color(LegacyRows.Tone tone) {
        return switch (tone) {
            case ACCENT -> Palette.accent();
            case ADDED -> Palette.added();
            case UPDATE -> Palette.update();
            case ERROR -> Palette.error();
        };
    }
}
