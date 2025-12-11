package ui;

import javax.swing.*;
import java.awt.*;

public class LauncherUI extends JFrame {
    private final HeaderPanel headerPanel;
    private final ModListPanel modListPanel;
    private final ControlPanel controlPanel;

    public LauncherUI() {
        super("Tcraft Client - Actualizador de Mods");
        setSize(850, 600);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Configurar tema oscuro
        setBackground(new Color(30, 30, 30));

        // Crear paneles
        headerPanel = new HeaderPanel();
        modListPanel = new ModListPanel();
        controlPanel = new ControlPanel();

        // Agregar paneles al frame
        add(headerPanel, BorderLayout.NORTH);
        add(modListPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public ModListPanel getModListPanel() {
        return modListPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }
}