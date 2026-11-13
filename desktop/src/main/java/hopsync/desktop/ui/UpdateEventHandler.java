package hopsync.desktop.ui;

import hopsync.desktop.config.AppProperties;
import hopsync.core.event.UpdateEvent;
import hopsync.desktop.ui.ModInfo;
import hopsync.core.manifest.PackInfo;
import hopsync.core.sync.SyncPlan;

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
                ui.getHeaderPanel().setStatusProcessing();
                ui.getControlPanel().updateStatus("Comprobando el pack...");
            }
            case UpdateEvent.PlanReady e -> {
                showPlan(e.plan());
                ui.getHeaderPanel().showPack(e.plan().manifest().pack());
                ui.getControlPanel().updateStatus("Revisa los cambios antes de aplicarlos");
            }
            case UpdateEvent.UpToDate e -> {
                showPlan(e.plan());
                ui.getHeaderPanel().showPack(e.plan().manifest().pack());
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getHeaderPanel().setStatusSuccess();
                PackInfo pack = e.plan().manifest().pack();
                ui.getControlPanel().updateStatus("Todo al día: " + pack.name() + " " + pack.version());
            }
            case UpdateEvent.ApplyStarted e -> {
                ui.getControlPanel().updateProgress(0);
                ui.getControlPanel().showProgress();
                ui.getControlPanel().updateStatus("Aplicando cambios...");
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
            case UpdateEvent.ProgressChanged e -> ui.getControlPanel().updateProgress(e.percent());
            case UpdateEvent.ModDownloading e -> ui.getModListPanel().setModDownloading(e.file());
            case UpdateEvent.ModCompleted e -> ui.getModListPanel().setModCompleted(e.file(), e.success());
            case UpdateEvent.ModRemoved e -> ui.getModListPanel().setModStatus(e.file(), StatusType.WARNING,
                "Eliminado", AppProperties.getAccentWarning());
            // Can be shown in a log panel if needed
            case UpdateEvent.LogMessage e -> { }
        }
    }

    private void showPlan(SyncPlan plan) {
        ModListPanel list = ui.getModListPanel();
        list.clear();
        for (SyncPlan.ModAction action : plan.mods()) {
            list.addModSync(ModInfo.of(action.mod()));
            String file = action.mod().file();
            switch (action.change()) {
                case UP_TO_DATE -> list.setModAlreadyInstalled(file);
                case NEW -> list.setModStatus(file, StatusType.PROCESSING, "Nuevo", AppProperties.getAccentInfo());
                case UPDATE -> list.setModStatus(file, StatusType.PROCESSING, "Se actualizará",
                    AppProperties.getAccentWarningLight());
            }
        }
        for (String file : plan.removals()) {
            list.addModSync(ModInfo.installed(file));
            list.setModStatus(file, StatusType.WARNING, "Se eliminará", AppProperties.getAccentWarning());
        }
    }
}
