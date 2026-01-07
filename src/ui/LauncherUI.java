package ui;

import config.AppProperties;
import service.ServiceFactory;

import javax.swing.*;
import java.awt.*;

/**
 * The LauncherUI class serves as the primary graphical user interface (GUI) for the Tcraft Client mod updater application.
 * This class extends JFrame and arranges various custom panels to provide functionality for mod management and updates.
 * It includes a header panel, a mod list panel, and a control panel, all integrated within a dark-themed interface.
 * <p>
 * Responsibilities:
 * - Initializes and sets up the main window of the application with a defined size, layout, and appearance.
 * - Manages the instances of HeaderPanel, ModListPanel, and ControlPanel, which represent different functional areas of the UI.
 * - Integrates a file system service used for utility operations and passing it to relevant components.
 */
public class LauncherUI extends JFrame {
    private final HeaderPanel headerPanel;
    private final ModListPanel modListPanel;
    private final ControlPanel controlPanel;

    /**
     * Constructs a new instance of the LauncherUI class, which serves as the main graphical interface
     * for the Tcraft Client mod updater application. This constructor initializes the application window
     * with a predefined size, layout, and dark-themed appearance and incorporates the primary user interface
     * panels.
     * <p>
     * Responsibilities of this constructor include:
     * - Setting the window title to "Tcraft Client - Actualizador de Mods".
     * - Defining and enforcing the default and minimum dimensions of the window.
     * - Configuring the frame to close the application when the window is closed.
     * - Centering the frame on the screen upon initialization.
     * - Applying a dark theme to the background of the window.
     * - Creating and integrating functional UI components: HeaderPanel, ModListPanel,
     *   and ControlPanel, which are positioned in the northern, central, and southern regions of the layout, respectively.
     */
    public LauncherUI() {
        super("Tcraft Client - Actualizador de Mods");
        setSize(850, 600);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Configurar tema oscuro
        setBackground(AppProperties.getBackgroundDark());

        // Crear paneles
        headerPanel = new HeaderPanel(ServiceFactory.getFileSystemService());
        modListPanel = new ModListPanel();
        controlPanel = new ControlPanel();

        // Agregar paneles al frame
        add(headerPanel, BorderLayout.NORTH);
        add(modListPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Retrieves the header panel of the application, which is responsible
     * for displaying the top section of the user interface.
     *
     * @return the HeaderPanel instance representing the header section of the UI.
     */
    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    /**
     * Retrieves the mod list panel of the application, which is responsible
     * for displaying the list of mods in the central section of the user interface.
     *
     * @return the ModListPanel instance representing the mod listing section of the UI.
     */
    public ModListPanel getModListPanel() {
        return modListPanel;
    }

    /**
     * Retrieves the control panel of the application, which is responsible
     * for handling user interactions and providing controls located in the
     * bottom section of the user interface.
     *
     * @return the ControlPanel instance representing the control section of the UI.
     */
    public ControlPanel getControlPanel() {
        return controlPanel;
    }
}