package ui;

import service.FileSystemService;

import javax.swing.*;
import java.util.List;

/**
 * Service responsible for loading and refreshing the mod list.
 */
public class ModLoadService {
    private final FileSystemService fileSystemService;
    private final UINotificationService notificationService;
    private final LauncherUI ui;

    /**
     * Creates a new ModLoadService.
     *
     * @param ui the launcher UI instance
     * @param notificationService the notification service
     */
    public ModLoadService(LauncherUI ui, UINotificationService notificationService) {
        this.ui = ui;
        this.notificationService = notificationService;
        this.fileSystemService = new FileSystemService();
    }

    /**
     * Loads the currently installed mods in a separate thread.
     * Updates the UI with the mod list and status.
     */
    public void loadCurrentMods() {
        new Thread(() -> {
            try {
                List<String> currentMods = fileSystemService.getCurrentMods();
                int modCount = currentMods.size();

                SwingUtilities.invokeLater(() -> {
                    ui.getModListPanel().showCurrentMods(currentMods);
                    notificationService.notifyModsLoaded(modCount);
                });

            } catch (Exception e) {
                System.err.println("Error al cargar mods: " + e.getMessage());
                SwingUtilities.invokeLater(notificationService::notifyModLoadError);
            }
        }).start();
    }

    /**
     * Refreshes the mod list by updating UI status and reloading mods.
     */
    public void refreshModList() {
        notificationService.notifyLoadingMods();
        loadCurrentMods();

        // Show "List updated" message after a short delay
        Timer timer = new Timer(500, e -> {
            notificationService.notifyListRefreshed();
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }
}
