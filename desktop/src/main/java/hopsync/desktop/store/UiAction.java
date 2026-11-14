package hopsync.desktop.store;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.Manifest;
import hopsync.core.pack.Pack;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Everything that can change the UI state: events from the core, data loaded in the background and what
 * the player does. Views only dispatch actions; {@code Effects} turns the ones with side effects into
 * calls to the core.
 */
public sealed interface UiAction {

    // From the core and background loads

    /** An event of any pack's session; the reducer switches over {@link SyncEvent} exhaustively. */
    record SyncEventReceived(SyncEvent event, Instant at) implements UiAction {}

    /** The packs in the registry and the app preferences, after startup or after the settings changed. */
    record PacksLoaded(List<Pack> packs, String activeId, Preferences preferences) implements UiAction {
        public PacksLoaded(List<Pack> packs, String activeId) {
            this(packs, activeId, Preferences.DEFAULT);
        }
    }

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

    /**
     * Retries failed downloads or continues a cancelled update: the plan is computed again and applied
     * without a new review when it only touches {@code files}.
     */
    record ContinueUpdate(String packId, Set<String> files) implements UiAction {
        public ContinueUpdate {
            files = Set.copyOf(files);
        }
    }

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

    // Sheets

    /** Settings of the selected pack (or the add-pack wizard when there are no packs). */
    record OpenSettings() implements UiAction {}

    record AddPack() implements UiAction {}

    /** The selected pack's settings, on the section that removes it. */
    record AskRemovePack() implements UiAction {}

    record OpenAppSettings(Sheet.AppSettings.Section section) implements UiAction {}

    /** Esc or "Cancelar"; the first-run wizard cannot be closed. */
    record CloseSheet() implements UiAction {}

    /** A sheet saved; the packs are reloaded by Effects. */
    record SheetSaved() implements UiAction {}

    record SheetFailed(SyncFailure failure) implements UiAction {}

    record SetLinkUrl(String url) implements UiAction {}

    /** Validates the link typed in the open sheet and, if it can be a pack, fetches its manifest. */
    record CheckLink() implements UiAction {}

    record LinkChecked(String url, Manifest manifest) implements UiAction {}

    record LinkFailed(String url, SyncFailure failure) implements UiAction {}

    record WizardNext() implements UiAction {}

    record WizardBack() implements UiAction {}

    record FoldersFound(List<GameFolder> folders) implements UiAction {}

    /** A folder picked in the wizard or the pack settings; Effects then checks who uses it. */
    record ChooseFolder(Path path) implements UiAction {}

    record FolderChecked(GameFolder folder) implements UiAction {}

    record FinishAddPack() implements UiAction {}

    record SelectPackSection(Sheet.PackSettings.Section section) implements UiAction {}

    record SavePackSettings() implements UiAction {}

    record SetPreferences(Preferences preferences) implements UiAction {}

    record SaveAppSettings() implements UiAction {}

    /** Copies version, system and paths for a bug report. */
    record CopyDiagnostics() implements UiAction {}

    /** Closes HopSync: stops whatever is running (safe: the state always matches the disk) and exits. */
    record Quit() implements UiAction {}

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
