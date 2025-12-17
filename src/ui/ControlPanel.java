package ui;

import config.AssetPaths;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * The ControlPanel class extends JPanel and serves as a custom control panel
 * UI component. It includes buttons for update and refresh operations, a
 * status label for displaying messages, and a progress bar for visualizing
 * progress updates.
 * <p>
 * This class supports interaction with external listeners to handle button
 *  actions and provides methods to manage and update the visual components in
 * response to user actions or background processes.
 */
public class ControlPanel extends JPanel {
    private final JButton updateButton;
    private final JButton refreshButton;
    private final JLabel statusLabel;
    private final ImageIcon updateIcon;
    private final ImageIcon cancelIcon;
    private final JProgressBar progressBar;
    private ActionListener updateListener;
    private ActionListener cancelListener;

    /**
     * Constructs a new instance of the ControlPanel class.
     * <p>
     * The ControlPanel is a custom JPanel that serves as a user interface component for displaying
     * and managing tasks related to updates. It includes the following elements:
     * <p>
     * - A progress bar to display the update progress. Initially hidden and configured with specific
     *   styling and dimensions. It uses a custom Minecraft-style font for its appearance.
     * - A set of buttons:
     *   - Update button: Styled with a custom icon, manages update actions, and includes a
     *     press effect for visual feedback.
     *   - Refresh button: Styled similarly to the update button and includes the same press effect.
     * - A status label to display textual information about the current operation or state.
     * - Panels are organized using BorderLayout to separate the progress bar, buttons, and status label.
     *   The layout and colors are customized to give a visually appealing and consistent design.
     * <p>
     * The ControlPanel is designed to handle tasks like setup and configuration of its components
     * during initialization, ensuring an interactive and user-friendly interface.
     */
    public ControlPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Progress bar in the top position
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setFont(FontLoader.getMinecraftFont(11f));
        progressBar.setForeground(new Color(100, 200, 100));
        progressBar.setBackground(new Color(50, 50, 50));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));
        progressBar.setBorderPainted(true);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
        progressBar.setPreferredSize(new Dimension(0, 20));
        progressBar.setVisible(false); // Oculta por defecto

        // Buttons panel in the left position
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);

        // Update and cancel buttons style
        updateIcon = new ImageIcon(AssetPaths.UPDATE_BUTTON);
        updateIcon.setImage(updateIcon.getImage().getScaledInstance(-1, 35, Image.SCALE_SMOOTH));
        cancelIcon = new ImageIcon(AssetPaths.CANCEL_BUTTON);
        cancelIcon.setImage(cancelIcon.getImage().getScaledInstance(-1, 35, Image.SCALE_SMOOTH));
        updateButton = new JButton(updateIcon);
        updateButton.setPreferredSize(new Dimension(updateIcon.getIconWidth(), updateIcon.getIconHeight()));
        updateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateButton.setFocusPainted(false);
        updateButton.setBorderPainted(false);
        updateButton.setContentAreaFilled(false);

        // Press effect
        updateButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                updateButton.setLocation(updateButton.getX() + 1, updateButton.getY() + 1);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                updateButton.setLocation(updateButton.getX() - 1, updateButton.getY() - 1);
            }
        });

        // Refresh button style
        ImageIcon refreshIcon = new ImageIcon(AssetPaths.REFRESH_BUTTON);
        refreshIcon.setImage(refreshIcon.getImage().getScaledInstance(-1, 35, Image.SCALE_SMOOTH));
        refreshButton = new JButton(refreshIcon);
        refreshButton.setPreferredSize(new Dimension(refreshIcon.getIconWidth(), refreshIcon.getIconHeight()));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);

        // Press effect
        refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                refreshButton.setLocation(refreshButton.getX() + 1, refreshButton.getY() + 1);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                refreshButton.setLocation(refreshButton.getX() - 1, refreshButton.getY() - 1);
            }
        });

        // Add buttons to the panel
        buttonPanel.add(updateButton);
        buttonPanel.add(refreshButton);

        // Status label in the right position
        statusLabel = new JLabel("Listo para actualizar");
        statusLabel.setFont(FontLoader.getMinecraftFont(12f));
        statusLabel.setForeground(new Color(180, 180, 180));

        // Central panel that includes the progress bar.
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);

        // Bottom panel that includes the button panel and status label.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);
        centerPanel.add(bottomPanel, BorderLayout.CENTER);

        // Add the central panel to the main panel
        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Sets the {@link ActionListener} for the update button and attaches
     * it to handle update-related actions when the button is pressed.
     *
     * @param listener the {@code ActionListener} that will be triggered when
     *                 the update button is clicked. Cannot be {@code null}.
     */
    public void setUpdateButtonListener(ActionListener listener) {
        this.updateListener = listener;
        updateButton.addActionListener(listener);
    }

    /**
     * Sets the {@link ActionListener} for the cancel button and associates it
     * to handle actions when the cancel button is pressed.
     *
     * @param listener the {@code ActionListener} to be triggered when the
     *                 cancel button is clicked. Cannot be {@code null}.
     */
    public void setCancelButtonListener(ActionListener listener) {
        this.cancelListener = listener;
    }

    /**
     * Sets the {@link ActionListener} for the refresh button and attaches it
     * to handle actions when the refresh button is pressed.
     *
     * @param listener the {@code ActionListener} that will be triggered when
     *                 the refresh button is clicked. Cannot be {@code null}.
     */
    public void setRefreshButtonListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }

    /**
     * Updates the state of the user interface to reflect whether an update is in progress.
     * Depending on the value of {@code inProgress}, this method adjusts the appearance
     * and behavior of the update button, enables or disables the refresh button, and refreshes
     * the panel to reflect the changes.
     *
     * @param inProgress a boolean indicating whether an update is currently in progress.
     *                   If {@code true}, the update button is switched to cancel mode with its
     *                   corresponding icon and action listener. If {@code false}, the button
     *                   is switched back to update mode.
     */
    public void setUpdateInProgress(boolean inProgress) {
        SwingUtilities.invokeLater(() -> {
            if (inProgress) {
                // Change to cancel mode
                updateButton.setIcon(cancelIcon);
                updateButton.setPreferredSize(new Dimension(cancelIcon.getIconWidth(), cancelIcon.getIconHeight()));
                // Remove update listener and add cancel listener
                for (ActionListener al : updateButton.getActionListeners()) {
                    updateButton.removeActionListener(al);
                }
                updateButton.addActionListener(cancelListener);
            } else {
                // Change to update mode
                updateButton.setIcon(updateIcon);
                updateButton.setPreferredSize(new Dimension(updateIcon.getIconWidth(), updateIcon.getIconHeight()));
                // Remove cancel listener and add update listener
                for (ActionListener al : updateButton.getActionListeners()) {
                    updateButton.removeActionListener(al);
                }
                updateButton.addActionListener(updateListener);
            }
            refreshButton.setEnabled(!inProgress);
            revalidate();
            repaint();
        });
    }

    /**
     * Updates the status text displayed in the status label on the user interface.
     * This operation is executed on the Event Dispatch Thread (EDT) to ensure thread safety
     * for Swing components.
     *
     * @param status the new status text to display. Cannot be {@code null}.
     *               Represents the current state or progress of an operation.
     */
    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    /**
     * Updates the progress bar with the specified percentage value and displays it if currently hidden.
     * The update is performed on the Event Dispatch Thread (EDT) to ensure thread safety for Swing components.
     *
     * @param percentage the progress value to set on the progress bar, represented as a percentage (0-100).
     *                    Determines the filled portion of the progress bar and updates its string representation.
     */
    public void updateProgress(int percentage) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percentage);
            progressBar.setString(percentage + "%");
            if (!progressBar.isVisible()) {
                progressBar.setVisible(true);
            }
        });
    }

    /**
     * Hides the progress bar in the {@code ControlPanel} user interface.
     * <p>
     * This method ensures that changes to the visibility of the progress bar
     * are performed on the Event Dispatch Thread (EDT) to guarantee thread
     * safety for Swing components.
     * <p>
     * The progress bar is set to invisible, typically used to indicate
     * that there is no longer an ongoing operation requiring a visual progress indicator.
     */
    public void hideProgress() {
        SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
    }

    /**
     * Displays the progress bar in the user interface and resets its value to 0.
     * <p>
     * This method ensures that changes to the visibility and state of the progress
     * bar are performed on the Event Dispatch Thread (EDT) to guarantee thread
     * safety for Swing components. The progress bar is made visible, typically
     * used to indicate the start of an operation requiring a visual progress indicator.
     */
    public void showProgress() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setValue(0);
        });
    }
}