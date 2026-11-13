package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.PackInfo;
import hopsync.core.pack.Pack;

import java.time.Instant;
import java.util.List;

/**
 * Everything that can change the UI state: events from the core, data loaded in the background and what
 * the player does. Views only dispatch actions; {@code Effects} turns the ones with side effects into
 * calls to the core.
 */
public sealed interface UiAction {

    // From the core and background loads

    /** An event of any pack's session; the reducer switches over {@link SyncEvent} exhaustively. */
    record SyncEventReceived(SyncEvent event, Instant at) implements UiAction {}

    /** The packs in the registry, after startup or after the settings changed. */
    record PacksLoaded(List<Pack> packs, String activeId) implements UiAction {}

    record InstalledModsLoaded(String packId, List<String> files) implements UiAction {}

    record PackInfoLoaded(String packId, PackInfo info) implements UiAction {}

    record PackInfoFailed(String packId, SyncFailure failure) implements UiAction {}

    // What the player does with a pack

    record CheckPack(String packId) implements UiAction {}

    record ConfirmPlan(String packId) implements UiAction {}

    record DismissPlan(String packId) implements UiAction {}

    record CancelUpdate(String packId) implements UiAction {}

    record ReloadInstalled(String packId) implements UiAction {}

    // Dialogs

    record OpenSettings() implements UiAction {}

    record SettingsClosed(boolean saved) implements UiAction {}

    // Navigation, only UI state

    record SelectPack(String packId) implements UiAction {}

    record SelectTab(UiState.Tab tab) implements UiAction {}

    record SetFilter(UiState.Filter filter) implements UiAction {}

    record SetQuery(String query) implements UiAction {}

    /** Expands the row of the file, or collapses it if it was expanded. */
    record ToggleRow(String file) implements UiAction {}
}
