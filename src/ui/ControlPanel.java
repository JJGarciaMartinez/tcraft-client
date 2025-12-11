package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ControlPanel extends JPanel {
    private final JButton updateButton;
    private final JButton cancelButton;
    private final JButton refreshButton;
    private final JLabel statusLabel;

    public ControlPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);

        updateButton = new JButton("Actualizar Mods");
        updateButton.setFont(new Font("Arial", Font.BOLD, 14));
        updateButton.setPreferredSize(new Dimension(180, 40));
        updateButton.setBackground(new Color(100, 200, 255));
        updateButton.setForeground(Color.WHITE);
        updateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateButton.setFocusPainted(false);
        updateButton.setBorderPainted(false);

        cancelButton = new JButton("Cancelar");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelButton.setPreferredSize(new Dimension(180, 40));
        cancelButton.setBackground(new Color(80, 80, 80));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setEnabled(false);

        refreshButton = new JButton("🔄 Refrescar");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 14));
        refreshButton.setPreferredSize(new Dimension(150, 40));
        refreshButton.setBackground(new Color(70, 180, 120));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);

        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(refreshButton);

        // Label de estado general
        statusLabel = new JLabel("Listo para actualizar");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(180, 180, 180));

        add(buttonPanel, BorderLayout.WEST);
        add(statusLabel, BorderLayout.EAST);
    }

    public void setUpdateButtonListener(ActionListener listener) {
        updateButton.addActionListener(listener);
    }

    public void setCancelButtonListener(ActionListener listener) {
        cancelButton.addActionListener(listener);
    }

    public void setRefreshButtonListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }

    public void setUpdateInProgress(boolean inProgress) {
        SwingUtilities.invokeLater(() -> {
            updateButton.setEnabled(!inProgress);
            cancelButton.setEnabled(inProgress);
            refreshButton.setEnabled(!inProgress);
        });
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
}