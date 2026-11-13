package hopsync;

import hopsync.desktop.AppContainer;
import hopsync.desktop.config.AppVersion;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.HopSyncDark;
import hopsync.desktop.logging.Logging;
import hopsync.desktop.ui.DisplayScaling;
import hopsync.desktop.ui.UIController;
import hopsync.platform.settings.SettingsStore;
import hopsync.platform.system.InstanceLock;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Entry point: sets up logging and display scaling, makes sure only one copy runs, builds the services
 * and opens the launcher window.
 */
public class Main {
    private static final System.Logger LOG = System.getLogger(Main.class.getName());

    // Held for the whole run; the OS releases it when the process exits
    private static InstanceLock instanceLock;

    public static void main(String[] args) {
        Path dataDir = SettingsStore.forCurrentUser().getFile().getParent();
        // Before any AWT class loads, or the scale is ignored
        DisplayScaling.configure();

        // Checked before file logging starts, so a second copy never competes for the log file
        if (!acquireInstanceLock(dataDir)) {
            LOG.log(System.Logger.Level.INFO, "Another HopSync instance is running, exiting");
            SwingUtilities.invokeLater(() -> {
                HopSyncDark.setup();
                JOptionPane.showMessageDialog(null, Messages.get("app.alreadyRunning"), "HopSync",
                    JOptionPane.INFORMATION_MESSAGE);
            });
            return;
        }

        Logging.configure(dataDir.resolve("logs"));
        LOG.log(System.Logger.Level.INFO, "Starting HopSync " + AppVersion.VERSION_APP + " on Java "
            + System.getProperty("java.version") + ", " + System.getProperty("os.name"));
        AppContainer container = AppContainer.forCurrentUser();
        SwingUtilities.invokeLater(() -> UIController.create(container));
    }

    /**
     * @return false only when another instance holds the lock; a lock that cannot be created does not
     *         stop the app
     */
    private static boolean acquireInstanceLock(Path dataDir) {
        try {
            Optional<InstanceLock> lock = InstanceLock.tryAcquire(dataDir.resolve("hopsync.lock"));
            lock.ifPresent(held -> instanceLock = held);
            return lock.isPresent();
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not create the instance lock, continuing without it", e);
            return true;
        }
    }
}
