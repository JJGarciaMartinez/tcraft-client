package hopsync.ui;

import hopsync.config.AppProperties;
import hopsync.event.EventBus;
import hopsync.event.UpdateEvent;
import hopsync.model.PackInfo;
import hopsync.service.*;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

/**
 * The UIController is responsible for managing the interactions between the user interface (UI)
 * components and the underlying services. It uses the event-based architecture for decoupled
 * communication between layers.
 */
public class UIController {
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
     * @param ui the instance of LauncherUI representing the primary user interface of the application.
     */
    public UIController(LauncherUI ui) {
        this.ui = ui;
        this.eventBus = ServiceFactory.getEventBus();
        this.coordinator = new UpdateCoordinator(eventBus, ServiceFactory.getModUpdateService());
        this.eventHandler = new UpdateEventHandler(ui);
        this.fileSystemService = ServiceFactory.getFileSystemService();
        this.modLoadService = new ModLoadService(fileSystemService);
        this.settingsService = ServiceFactory.getSettingsService();
        this.manifestService = ServiceFactory.getManifestService();

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
                    ui.getHeaderPanel().showPackError(e.getCause().getMessage());
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
        if (event instanceof UpdateEvent.UpdateCompleted || event instanceof UpdateEvent.UpdateCancelled) {
            SwingUtilities.invokeLater(() -> showModCount(modLoadService.loadCurrentMods().size()));
        }
    }

    private void showModCount(int count) {
        ui.getHeaderPanel().updateModsStatus(count + (count == 1 ? " mod instalado" : " mods instalados"));
    }

    /**
     * Cancels the ongoing update operation and reloads the current mods list.
     */
    private void cancelUpdate() {
        coordinator.cancelUpdate();
        loadCurrentMods();
    }

    /**
     * Loads and displays the current mods from the file system.
     */
    private void loadCurrentMods() {
        ui.getModListPanel().clear();
        var mods = modLoadService.loadCurrentMods();
        if (mods.isEmpty()) {
            ui.getModListPanel().showEmpty();
        }
        for (var mod : mods) {
            ui.getModListPanel().addModSync(mod);
            ui.getModListPanel().setModStatus(mod.file(), StatusType.IN_SYSTEM, "Instalado", AppProperties.getAccentSuccess());
        }
        showModCount(mods.size());
    }

    /**
     * Creates and displays the launcher UI with a new controller instance.
     */
    public static void create() {
        LauncherUI ui = new LauncherUI();
        ui.setVisible(true);
        new UIController(ui);
    }
}
