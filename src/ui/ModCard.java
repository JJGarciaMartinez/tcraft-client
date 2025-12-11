package ui;

import javax.swing.*;
import java.awt.*;

public class ModCard extends JPanel {
    private final JLabel nameLabel;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel iconLabel;

    public ModCard(String modName) {
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setBackground(new Color(40, 40, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Panel izquierdo con icono
        iconLabel = new JLabel("📦");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        add(iconLabel, BorderLayout.WEST);

        // Panel central con información
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        nameLabel = new JLabel(modName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);

        statusLabel = new JLabel("Esperando...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(180, 180, 180));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressBar.setVisible(false);

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(progressBar);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void setStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    public void setProgress(int percentage) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setValue(percentage);
            progressBar.setString(percentage + "%");
        });
    }

    public void setCompleted(boolean success) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(false);
            if (success) {
                iconLabel.setText("✅");
                setStatus("Completado", new Color(100, 200, 100));
            } else {
                iconLabel.setText("❌");
                setStatus("Error", new Color(200, 100, 100));
            }
        });
    }

    public void setDownloading() {
        SwingUtilities.invokeLater(() -> {
            iconLabel.setText("⬇️");
            setStatus("Descargando...", new Color(100, 150, 255));
        });
    }

    public void setChecking() {
        SwingUtilities.invokeLater(() -> {
            iconLabel.setText("🔍");
            setStatus("Verificando...", new Color(255, 200, 100));
        });
    }

    public void setAlreadyInstalled() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(false);
            iconLabel.setText("✓");
            setStatus("Ya instalado", new Color(150, 200, 150));
        });
    }
}