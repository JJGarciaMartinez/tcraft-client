package hopsync.desktop.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the window shows. The single source of truth for rendering: views read it, never the core.
 *
 * @param packs          by pack id, in the order the player added them
 * @param selectedPackId the pack shown in the main area, or {@code null} when there are no packs
 * @param query          search text of the mod list, never {@code null}
 * @param expandedRow    file of the expanded mod row, or {@code null}
 * @param sheet          the dialog or sheet open on top of the window
 */
public record UiState(Map<String, PackUiState> packs, String selectedPackId, Tab tab, Filter filter, String query,
                      String expandedRow, Sheet sheet) {

    public enum Tab { PACK, OWN_MODS, CONFIGS }

    public enum Filter { CHANGES, ALL }

    public enum Sheet { NONE, REVIEW, SETTINGS, SETTINGS_FIRST_RUN }

    public static final UiState INITIAL = new UiState(Map.of(), null, Tab.PACK, Filter.ALL, "", null, Sheet.NONE);

    public UiState {
        packs = Collections.unmodifiableMap(new LinkedHashMap<>(packs));
        query = query == null ? "" : query;
    }

    public Optional<PackUiState> selected() {
        return Optional.ofNullable(selectedPackId).map(packs::get);
    }

    UiState withPacks(Map<String, PackUiState> newPacks, String newSelected) {
        return new UiState(newPacks, newSelected, tab, filter, query, expandedRow, sheet);
    }

    UiState withPack(PackUiState pack) {
        Map<String, PackUiState> next = new LinkedHashMap<>(packs);
        next.put(pack.packId(), pack);
        return withPacks(next, selectedPackId);
    }

    UiState withSelected(String packId) {
        return new UiState(packs, packId, tab, filter, query, null, sheet);
    }

    UiState withTab(Tab newTab) {
        return new UiState(packs, selectedPackId, newTab, filter, query, expandedRow, sheet);
    }

    UiState withFilter(Filter newFilter) {
        return new UiState(packs, selectedPackId, tab, newFilter, query, expandedRow, sheet);
    }

    UiState withQuery(String newQuery) {
        return new UiState(packs, selectedPackId, tab, filter, newQuery, expandedRow, sheet);
    }

    UiState withExpandedRow(String file) {
        return new UiState(packs, selectedPackId, tab, filter, query, file, sheet);
    }

    UiState withSheet(Sheet newSheet) {
        return new UiState(packs, selectedPackId, tab, filter, query, expandedRow, newSheet);
    }
}
