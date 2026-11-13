package hopsync;

import hopsync.desktop.AppContainer;
import hopsync.desktop.ui.DisplayScaling;
import hopsync.desktop.ui.UIController;

import javax.swing.SwingUtilities;

/**
 * Entry point: configures display scaling, builds the services and opens the launcher window.
 */
public class Main {
    public static void main(String[] args) {
        // Before any AWT class loads, or the scale is ignored
        DisplayScaling.configure();
        AppContainer container = AppContainer.forCurrentUser();
        SwingUtilities.invokeLater(() -> UIController.create(container));
    }
}