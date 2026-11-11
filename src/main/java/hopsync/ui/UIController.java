package hopsync.ui;

import hopsync.event.EventBus;
import hopsync.event.UpdateEvent;
import hopsync.service.*;

import javax.swing.*;

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
        this.modLoadService = new ModLoadService(ServiceFactory.getFileSystemService());

        eventBus.subscribe(eventHandler::handleEvent);
        eventBus.subscribe(this::askForConfirmation);
        setupListeners();
        SwingUtilities.invokeLater(this::loadCurrentMods);
    }

    /**
     * Configures the action listeners for the control panel buttons in the user interface.
     */
    private void setupListeners() {
        ui.getControlPanel().setUpdateButtonListener(e -> coordinator.startUpdate());
        ui.getControlPanel().setCancelButtonListener(e -> cancelUpdate());
        ui.getControlPanel().setRefreshButtonListener(e -> loadCurrentMods());
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
        for (var mod : modLoadService.loadCurrentMods()) {
            ui.getModListPanel().addModSync(mod);
        }
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
