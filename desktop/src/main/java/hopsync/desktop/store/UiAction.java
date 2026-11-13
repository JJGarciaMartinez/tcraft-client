package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
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

    record InstalledModsLoaded(String packId, InstalledMods mods) implements UiAction {}

    /** The pack's manifest, from the cache at startup and then from the network. */
    record ManifestLoaded(String packId, Manifest manifest) implements UiAction {}

    record ManifestFailed(String packId, SyncFailure failure) implements UiAction {}

    // What the player does with a pack

    record CheckPack(String packId) implements UiAction {}

    /** Checks every pack that is not already busy. */
    record CheckAll() implements UiAction {}

    record ConfirmPlan(String packId) implements UiAction {}

    record DismissPlan(String packId) implements UiAction {}

    /** Stops the check or apply, or takes the pack out of the apply queue. */
    record CancelUpdate(String packId) implements UiAction {}

    record ReloadInstalled(String packId) implements UiAction {}

    record OpenFolder(String packId) implements UiAction {}

    record CopyServerIp(String packId) implements UiAction {}

    record CopyPackLink(String packId) implements UiAction {}

    /** Opens a mod's page (Modrinth or the pack's repository) in the browser. */
    record OpenModPage(String url) implements UiAction {}

    /** Forgets the pack after the player confirmed; its files stay where they are. */
    record RemovePack(String packId) implements UiAction {}

    // Dialogs

    /** Settings of the selected pack (or the first-run settings when there are no packs). */
    record OpenSettings() implements UiAction {}

    record AddPack() implements UiAction {}

    /** Asks the player to confirm removing the selected pack. */
    record AskRemovePack() implements UiAction {}

    record OpenAbout() implements UiAction {}

    /** @param changed the dialog saved something, so the packs must be reloaded */
    record SheetClosed(boolean changed) implements UiAction {}

    /** A short notice at the bottom of the window. */
    record ShowToast(String text) implements UiAction {}

    // Navigation, only UI state

    record SelectPack(String packId) implements UiAction {}

    record SelectTab(UiState.Tab tab) implements UiAction {}

    record SetFilter(UiState.Filter filter) implements UiAction {}

    record SetQuery(String query) implements UiAction {}

    /** Expands the row of the file, or collapses it if it was expanded. */
    record ToggleRow(String file) implements UiAction {}
}
