package hopsync;

import hopsync.ui.DisplayScaling;
import hopsync.ui.UIController;

import javax.swing.SwingUtilities;

/**
 * The Main class serves as the entry point for the Tcraft Client mod updater application.
 * It initializes and launches the user interface by invoking the UIController.
 */
public class Main {
    public static void main(String[] args) {
        // Before any AWT class loads, or the scale is ignored
        DisplayScaling.configure();
        SwingUtilities.invokeLater(UIController::create);
    }
}