package hopsync.desktop.ui;

import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;

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
public final class ControlPanel extends JPanel {
    private final JButton updateButton;
    private final JButton refreshButton;
    private final JButton settingsButton;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private ActionListener updateListener;
    private ActionListener cancelListener;

    /**
     * Constructs a new instance of the ControlPanel class.
     * <p>
     * The ControlPanel is a custom JPanel that serves as a user interface component for displaying
     * and managing tasks related to updates. It includes the following elements:
     * <p>
     * - A progress bar to display the update progress. Initially hidden.
     * - A set of raised buttons:
     *   - Update button: the primary action; turns into a danger "Cancelar" button while updating.
     *   - Refresh button: an icon button that reloads the installed mods.
     *   - Settings button: opens the pack settings.
     * - A status label to display textual information about the current operation or state.
     * - Panels are organized using BorderLayout to separate the progress bar, buttons, and status label.
     *   The layout and colors are customized to give a visually appealing and consistent design.
     * <p>
     * The ControlPanel is designed to handle tasks like setup and configuration of its components
     * during initialization, ensuring an interactive and user-friendly interface.
     */
    public ControlPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Palette.surface());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.edge()),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Progress bar in the top position
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 20));
        progressBar.setVisible(false); // Oculta por defecto

        // Buttons panel in the left position
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);

        updateButton = new JButton();
        showUpdateButtonFace(false);

        refreshButton = new JButton(Icons.get(Icons.Glyph.REFRESH));
        refreshButton.setToolTipText("Recargar la lista de mods");
        refreshButton.getAccessibleContext().setAccessibleName("Recargar la lista de mods");
        ButtonStyle.apply(refreshButton, ButtonStyle.SECONDARY);

        settingsButton = new JButton("Ajustes", Icons.get(Icons.Glyph.GEAR));
        ButtonStyle.apply(settingsButton, ButtonStyle.SECONDARY);

        // Add buttons to the panel
        buttonPanel.add(updateButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(settingsButton);

        // Status label in the right position
        statusLabel = new JLabel("Listo para actualizar");
        statusLabel.setForeground(Palette.textSecondary());
        // Takes the space left of the buttons; long messages end in "…" and show in full as a tooltip
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setMinimumSize(new Dimension(0, 0));

        // Central panel that includes the progress bar.
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);

        // Bottom panel that includes the button panel and status label.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
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
     * Sets the {@link ActionListener} that opens the settings dialog.
     */
    public void setSettingsButtonListener(ActionListener listener) {
        settingsButton.addActionListener(listener);
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
                showUpdateButtonFace(true);
                // Remove update listener and add cancel listener
                for (ActionListener al : updateButton.getActionListeners()) {
                    updateButton.removeActionListener(al);
                }
                updateButton.addActionListener(cancelListener);
            } else {
                // Change to update mode
                showUpdateButtonFace(false);
                // Remove cancel listener and add update listener
                for (ActionListener al : updateButton.getActionListeners()) {
                    updateButton.removeActionListener(al);
                }
                updateButton.addActionListener(updateListener);
            }
            refreshButton.setEnabled(!inProgress);
            settingsButton.setEnabled(!inProgress);
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
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setToolTipText(status);
        });
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

    /**
     * Shows the update button as the primary "Actualizar" action, or as a danger "Cancelar" while updating.
     */
    private void showUpdateButtonFace(boolean cancel) {
        updateButton.setText(cancel ? "Cancelar" : "Actualizar");
        ButtonStyle.apply(updateButton, cancel ? ButtonStyle.DANGER : ButtonStyle.PRIMARY);
    }
}