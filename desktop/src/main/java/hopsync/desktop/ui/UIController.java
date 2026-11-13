package hopsync.desktop.ui;

import hopsync.core.event.EventBus;
import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.PackInfo;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.core.session.PackSession;
import hopsync.core.session.PackSessions;
import hopsync.desktop.AppContainer;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.kit.Palette;
import hopsync.platform.fs.DefaultGameDir;
import hopsync.platform.manifest.ManifestService;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.concurrent.ExecutionException;

/**
 * The UIController is responsible for managing the interactions between the user interface (UI)
 * components and the underlying services. It uses the event-based architecture for decoupled
 * communication between layers.
 * <p>
 * Until the pack library exists the window shows only the active pack: its session drives the buttons
 * and events from other packs are ignored.
 */
public class UIController {
    private static final System.Logger LOG = System.getLogger(UIController.class.getName());

    private final LauncherUI ui;
    private final AppContainer container;
    private final EventBus eventBus;
    private final PackRegistry registry;
    private final PackSessions sessions;
    private final UpdateEventHandler eventHandler;
    private final ModLoadService modLoadService = new ModLoadService();
    private final ManifestService manifestService;

    /**
     * Constructs a new instance of the UIController class, which serves as the controller
     * for coordinating interactions between the LauncherUI and underlying services.
     *
     * @param ui        the instance of LauncherUI representing the primary user interface of the application.
     * @param container the services the UI works with
     */
    public UIController(LauncherUI ui, AppContainer container) {
        this.ui = ui;
        this.container = container;
        this.eventBus = container.eventBus();
        this.registry = container.packRegistry();
        this.sessions = container.packSessions();
        this.eventHandler = new UpdateEventHandler(ui);
        this.manifestService = container.manifestService();

        eventBus.subscribe(event -> {
            if (isActivePack(event)) {
                eventHandler.handleEvent(event);
                askForConfirmation(event);
                refreshModCountWhenDone(event);
            }
        });
        setupListeners();
        SwingUtilities.invokeLater(() -> {
            loadCurrentMods();
            if (!registry.isEmpty() || !openSettings(true)) {
                loadPackInfo();
            }
        });
    }

    /**
     * Configures the action listeners for the control panel buttons in the user interface.
     */
    private void setupListeners() {
        ui.getControlPanel().setUpdateButtonListener(e ->
            activeSession().ifPresentOrElse(PackSession::startUpdate, () -> openSettings(true)));
        ui.getControlPanel().setCancelButtonListener(e -> cancelUpdate());
        ui.getControlPanel().setRefreshButtonListener(e -> loadCurrentMods());
        ui.getControlPanel().setSettingsButtonListener(e -> openSettings(false));
    }

    /**
     * Opens the settings dialog and reloads everything that depends on the settings if they changed.
     *
     * @return true if new settings were saved
     */
    private boolean openSettings(boolean firstRun) {
        if (isRunning()) return false;
        boolean saved = SettingsDialog.show(ui, registry, manifestService, container.presetManifestUrl(),
            container.settingsFile(), DefaultGameDir.detect(), firstRun);
        if (saved) {
            activePack().ifPresent(pack -> ui.getHeaderPanel().setGameDir(pack.gameDir().toFile()));
            loadCurrentMods();
            loadPackInfo();
        }
        return saved;
    }

    /**
     * Fetches the manifest in the background to show the pack in the header.
     */
    private void loadPackInfo() {
        Optional<Pack> pack = activePack();
        new SwingWorker<PackInfo, Void>() {
            @Override
            protected PackInfo doInBackground() throws Exception {
                Manifest manifest = manifestService.fetchManifest(pack.map(Pack::manifestUrl).orElse(""));
                pack.ifPresent(p -> registry.rememberName(p.id(), manifest.pack().name()));
                return manifest.pack();
            }

            @Override
            protected void done() {
                try {
                    ui.getHeaderPanel().showPack(get());
                } catch (ExecutionException e) {
                    ui.getHeaderPanel().showPackError(Messages.failure(SyncFailures.from(e.getCause(), "")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /**
     * Shows the plan to the player once it is ready; nothing is written until they accept it.
     */
    private void askForConfirmation(SyncEvent event) {
        if (event instanceof SyncEvent.PlanReady ready) {
            SwingUtilities.invokeLater(() -> {
                PackSession session = sessions.get(ready.packId());
                if (SyncPlanDialog.confirm(ui, ready.plan())) {
                    session.confirmPlan();
                } else {
                    session.cancelUpdate();
                }
            });
        }
    }

    private void refreshModCountWhenDone(SyncEvent event) {
        if (event instanceof SyncEvent.UpdateCompleted) {
            SwingUtilities.invokeLater(() -> withInstalledMods(mods -> showModCount(mods.size())));
        } else if (event instanceof SyncEvent.UpdateCancelled || event instanceof SyncEvent.UpdateFailed) {
            // Back to what is on disk once the worker has actually stopped
            SwingUtilities.invokeLater(this::loadCurrentMods);
        }
    }

    private void showModCount(int count) {
        ui.getHeaderPanel().updateModsStatus(Messages.get("mods.installedCount", count));
    }

    /**
     * Cancels the ongoing update; the mods list is reloaded when UpdateCancelled arrives.
     */
    private void cancelUpdate() {
        activeSession().ifPresent(PackSession::cancelUpdate);
    }

    /**
     * Loads and displays the current mods from the file system.
     */
    private void loadCurrentMods() {
        withInstalledMods(mods -> {
            // An update that started meanwhile owns the list
            if (isRunning()) return;
            ui.getModListPanel().clear();
            if (mods.isEmpty()) {
                ui.getModListPanel().showEmpty();
            }
            for (var mod : mods) {
                ui.getModListPanel().addModSync(mod);
                ui.getModListPanel().setModStatus(mod.file(), StatusType.IN_SYSTEM, Messages.get("mod.installed"), Palette.accent());
            }
            showModCount(mods.size());
        });
    }

    /**
     * Lists the jars in the mods folder off the EDT, then hands them to {@code onLoaded} on the EDT.
     */
    private void withInstalledMods(Consumer<List<ModInfo>> onLoaded) {
        Optional<Path> modsFolder = activePack().map(pack -> pack.paths().mods());
        new SwingWorker<List<ModInfo>, Void>() {
            @Override
            protected List<ModInfo> doInBackground() {
                return modsFolder.map(modLoadService::loadCurrentMods).orElse(List.of());
            }

            @Override
            protected void done() {
                try {
                    onLoaded.accept(get());
                } catch (ExecutionException e) {
                    LOG.log(System.Logger.Level.WARNING, "Could not list installed mods", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private Optional<Pack> activePack() {
        return registry.active();
    }

    private Optional<PackSession> activeSession() {
        return activePack().map(pack -> sessions.get(pack.id()));
    }

    private boolean isActivePack(SyncEvent event) {
        return activePack().map(pack -> pack.id().equals(event.packId())).orElse(false);
    }

    private boolean isRunning() {
        return activeSession().map(PackSession::isRunning).orElse(false);
    }

    /**
     * Creates and displays the launcher UI with a new controller instance.
     */
    public static void create(AppContainer container) {
        HopSyncDark.setup();
        LauncherUI ui = new LauncherUI(container.packRegistry().active().map(Pack::gameDir).orElse(null));
        ui.setVisible(true);
        new UIController(ui, container);
    }
}
