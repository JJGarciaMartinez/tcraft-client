package ui;

import model.ModInfo;
import service.ModUpdater;

import javax.swing.*;

/**
 * The UIController is responsible for managing the interactions between the user interface (UI)
 * components and the underlying services, specifically handling the process of loading, refreshing,
 * and updating mods. It serves as the primary controller for managing user actions and system updates
 * within the mod-launching application.
 */
public class UIController {
    private final LauncherUI ui;
    private final UpdateStateManager stateManager;
    private final UINotificationService notificationService;
    private final ModLoadService modLoadService;
    private final LogHandler logHandler;

    /**
     * Constructs a new instance of the UIController class, which serves as the controller
     * for coordinating interactions between the LauncherUI and underlying services. This class
     * initializes the necessary services and sets up event listeners to handle user interactions
     * and mod management operations.
     *
     * @param ui the instance of LauncherUI representing the primary user interface of the application.
     */
    public UIController(LauncherUI ui) {
        this.ui = ui;
        this.stateManager = new UpdateStateManager();
        this.notificationService = new UINotificationService(ui);
        this.modLoadService = new ModLoadService(ui, notificationService);
        this.logHandler = new LogHandler(ui, stateManager);

        setupListeners();

        // Load mods before showing UI
        SwingUtilities.invokeLater(modLoadService::loadCurrentMods);
    }


    /**
     * Configures the action listeners for the control panel buttons in the user interface.
     * This method assigns specific operations to be executed when each button is clicked:
     * - The update button triggers the {@code startUpdate()} method to initiate the update process.
     * - The cancel button triggers the {@code cancelUpdate()} method to abort an ongoing update operation.
     * - The refresh button triggers the {@code refreshModList()} method to reload and display the updated list of mods.
     * <p>
     * The listeners ensure that the UI responds appropriately to user interactions on the
     * control panel, facilitating update management and mod list refresh functionality.
     */
    private void setupListeners() {
        ui.getControlPanel().setUpdateButtonListener(_ -> startUpdate());
        ui.getControlPanel().setCancelButtonListener(_ -> cancelUpdate());
        ui.getControlPanel().setRefreshButtonListener(_ -> refreshModList());
    }

    /**
     * Refreshes the mod list displayed in the user interface.
     */
    private void refreshModList() {
        modLoadService.refreshModList();
    }

    /**
     * Initiates the update process by resetting UI components, clearing counters,
     * and starting a new thread to execute the update operation.
     */
    private void startUpdate() {
        notificationService.notifyUpdateStarting();
        stateManager.reset();

        Thread updateThread = new Thread(this::executeUpdate);
        stateManager.setUpdateThread(updateThread);
        updateThread.start();
    }

    /**
     * Cancels the ongoing update operation if it is currently in progress.
     */
    private void cancelUpdate() {
        if (stateManager.isUpdateRunning()) {
            stateManager.cancelUpdate();
            notificationService.notifyUpdateCanceled();
            modLoadService.loadCurrentMods();
        }
    }

    /**
     * Executes the update process for modifications (mods).
     */
    private void executeUpdate() {
        try {
            ModUpdater updater = new ModUpdater(
                    logHandler::handleLog,
                    notificationService::notifyProgress,
                    this::onModInfo
            );
            updater.initUpdate();

            SwingUtilities.invokeLater(() -> {
                if (stateManager.hasErrors() || stateManager.getFailedCount() > 0) {
                    notificationService.notifyUpdateCompletedWithErrors(
                            stateManager.getCompletedCount(),
                            stateManager.getFailedCount()
                    );
                } else {
                    notificationService.notifyUpdateCompletedSuccessfully(
                            stateManager.getCompletedCount()
                    );
                }
            });

        } catch (Exception e) {
            System.err.println("Error crítico durante la actualización: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.getClass().getSimpleName();
                }

                notificationService.notifyCriticalError(errorMessage);
                modLoadService.loadCurrentMods();
            });
        }
    }

    /**
     * Handles the modification information by adding it to the Mod List Panel
     * for synchronization, ensuring that the card is created before log processing.
     *
     * @param modInfo The modification information object containing details
     *                about the specific modification to process.
     */
    private void onModInfo(ModInfo modInfo) {
        ui.getModListPanel().addModSync(modInfo);
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