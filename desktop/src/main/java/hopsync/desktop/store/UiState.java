package hopsync.desktop.store;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the window shows. The single source of truth for rendering: views read it, never the core.
 *
 * @param packs          by pack id, in the order the player added them
 * @param selectedPackId the pack shown in the main area, or {@code null} when there are no packs
 * @param queries        search text of the mod list, per tab
 * @param expandedRow    file of the expanded mod row, or {@code null}
 * @param sheet          the dialog or sheet open on top of the window
 * @param toast          the last short notice shown, or {@code null}
 */
public record UiState(Map<String, PackUiState> packs, String selectedPackId, Tab tab, Filter filter,
                      Map<Tab, String> queries, String expandedRow, Sheet sheet, Toast toast) {

    public enum Tab { PACK, OWN_MODS, CONFIGS }

    public enum Filter { CHANGES, ALL }

    /** Dialogs; {@code SETTINGS} and {@code REMOVE_PACK} act on the selected pack. */
    public enum Sheet { NONE, SETTINGS, SETTINGS_FIRST_RUN, ADD_PACK, REMOVE_PACK, ABOUT }

    /** @param id grows with every notice, so the same text shown twice still counts as new */
    public record Toast(String text, long id) {
    }

    public static final UiState INITIAL = new UiState(Map.of(), null, Tab.PACK, Filter.CHANGES, Map.of(), null,
        Sheet.NONE, null);

    public UiState {
        packs = Collections.unmodifiableMap(new LinkedHashMap<>(packs));
        queries = Map.copyOf(queries);
    }

    /** Search text of the current tab, never {@code null}. */
    public String query() {
        return queries.getOrDefault(tab, "");
    }

    public Optional<PackUiState> selected() {
        return Optional.ofNullable(selectedPackId).map(packs::get);
    }

    UiState withPacks(Map<String, PackUiState> newPacks, String newSelected) {
        return new UiState(newPacks, newSelected, tab, filter, queries, expandedRow, sheet, toast);
    }

    UiState withPack(PackUiState pack) {
        Map<String, PackUiState> next = new LinkedHashMap<>(packs);
        next.put(pack.packId(), pack);
        return withPacks(next, selectedPackId);
    }

    UiState withSelected(String packId) {
        return new UiState(packs, packId, tab, filter, queries, null, sheet, toast);
    }

    UiState withTab(Tab newTab) {
        return new UiState(packs, selectedPackId, newTab, filter, queries, expandedRow, sheet, toast);
    }

    UiState withFilter(Filter newFilter) {
        return new UiState(packs, selectedPackId, tab, newFilter, queries, expandedRow, sheet, toast);
    }

    UiState withQuery(String newQuery) {
        Map<Tab, String> next = new EnumMap<>(Tab.class);
        next.putAll(queries);
        next.put(tab, newQuery == null ? "" : newQuery);
        return new UiState(packs, selectedPackId, tab, filter, next, expandedRow, sheet, toast);
    }

    UiState withExpandedRow(String file) {
        return new UiState(packs, selectedPackId, tab, filter, queries, file, sheet, toast);
    }

    UiState withToast(String text) {
        return new UiState(packs, selectedPackId, tab, filter, queries, expandedRow, sheet,
            new Toast(text, toast == null ? 1 : toast.id() + 1));
    }

    /** True while any pack other than {@code packId} is writing files. */
    public boolean anotherPackApplying(String packId) {
        return applyingPack().map(pack -> !pack.packId().equals(packId)).orElse(false);
    }

    public Optional<PackUiState> applyingPack() {
        return packs.values().stream().filter(pack -> pack.phase() == PackPhase.APPLYING).findFirst();
    }

    UiState withSheet(Sheet newSheet) {
        return new UiState(packs, selectedPackId, tab, filter, queries, expandedRow, newSheet, toast);
    }
}
