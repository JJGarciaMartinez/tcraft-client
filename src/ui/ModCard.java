package ui;

import model.ModInfo;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;

import static ui.StatusType.*;

/**
 * A card component that displays information about a mod, including its name, version, author, description, and status.
 * This uses Swing components to create a visually appealing layout.
 */
public class ModCard extends JPanel {
    private final JLabel statusLabel;
    private final JLabel iconLabel;

    /**
     * Constructor that accepts a mod name.
     * This will create a ModInfo object with empty values for a version, author, and description.
     */
    public ModCard(String modName) {
        this(new ModInfo(modName, "", "", "", ""));
    }

    /**
     * Constructor that accepts a ModInfo object.
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
     * Set the status of the mod card.
     * @param type  The status type (icon).
     * @param status The status text.
     * @param color  The color of the status text.
     */
    public void setStatus(StatusType type, String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            ImageIcon originalIcon = new ImageIcon(type.getIconPath());
            Image scaledImage = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            iconLabel.setIcon(icon);
            statusLabel.setText(status);
            statusLabel.setForeground(color);
            statusLabel.setFont(FontLoader.getMinecraftFont(14f));
        });
    }

    /**
     * Set the status of the mod card to complete.
     * @param success Whether the operation was successful or not.
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
     * Set the status of the mod card to downloading.
     */
    public void setDownloading() {
        SwingUtilities.invokeLater(() -> setStatus(PROCESSING,"Descargando...", new Color(100, 150, 255)));
    }

    /**
     * Set the status of the mod card to checking.
     */
    public void setChecking() {
        SwingUtilities.invokeLater(() -> setStatus(PROCESSING,"Verificando...", new Color(255, 200, 100)));
    }

    /**
     * Set the status of the mod card to already installed.
     */
    public void setAlreadyInstalled() {
        SwingUtilities.invokeLater(() -> setStatus(SUCCESS,"Ya instalado", new Color(150, 200, 150)));
    }
}