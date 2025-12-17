package ui;

import model.ModInfo;
import util.FontLoader;
import util.ImageLoader;

import javax.swing.*;
import java.awt.*;

import static ui.StatusType.*;

/**
 * Represents a UI component for displaying information about a mod
 * and its status. The component includes the mod's name, version,
 * author, description, and a status indicator.
 */
public class ModCard extends JPanel {
    private final JLabel statusLabel;
    private final JLabel iconLabel;

    /**
     * Constructor for creating a ModCard object with a specified mod name.
     * This constructor initializes the ModCard instance using a ModInfo object
     * populated with the given mod name and placeholder values for other fields.
     *
     * @param modName The name of the mod to be displayed on the ModCard.
     */
    public ModCard(String modName) {
        this(new ModInfo(modName, "", "", "", ""));
    }

    /**
     * Constructs a ModCard object initialized with information about a mod.
     * The card displays the mod's name, version, author, description,
     * and provides a section for status updates.
     *
     * @param modInfo An instance of ModInfo containing details about the mod,
     *                such as its name, version, description, and author.
     */
    public ModCard(ModInfo modInfo) {
        Font smallMinecraftFont = FontLoader.getMinecraftFont(11f);
        Font normalMinecraftFont = FontLoader.getMinecraftFont(14f);
        Font biggerMinecraftFont = FontLoader.getMinecraftFont(32f);

        // Set up panel layout and styles
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setBackground(new Color(40, 40, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Left panel with icon setup
        iconLabel = new JLabel("");
        iconLabel.setFont(biggerMinecraftFont);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(iconLabel, BorderLayout.WEST);

        // Center panel with text and metadata setup
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Name mod panel setup
        JLabel nameLabel = new JLabel(modInfo.name());
        nameLabel.setFont(normalMinecraftFont);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Version and Author panel setup
        JPanel metadataPanel = new JPanel();
        metadataPanel.setLayout(new BoxLayout(metadataPanel, BoxLayout.X_AXIS));
        metadataPanel.setOpaque(false);
        metadataPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Mod version validation
        boolean modVersionExist = modInfo.version() != null && !modInfo.version().isEmpty();

        // Show version if available
        if (modVersionExist) {
            JLabel versionLabel = new JLabel("v" + modInfo.version());
            versionLabel.setFont(smallMinecraftFont);
            versionLabel.setForeground(new Color(150, 150, 250));
            metadataPanel.add(versionLabel);
        }

        // Show author if available
        if (modInfo.author() != null && !modInfo.author().isEmpty()) {
            if (modVersionExist) {
                JLabel separator = new JLabel(" • ");
                separator.setFont(smallMinecraftFont);
                separator.setForeground(new Color(120, 120, 120));
                metadataPanel.add(separator);
            }
            JLabel authorLabel = new JLabel("por " + modInfo.author());
            authorLabel.setFont(smallMinecraftFont);
            authorLabel.setForeground(new Color(180, 180, 180));
            metadataPanel.add(authorLabel);
        }

        // Agregar glue para mantener el contenido a la izquierda
        metadataPanel.add(Box.createHorizontalGlue());

        // Descripción
        JLabel descriptionLabel = null;
        if (modInfo.description() != null && !modInfo.description().isEmpty()) {
            descriptionLabel = new JLabel(modInfo.description());
            descriptionLabel.setFont(smallMinecraftFont);
            descriptionLabel.setForeground(new Color(160, 160, 160));
            descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        // State label setup
        statusLabel = new JLabel("");
        statusLabel.setFont(normalMinecraftFont);
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components to the center panel
        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(metadataPanel);
        if (descriptionLabel != null) {
            centerPanel.add(Box.createVerticalStrut(3));
            centerPanel.add(descriptionLabel);
        }
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(5));

        // Add the center panel to the main panel
        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Updates the status of the mod card asynchronously by delegating to an internal method.
     * The method is executed on the Event Dispatch Thread (EDT).
     *
     * @param type   The type of the status represented by an icon, such as SUCCESS, WARNING, ERROR, etc.
     * @param status The status message text to display on the mod card.
     * @param color  The color of the status message text.
     */
    public void setStatus(StatusType type, String status, Color color) {
        SwingUtilities.invokeLater(() -> setStatusInternal(type, status, color));
    }

    /**
     * Updates the internal status of the mod card by setting the icon, status message,
     * text color, and font on the associated UI components.
     *
     * @param type   The type of the status represented by an icon. This determines
     *               the image to be displayed, such as SUCCESS, WARNING, ERROR, etc.
     * @param status The status text to display on the mod card, representing the
     *               current state or message.
     * @param color  The color in which the status text should be displayed.
     */
    private void setStatusInternal(StatusType type, String status, Color color) {
        ImageIcon icon = ImageLoader.loadScaledImageIcon(type.getIconPath(), 32, 32);
        if (icon != null) {
            iconLabel.setIcon(icon);
        }
        statusLabel.setText(status);
        statusLabel.setForeground(color);
        statusLabel.setFont(FontLoader.getMinecraftFont(14f));
    }

    /**
     * Updates the completion status of the mod card. The method modifies the
     * status asynchronously on the Event Dispatch Thread (EDT) by invoking an
     * internal status update method based on the completion result.
     *
     * @param success A boolean indicating the result of the operation:
     *                true if the operation was successful, false otherwise.
     */
    public void setCompleted(boolean success) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                setStatus(SUCCESS,"Completado", new Color(100, 200, 100));
            } else {
                setStatus(StatusType.ERROR,"Error", new Color(200, 100, 100));
            }
        });
    }

    /**
     * Updates the status of the mod card to indicate that a download is in progress.
     * This method sets the status asynchronously on the Event Dispatch Thread (EDT)
     * by invoking the {@link #setStatus(StatusType, String, Color)} method with
     * pre-defined status type, message, and color values.
     * <p>
     * The status type is set to {@code PROCESSING}, the message is set to
     * "Descargando..." (indicating downloading in progress), and the color is
     * set to a light blue shade.
     * <p>
     * This method ensures that the status update is performed in a thread-safe
     * manner, as required for Swing components.
     */
    public void setDownloading() {
        SwingUtilities.invokeLater(() -> setStatus(PROCESSING,"Descargando...", new Color(100, 150, 255)));
    }

    /**
     * Updates the status of the mod card to indicate that a verification process is ongoing.
     * This method sets the status asynchronously on the Event Dispatch Thread (EDT)
     * using the {@link #setStatus(StatusType, String, Color)} method.
     * <p>
     * The status type is set to {@code PROCESSING}, the status message is set to "Verificando...",
     * and the status color is a light orange shade.
     * <p>
     * This method ensures that the status update is performed in a thread-safe manner,
     * as required for Swing components.
     */
    public void setChecking() {
        SwingUtilities.invokeLater(() -> setStatus(PROCESSING,"Verificando...", new Color(255, 200, 100)));
    }

    /**
     * Updates the status of the mod card to indicate that the mod is already installed.
     * This method sets the status asynchronously on the Event Dispatch Thread (EDT)
     * by invoking the {@link #setStatus(StatusType, String, Color)} method.
     * <p>
     * The status type is set to {@code SUCCESS}, the status message is set to "Ya instalado"
     * (indicating that the mod is already installed), and the status color is a shade of green.
     * <p>
     * This method ensures thread safety when updating Swing components.
     */
    public void setAlreadyInstalled() {
        SwingUtilities.invokeLater(() -> setStatus(SUCCESS,"Ya instalado", new Color(150, 200, 150)));
    }
}