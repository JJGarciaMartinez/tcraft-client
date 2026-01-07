package ui;

import event.UpdateEvent;
import javax.swing.SwingUtilities;

/**
 * Handles update events and translates them to UI updates.
 * Ensures all UI updates happen on the EDT.
 */
public class UpdateEventHandler {
    private final LauncherUI ui;

    public UpdateEventHandler(LauncherUI ui) {
        this.ui = ui;
    }

    public void handleEvent(UpdateEvent event) {
        SwingUtilities.invokeLater(() -> dispatchEvent(event));
    }

    private void dispatchEvent(UpdateEvent event) {
        if (event instanceof UpdateEvent.UpdateStarted) {
            ui.getModListPanel().clear();
            ui.getControlPanel().setUpdateInProgress(true);
            ui.getControlPanel().showProgress();
            ui.getHeaderPanel().setStatusProcessing();
        } else if (event instanceof UpdateEvent.UpdateCompleted) {
            UpdateEvent.UpdateCompleted e = (UpdateEvent.UpdateCompleted) event;
            ui.getControlPanel().setUpdateInProgress(false);
            ui.getControlPanel().hideProgress();
            if (e.failCount() > 0) {
                ui.getHeaderPanel().setStatusWarning();
                ui.getControlPanel().updateStatus("Completado con " + e.failCount() + " errores");
            } else {
                ui.getHeaderPanel().setStatusSuccess();
                ui.getControlPanel().updateStatus("¡Actualización completada!");
            }
        } else if (event instanceof UpdateEvent.UpdateCancelled) {
            ui.getControlPanel().setUpdateInProgress(false);
            ui.getControlPanel().hideProgress();
            ui.getHeaderPanel().setStatusWarning();
            ui.getControlPanel().updateStatus("Actualización cancelada");
        } else if (event instanceof UpdateEvent.UpdateFailed) {
            UpdateEvent.UpdateFailed e = (UpdateEvent.UpdateFailed) event;
            ui.getControlPanel().setUpdateInProgress(false);
            ui.getControlPanel().hideProgress();
            ui.getHeaderPanel().setStatusError();
            ui.getControlPanel().updateStatus("Error: " + e.error());
        } else if (event instanceof UpdateEvent.ProgressChanged) {
            UpdateEvent.ProgressChanged e = (UpdateEvent.ProgressChanged) event;
            int percentage = e.total() > 0 ? (e.current() * 100) / e.total() : 0;
            ui.getControlPanel().updateProgress(percentage);
        } else if (event instanceof UpdateEvent.ModProcessing) {
            UpdateEvent.ModProcessing e = (UpdateEvent.ModProcessing) event;
            ui.getModListPanel().addModSync(e.mod());
        } else if (event instanceof UpdateEvent.ModDownloading) {
            UpdateEvent.ModDownloading e = (UpdateEvent.ModDownloading) event;
            ui.getModListPanel().setModDownloading(e.modName());
        } else if (event instanceof UpdateEvent.ModCompleted) {
            UpdateEvent.ModCompleted e = (UpdateEvent.ModCompleted) event;
            ui.getModListPanel().setModCompleted(e.modName(), e.success());
        } else if (event instanceof UpdateEvent.ModAlreadyInstalled) {
            UpdateEvent.ModAlreadyInstalled e = (UpdateEvent.ModAlreadyInstalled) event;
            ui.getModListPanel().setModAlreadyInstalled(e.modName());
        } else if (event instanceof UpdateEvent.ModError) {
            UpdateEvent.ModError e = (UpdateEvent.ModError) event;
            ui.getModListPanel().setModCompleted(e.modName(), false);
        }
        // LogMessage events are ignored - can be shown in a log panel if needed
    }
}
