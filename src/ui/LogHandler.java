package ui;

import util.LogMessageParser;

/**
 * Handles log messages from the update process and updates the UI accordingly.
 */
public class LogHandler {
    private final LauncherUI ui;
    private final UpdateStateManager stateManager;

    /**
     * Creates a new LogHandler.
     *
     * @param ui the launcher UI instance
     * @param stateManager the update state manager
     */
    public LogHandler(LauncherUI ui, UpdateStateManager stateManager) {
        this.ui = ui;
        this.stateManager = stateManager;
    }

    /**
     * Processes a log message and updates the UI based on its content.
     *
     * @param message the log message to process
     */
    public void handleLog(String message) {
        System.out.println(message); // For debugging

        // Ignore verification/system messages
        if (message.contains("Verificando") || message.contains("---")) {
            return;
        }

        if (message.contains("Descargando:")) {
            handleDownloading(message);
        } else if (message.contains("Completado:")) {
            handleCompleted(message);
        } else if (message.contains("OK:")) {
            handleAlreadyInstalled(message);
        } else if (message.contains("Error en")) {
            handleModError(message);
        } else if (message.contains("ERROR CRÍTICO") || message.contains("error") || message.contains("Error")) {
            stateManager.setHasErrors(true);
        }
    }

    /**
     * Handles a "Descargando:" log message.
     */
    private void handleDownloading(String message) {
        String modName = LogMessageParser.extractModName(message);
        if (modName != null) {
            System.out.println("🔄 Actualizando estado 'Descargando' para: '" + modName + "'");
            ui.getModListPanel().setModDownloading(modName);
        }
    }

    /**
     * Handles a "Completado:" log message.
     */
    private void handleCompleted(String message) {
        String modName = LogMessageParser.extractModName(message);
        if (modName != null) {
            System.out.println("🔄 Actualizando estado 'Completado' para: '" + modName + "'");
            ui.getModListPanel().setModCompleted(modName, true);
            stateManager.incrementCompleted();
        }
    }

    /**
     * Handles an "OK:" log message (mod already installed).
     */
    private void handleAlreadyInstalled(String message) {
        String modName = LogMessageParser.extractModName(message);
        if (modName != null) {
            System.out.println("🔄 Actualizando estado 'OK' para: '" + modName + "'");
            ui.getModListPanel().setModChecking(modName);
            ui.getModListPanel().setModAlreadyInstalled(modName);
            stateManager.incrementCompleted();
        }
    }

    /**
     * Handles an "Error en" log message.
     */
    private void handleModError(String message) {
        String modName = LogMessageParser.extractModNameFromError(message);
        if (modName != null) {
            ui.getModListPanel().setModCompleted(modName, false);
            stateManager.incrementFailed();
            stateManager.setHasErrors(true);
        }
    }
}
