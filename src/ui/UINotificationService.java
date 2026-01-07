package ui;

import javax.swing.*;

/**
 * Centralizes UI notifications and status updates across different panels.
 */
public class UINotificationService {
    private final LauncherUI ui;

    /**
     * Creates a new UINotificationService.
     *
     * @param ui the launcher UI instance
     */
    public UINotificationService(LauncherUI ui) {
        this.ui = ui;
    }

    /**
     * Updates the UI to reflect that mods are being loaded.
     */
    public void notifyLoadingMods() {
        ui.getHeaderPanel().setStatusProcessing();
        ui.getHeaderPanel().updateModsStatus("Refrescando lista...");
        ui.getControlPanel().updateStatus("Actualizando información...");
    }

    /**
     * Updates the UI to show the current mod count.
     *
     * @param modCount the number of mods found
     */
    public void notifyModsLoaded(int modCount) {
        if (modCount > 0) {
            ui.getHeaderPanel().setStatusSuccess();
            ui.getHeaderPanel().updateModsStatus(modCount + (modCount == 1 ? " mod instalado" : " mods instalados"));
        } else {
            ui.getHeaderPanel().setStatusWarning();
            ui.getHeaderPanel().updateModsStatus("No se encontraron mods instalados");
        }
    }

    /**
     * Updates the UI to show a mod loading error.
     */
    public void notifyModLoadError() {
        ui.getHeaderPanel().setStatusError();
        ui.getHeaderPanel().updateModsStatus("Error al cargar mods");
    }

    /**
     * Updates the UI to indicate list refresh completion.
     */
    public void notifyListRefreshed() {
        ui.getControlPanel().updateStatus("Lista actualizada");
    }

    /**
     * Updates the UI to show that an update is starting.
     */
    public void notifyUpdateStarting() {
        ui.getModListPanel().clear();
        ui.getControlPanel().setUpdateInProgress(true);
        ui.getControlPanel().showProgress();
        ui.getControlPanel().updateStatus("Actualizando...");
        ui.getHeaderPanel().updateModsStatus("Actualización en progreso");
        ui.getHeaderPanel().setStatusProcessing();
    }

    /**
     * Updates the UI to show that an update was canceled.
     */
    public void notifyUpdateCanceled() {
        ui.getControlPanel().updateStatus("Actualización cancelada");
        ui.getControlPanel().hideProgress();
        ui.getHeaderPanel().updateModsStatus("Actualizador de Mods");
        ui.getHeaderPanel().setStatusWarning();
        ui.getControlPanel().setUpdateInProgress(false);
    }

    /**
     * Updates the UI to show update completion with errors.
     *
     * @param completed number of completed mods
     * @param failed number of failed mods
     */
    public void notifyUpdateCompletedWithErrors(int completed, int failed) {
        ui.getControlPanel().setUpdateInProgress(false);
        ui.getControlPanel().hideProgress();
        ui.getControlPanel().updateStatus(
                String.format("Completado con errores (%d fallidos)", failed)
        );
        ui.getHeaderPanel().updateModsStatus("Actualización completada con errores");
        ui.getHeaderPanel().setStatusWarning();

        JOptionPane.showMessageDialog(ui,
                String.format(
                        """
                                Actualización completada con algunos errores.

                                Mods exitosos: %d
                                Mods fallidos: %d

                                Revisa los detalles arriba.""",
                        completed, failed
                ),
                "Actualización Completada con Errores",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Updates the UI to show successful update completion.
     *
     * @param completed number of completed mods
     */
    public void notifyUpdateCompletedSuccessfully(int completed) {
        ui.getControlPanel().setUpdateInProgress(false);
        ui.getControlPanel().hideProgress();
        ui.getControlPanel().updateStatus("Actualización completada exitosamente");
        ui.getHeaderPanel().updateModsStatus("Mods actualizados correctamente");
        ui.getHeaderPanel().setStatusSuccess();

        JOptionPane.showMessageDialog(ui, String.format("""
            Todos los mods han sido actualizados correctamente.
            Total de mods: %d""",
            completed
            ), "Actualización Completa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Updates the UI to show a critical error.
     *
     * @param errorMessage the error message to display
     */
    public void notifyCriticalError(String errorMessage) {
        ui.getControlPanel().updateStatus("Error crítico en la actualización");
        ui.getControlPanel().hideProgress();
        ui.getHeaderPanel().updateModsStatus("Ha ocurrido un error durante la actualización: " + errorMessage);
        ui.getHeaderPanel().setStatusError();
        ui.getControlPanel().setUpdateInProgress(false);

        JOptionPane.showMessageDialog(ui,
                "Error crítico durante la actualización:\n" + errorMessage,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Updates the progress bar and status.
     *
     * @param current current progress value
     * @param total total progress value
     */
    public void notifyProgress(int current, int total) {
        int percentage = (int) ((current / (float) total) * 100);
        ui.getControlPanel().updateProgress(percentage);
        ui.getControlPanel().updateStatus(
                String.format("Progreso: %d/%d", current, total)
        );
    }
}
