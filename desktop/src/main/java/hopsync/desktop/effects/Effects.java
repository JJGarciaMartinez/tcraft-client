package hopsync.desktop.effects;

import hopsync.core.event.EventBus;
import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.Manifest;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.core.port.ManifestSource;
import hopsync.core.session.PackSessions;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * The only bridge between the UI and the core: turns core events into actions, and actions with side
 * effects (check, confirm, cancel, load from disk or network) into calls to the core. Screens never call
 * the core themselves.
 */
public final class Effects {
    private static final System.Logger LOG = System.getLogger(Effects.class.getName());

    private final Store store;
    private final PackRegistry registry;
    private final PackSessions sessions;
    private final ManifestSource manifests;
    private final ModLoadService modLoadService;
    private final Executor background;
    private final Clock clock;

    public Effects(Store store, PackRegistry registry, PackSessions sessions, ManifestSource manifests,
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
            case UiAction.ConfirmPlan a -> sessions.get(a.packId()).confirmPlan();
            case UiAction.DismissPlan a -> sessions.get(a.packId()).cancelUpdate();
            case UiAction.CancelUpdate a -> sessions.get(a.packId()).cancelUpdate();
            case UiAction.ReloadInstalled a -> loadInstalled(a.packId());
            case UiAction.SelectPack a -> {
                loadInstalled(a.packId());
                loadPackInfo(a.packId());
            }
            case UiAction.SettingsClosed a when a.saved() -> reloadPacks();
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
            loadPackInfo(pack.id());
        });
    }

    private void loadInstalled(String packId) {
        registry.find(packId).ifPresent(pack -> background.execute(() -> {
            try {
                store.dispatch(new UiAction.InstalledModsLoaded(packId,
                    modLoadService.loadCurrentMods(pack.paths().mods())));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not list installed mods of pack " + packId, e);
            }
        }));
    }

    private void loadPackInfo(String packId) {
        registry.find(packId).ifPresent(pack -> background.execute(() -> {
            try {
                Manifest manifest = manifests.fetchManifest(pack.manifestUrl());
                registry.rememberName(packId, manifest.pack().name());
                store.dispatch(new UiAction.PackInfoLoaded(packId, manifest.pack()));
            } catch (Exception e) {
                store.dispatch(new UiAction.PackInfoFailed(packId, SyncFailures.from(e, "")));
            }
        }));
    }
}
