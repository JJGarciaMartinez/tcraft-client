package hopsync.desktop.ui;

import hopsync.core.event.UpdateEvent;
import hopsync.core.manifest.PackInfo;
import hopsync.core.sync.SyncPlan;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.ui.ModInfo;

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
                ui.getControlPanel().updateStatus(Messages.get("status.checking"));
            }
            case UpdateEvent.PlanReady e -> {
                showPlan(e.plan());
                ui.getHeaderPanel().showPack(e.plan().manifest().pack());
                ui.getControlPanel().updateStatus(Messages.get("status.review"));
            }
            case UpdateEvent.UpToDate e -> {
                showPlan(e.plan());
                ui.getHeaderPanel().showPack(e.plan().manifest().pack());
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getHeaderPanel().setStatusSuccess();
                PackInfo pack = e.plan().manifest().pack();
                ui.getControlPanel().updateStatus(Messages.get("status.upToDate", pack.name(), pack.version()));
            }
            case UpdateEvent.ApplyStarted e -> {
                ui.getControlPanel().updateProgress(0);
                ui.getControlPanel().showProgress();
                ui.getControlPanel().updateStatus(Messages.get("status.applying"));
            }
            case UpdateEvent.UpdateCompleted e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                if (e.failCount() > 0) {
                    ui.getHeaderPanel().setStatusWarning();
                    ui.getControlPanel().updateStatus(Messages.get("status.completedWithErrors", e.failCount()));
                } else {
                    ui.getHeaderPanel().setStatusSuccess();
                    ui.getControlPanel().updateStatus(Messages.get("status.completed"));
                }
            }
            case UpdateEvent.UpdateCancelled e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                ui.getHeaderPanel().setStatusWarning();
                ui.getControlPanel().updateStatus(Messages.get("status.cancelled"));
            }
            case UpdateEvent.UpdateFailed e -> {
                ui.getControlPanel().setUpdateInProgress(false);
                ui.getControlPanel().hideProgress();
                ui.getHeaderPanel().setStatusError();
                ui.getControlPanel().updateStatus(Messages.failure(e.failure()));
            }
            case UpdateEvent.ProgressChanged e -> ui.getControlPanel().updateProgress(e.percent());
            case UpdateEvent.ModDownloading e -> ui.getModListPanel().setModDownloading(e.file());
            case UpdateEvent.ModCompleted e -> {
                if (e.failure() == null) {
                    ui.getModListPanel().setModCompleted(e.file(), e.success());
                } else {
                    ui.getModListPanel().setModStatus(e.file(), StatusType.ERROR, Messages.failure(e.failure()),
                        Palette.error());
                }
            }
            case UpdateEvent.ModRemoved e -> ui.getModListPanel().setModStatus(e.file(), StatusType.WARNING,
                Messages.get("mod.removed"), Palette.update());
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
                case NEW -> list.setModStatus(file, StatusType.PROCESSING, Messages.get("mod.new"), Palette.added());
                case UPDATE -> list.setModStatus(file, StatusType.PROCESSING, Messages.get("mod.willUpdate"),
                    Palette.update());
            }
        }
        for (String file : plan.removals()) {
            list.addModSync(ModInfo.installed(file));
            list.setModStatus(file, StatusType.WARNING, Messages.get("mod.willRemove"), Palette.update());
        }
    }
}
