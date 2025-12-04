import service.ModUpdater;

import javax.swing.*;
import java.awt.*;

public class LauncherFrame extends JFrame {

    private final JTextArea logArea;
    private final JProgressBar progressBar;

    public LauncherFrame() {
        super("Tcraft Client - Actualizador de Mods");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Área de log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Barra de progreso
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);

        // Ejecuta la lógica en un hilo separado para no congelar la ventana
        new Thread(this::executeUpdate).start();
    }

    private void log(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateProgressBar(int current, int total) {
        SwingUtilities.invokeLater(() -> {
            int percentage = (int) ((current / (float) total) * 100);
            progressBar.setValue(percentage);
        });
    }

    private void executeUpdate() {
        ModUpdater updater = new ModUpdater(this::log, this::updateProgressBar);
        updater.initUpdate();
        
        JOptionPane.showMessageDialog(this, "Mods actualizados correctamente.");
        System.exit(0);
    }

    // Método estático para crear y mostrar el frame
    public static void init() {
        SwingUtilities.invokeLater(() -> {
            LauncherFrame frame = new LauncherFrame();
            frame.setVisible(true);
        });
    }
}
