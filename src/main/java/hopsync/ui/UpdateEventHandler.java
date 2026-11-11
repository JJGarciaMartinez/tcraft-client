package hopsync.ui;

import hopsync.event.UpdateEvent;
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
        switch (event) {
            case UpdateEvent.UpdateStarted e -> {
                ui.getModListPanel().clear();
                ui.getControlPanel().setUpdateInProgress(true);
                ui.getControlPanel().showProgress();
                ui.getHeaderPanel().setStatusProcessing();
            }
            case UpdateEvent.UpdateCompleted e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                if (e.failCount() > 0) {
                    ui.getHeaderPanel().setStatusWarning();
                    ui.getControlPanel().updateStatus("Completado con " + e.failCount() + " errores");
                } else {
                    ui.getHeaderPanel().setStatusSuccess();
                    ui.getControlPanel().updateStatus("¡Actualización completada!");
                }
            }
            case UpdateEvent.UpdateCancelled e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                ui.getHeaderPanel().setStatusWarning();
                ui.getControlPanel().updateStatus("Actualización cancelada");
            }
            case UpdateEvent.UpdateFailed e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                ui.getHeaderPanel().setStatusError();
                ui.getControlPanel().updateStatus("Error: " + e.error());
            }
            case UpdateEvent.ProgressChanged e -> {
                int percentage = e.total() > 0 ? (e.current() * 100) / e.total() : 0;
                ui.getControlPanel().updateProgress(percentage);
            }
            case UpdateEvent.ModProcessing e -> ui.getModListPanel().addModSync(e.mod());
            case UpdateEvent.ModDownloading e -> ui.getModListPanel().setModDownloading(e.modName());
            case UpdateEvent.ModCompleted e -> ui.getModListPanel().setModCompleted(e.modName(), e.success());
            case UpdateEvent.ModAlreadyInstalled e -> ui.getModListPanel().setModAlreadyInstalled(e.modName());
            case UpdateEvent.ModError e -> ui.getModListPanel().setModCompleted(e.modName(), false);
            // Can be shown in a log panel if needed
            case UpdateEvent.LogMessage e -> { }
        }
    }
}
