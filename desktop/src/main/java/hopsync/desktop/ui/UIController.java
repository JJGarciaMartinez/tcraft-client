package hopsync.desktop.ui;

import hopsync.core.event.EventBus;
import hopsync.core.event.UpdateEvent;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.PackInfo;
import hopsync.core.session.UpdateCoordinator;
import hopsync.desktop.AppContainer;
import hopsync.desktop.config.AppProperties;
import hopsync.desktop.i18n.Messages;
import hopsync.platform.fs.FileSystemService;
import hopsync.platform.manifest.ManifestService;
import hopsync.platform.settings.SettingsService;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ExecutionException;

/**
 * The UIController is responsible for managing the interactions between the user interface (UI)
 * components and the underlying services. It uses the event-based architecture for decoupled
 * communication between layers.
 */
public class UIController {
    private static final System.Logger LOG = System.getLogger(UIController.class.getName());

    private final LauncherUI ui;
    private final EventBus eventBus;
    private final UpdateCoordinator coordinator;
    private final UpdateEventHandler eventHandler;
    private final ModLoadService modLoadService;
    private final SettingsService settingsService;
    private final ManifestService manifestService;
    private final FileSystemService fileSystemService;

    /**
     * Constructs a new instance of the UIController class, which serves as the controller
     * for coordinating interactions between the LauncherUI and underlying services.
     *
     * @param ui        the instance of LauncherUI representing the primary user interface of the application.
     * @param container the services the UI works with
     */
    public UIController(LauncherUI ui, AppContainer container) {
        this.ui = ui;
        this.eventBus = container.eventBus();
        this.coordinator = container.updateCoordinator();
        this.eventHandler = new UpdateEventHandler(ui);
        this.fileSystemService = container.fileSystemService();
        this.modLoadService = new ModLoadService(fileSystemService);
        this.settingsService = container.settingsService();
        this.manifestService = container.manifestService();

        eventBus.subscribe(eventHandler::handleEvent);
        eventBus.subscribe(this::askForConfirmation);
        eventBus.subscribe(this::refreshModCountWhenDone);
        setupListeners();
        SwingUtilities.invokeLater(() -> {
            loadCurrentMods();
            if (settingsService.isConfigured() || !openSettings(true)) {
                loadPackInfo();
            }
        });
    }

    /**
     * Configures the action listeners for the control panel buttons in the user interface.
     */
    private void setupListeners() {
        ui.getControlPanel().setUpdateButtonListener(e -> coordinator.startUpdate());
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
        if (coordinator.isRunning()) return false;
        boolean saved = SettingsDialog.show(ui, settingsService, manifestService,
            FileSystemService.detectDefaultGameDir(), firstRun);
        if (saved) {
            ui.getHeaderPanel().setGameDir(fileSystemService.getGameDir());
            loadCurrentMods();
            loadPackInfo();
        }
        return saved;
    }

    /**
     * Fetches the manifest in the background to show the pack in the header.
     */
    private void loadPackInfo() {
        new SwingWorker<PackInfo, Void>() {
            @Override
            protected PackInfo doInBackground() throws Exception {
                return manifestService.fetchManifest().pack();
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
    private void askForConfirmation(UpdateEvent event) {
        if (event instanceof UpdateEvent.PlanReady ready) {
            SwingUtilities.invokeLater(() -> {
                if (SyncPlanDialog.confirm(ui, ready.plan())) {
                    coordinator.confirmPlan();
                } else {
                    cancelUpdate();
                }
            });
        }
    }

    private void refreshModCountWhenDone(UpdateEvent event) {
        if (event instanceof UpdateEvent.UpdateCompleted) {
            SwingUtilities.invokeLater(() -> withInstalledMods(mods -> showModCount(mods.size())));
        } else if (event instanceof UpdateEvent.UpdateCancelled || event instanceof UpdateEvent.UpdateFailed) {
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
        coordinator.cancelUpdate();
    }

    /**
     * Loads and displays the current mods from the file system.
     */
    private void loadCurrentMods() {
        withInstalledMods(mods -> {
            // An update that started meanwhile owns the list
            if (coordinator.isRunning()) return;
            ui.getModListPanel().clear();
            if (mods.isEmpty()) {
                ui.getModListPanel().showEmpty();
            }
            for (var mod : mods) {
                ui.getModListPanel().addModSync(mod);
                ui.getModListPanel().setModStatus(mod.file(), StatusType.IN_SYSTEM, Messages.get("mod.installed"), AppProperties.getAccentSuccess());
            }
            showModCount(mods.size());
        });
    }

    /**
     * Lists the jars in the mods folder off the EDT, then hands them to {@code onLoaded} on the EDT.
     */
    private void withInstalledMods(Consumer<List<ModInfo>> onLoaded) {
        new SwingWorker<List<ModInfo>, Void>() {
            @Override
            protected List<ModInfo> doInBackground() {
                return modLoadService.loadCurrentMods();
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

    /**
     * Creates and displays the launcher UI with a new controller instance.
     */
    public static void create(AppContainer container) {
        Theme.install();
        LauncherUI ui = new LauncherUI(container.fileSystemService());
        ui.setVisible(true);
        new UIController(ui, container);
    }
}
