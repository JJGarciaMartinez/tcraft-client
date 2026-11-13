package hopsync.desktop.effects;

import hopsync.core.event.EventBus;
import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.Manifest;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.core.session.PackSessions;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.platform.manifest.ManifestService;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * The only bridge between the UI and the core: turns core events into actions, and actions with side
 * effects (check, confirm, cancel, remove, load from disk or network, open folders, copy to the clipboard)
 * into calls to the core or the desktop. Screens never call the core themselves.
 */
public final class Effects {
    private static final System.Logger LOG = System.getLogger(Effects.class.getName());

    private final Store store;
    private final PackRegistry registry;
    private final PackSessions sessions;
    private final ManifestService manifests;
    private final ModLoadService modLoadService;
    private final Executor background;
    private final Clock clock;

    public Effects(Store store, PackRegistry registry, PackSessions sessions, ManifestService manifests,
                   ModLoadService modLoadService, Executor background, Clock clock) {
        this.store = store;
        this.registry = registry;
        this.sessions = sessions;
        this.manifests = manifests;
        this.modLoadService = modLoadService;
        this.background = background;
        this.clock = clock;
    }

    /** Starts listening to the core and the store, then loads the packs. */
    public void start(EventBus events) {
        events.subscribe(event -> store.dispatch(new UiAction.SyncEventReceived(event, clock.instant())));
        store.addEffect(this::run);
        reloadPacks();
    }

    private void run(UiAction action, UiState state) {
        switch (action) {
            case UiAction.CheckPack a -> sessions.get(a.packId()).startUpdate();
            case UiAction.CheckAll a -> state.packs().values().stream()
                .filter(pack -> !pack.phase().isBusy())
                .forEach(pack -> sessions.get(pack.packId()).startUpdate());
            case UiAction.ConfirmPlan a -> sessions.get(a.packId()).confirmPlan();
            case UiAction.DismissPlan a -> sessions.get(a.packId()).cancelUpdate();
            case UiAction.CancelUpdate a -> sessions.get(a.packId()).cancelUpdate();
            case UiAction.ReloadInstalled a -> loadInstalled(a.packId());
            case UiAction.SelectPack a -> {
                remember(a.packId());
                loadInstalled(a.packId());
                loadManifest(a.packId());
            }
            case UiAction.OpenFolder a -> openFolder(a.packId());
            case UiAction.CopyServerIp a -> Optional.ofNullable(state.packs().get(a.packId()))
                .map(PackUiState::packInfo)
                .filter(info -> info.serverIp() != null && !info.serverIp().isBlank())
                .ifPresent(info -> copy(info.serverIp(), Messages.get("toast.ipCopied")));
            case UiAction.CopyPackLink a -> registry.find(a.packId())
                .ifPresent(pack -> copy(pack.manifestUrl(), Messages.get("toast.linkCopied")));
            case UiAction.RemovePack a -> removePack(a.packId(), state);
            case UiAction.OpenModPage a -> openModPage(a.url());
            case UiAction.SheetClosed a when a.changed() -> reloadPacks();
            case UiAction.SyncEventReceived a when finishesUpdate(a.event()) ->
                // Back to what is on disk once the worker has stopped
                loadInstalled(a.event().packId());
            default -> { }
        }
    }

    private static boolean finishesUpdate(SyncEvent event) {
        return event instanceof SyncEvent.UpdateCompleted || event instanceof SyncEvent.UpdateCancelled
            || event instanceof SyncEvent.UpdateFailed;
    }

    private void reloadPacks() {
        Optional<Pack> active = registry.active();
        store.dispatch(new UiAction.PacksLoaded(registry.packs(), active.map(Pack::id).orElse(null)));
        active.ifPresent(pack -> {
            loadInstalled(pack.id());
            loadManifest(pack.id());
        });
    }

    /** The selected pack is the one shown first on the next start. */
    private void remember(String packId) {
        try {
            registry.select(packId);
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not save the selected pack " + packId, e);
        }
    }

    private void removePack(String packId, UiState state) {
        String name = Optional.ofNullable(state.packs().get(packId)).map(PackUiState::displayName).orElse(packId);
        sessions.drop(packId);
        Optional<String> link = registry.find(packId).map(Pack::manifestUrl);
        try {
            registry.remove(packId);
            // The cached manifest is HopSync's own file, unless another pack still uses the same link
            link.filter(url -> registry.packs().stream().noneMatch(other -> other.manifestUrl().equals(url)))
                .ifPresent(manifests::forget);
            store.dispatch(new UiAction.ShowToast(Messages.get("toast.removed", name)));
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not remove pack " + packId, e);
            store.dispatch(new UiAction.ShowToast(Messages.failure(SyncFailures.from(e, ""))));
        }
        reloadPacks();
    }

    private void openFolder(String packId) {
        registry.find(packId).ifPresent(pack -> background.execute(() -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    throw new IOException("opening folders is not supported here");
                }
                Desktop.getDesktop().open(pack.gameDir().toFile());
            } catch (IOException | RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not open " + pack.gameDir(), e);
                store.dispatch(new UiAction.ShowToast(Messages.get("toast.folderFailed")));
            }
        }));
    }

    private void copy(String text, String notice) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        store.dispatch(new UiAction.ShowToast(notice));
    }

    private void loadInstalled(String packId) {
        registry.find(packId).ifPresent(pack -> background.execute(() -> {
            try {
                store.dispatch(new UiAction.InstalledModsLoaded(packId, modLoadService.load(pack.paths())));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not list installed mods of pack " + packId, e);
            }
        }));
    }

    /** The cached manifest first, so the list shows right away, then the current one from the network. */
    private void loadManifest(String packId) {
        registry.find(packId).ifPresent(pack -> background.execute(() -> {
            manifests.cached(pack.manifestUrl())
                .ifPresent(cached -> store.dispatch(new UiAction.ManifestLoaded(packId, cached)));
            try {
                Manifest manifest = manifests.fetchManifest(pack.manifestUrl());
                registry.rememberName(packId, manifest.pack().name());
                store.dispatch(new UiAction.ManifestLoaded(packId, manifest));
            } catch (Exception e) {
                store.dispatch(new UiAction.ManifestFailed(packId, SyncFailures.from(e, "")));
            }
        }));
    }

    private void openModPage(String url) {
        if (url == null || !url.startsWith("https://")) return;
        background.execute(() -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IOException("opening links is not supported here");
                }
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException | RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not open " + url, e);
                store.dispatch(new UiAction.ShowToast(Messages.get("toast.linkFailed")));
            }
        });
    }
}
