package ui;

import config.AppConfig;
import config.AssetPaths;
import service.FileSystemService;
import service.SystemInfoService;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;

/**
 * The HeaderPanel class is a custom JPanel implementation used as the
 * header section for a user interface. It contains a banner, system information,
 * and mod installation statistics. The panel is styled using custom fonts
 * and a layout that organizes its components into distinct sections.
 */
public class HeaderPanel extends JPanel {
    private final JLabel modStatusLabel;
    private final JLabel statusIconLabel;

    /**
     * Constructs a HeaderPanel for displaying header information such as a banner,
     * system info, and statistics. This panel is styled with a Minecraft-inspired
     * theme and provides display areas for basic status and configuration details.
     *
     * @param fileSystemService A service instance that provides access to file system
     *                          operations, such as locating the mods folder and retrieving
     *                          information about installed mods.
     */
    public HeaderPanel(FileSystemService fileSystemService) {
        Font smallMinecraftFont = FontLoader.getMinecraftFont(11f);
        Font normalMinecraftFont = FontLoader.getMinecraftFont(14f);
        Font bigMinecraftFont = FontLoader.getMinecraftFont(20f);

        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));
        setBorder(BorderFactory.createEmptyBorder(35, 35, 35, 35));

        // Set up the top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        // Set up the banner panel
        JPanel bannerPanel = new JPanel();
        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setOpaque(false);

        // Load and scale the banner image
        ImageIcon originalBanner = new ImageIcon(AssetPaths.MINECRAFT_TITLE);
        Image scaledImage = originalBanner.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel labelBanner = new JLabel(scaledIcon);
        labelBanner.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Set up the subtitle label
        JLabel subtitleLabel = new JLabel("Mod & Config Manager - v" + AppConfig.VERSION_APP);
        subtitleLabel.setFont(smallMinecraftFont);
        subtitleLabel.setForeground(new Color(180, 180, 180));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components to the banner panel
        bannerPanel.add(labelBanner);
        bannerPanel.add(Box.createVerticalStrut(8));
        bannerPanel.add(subtitleLabel);

        // Set up the system info panel
        JPanel systemInfoPanel = new JPanel();
        systemInfoPanel.setLayout(new BoxLayout(systemInfoPanel, BoxLayout.Y_AXIS));
        systemInfoPanel.setOpaque(false);
        systemInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

        // Add system info components
        JLabel systemInfoTitle = new JLabel("Información del Sistema");
        systemInfoTitle.setFont(bigMinecraftFont);
        systemInfoTitle.setForeground(new Color(255, 170, 0));
        systemInfoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add system info labels
        String osName = SystemInfoService.getOS();
        ImageIcon systemIconOg = new ImageIcon(AssetPaths.SYSTEM_ICON);
        Image systemImg = systemIconOg.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon systemIcon = new ImageIcon(systemImg);
        JLabel osLabel = new JLabel(" Sistema: " + osName);
        osLabel.setIcon(systemIcon);
        osLabel.setFont(normalMinecraftFont);
        osLabel.setForeground(new Color(180, 180, 180));
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add Java version label
        String javaVersion = System.getProperty("java.version");
        JLabel javaVersionLabel = new JLabel("☕ Java: " + javaVersion);
        javaVersionLabel.setFont(normalMinecraftFont);
        javaVersionLabel.setForeground(new Color(180, 180, 180));
        javaVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add mods' path label
        String modsPath = fileSystemService.getModsFolder().getAbsolutePath();
        JLabel modPathLabel = new JLabel("📁 Ruta: " + modsPath);
        modPathLabel.setFont(normalMinecraftFont);
        modPathLabel.setForeground(new Color(150, 150, 150));
        modPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components to the system info panel
        systemInfoPanel.add(systemInfoTitle);
        systemInfoPanel.add(Box.createVerticalStrut(10));
        systemInfoPanel.add(osLabel);
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(javaVersionLabel);
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(modPathLabel);

        // Add components to the top panel
        topPanel.add(bannerPanel, BorderLayout.WEST);
        topPanel.add(systemInfoPanel, BorderLayout.CENTER);

        // Inferior panel with stats
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.X_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Status icon and label
        statusIconLabel = new JLabel("");

        // Mods status label
        modStatusLabel = new JLabel("0 mods instalados");
        modStatusLabel.setFont(normalMinecraftFont);
        modStatusLabel.setForeground(new Color(255, 170, 0));

        // Add components to the stats panel
        statsPanel.add(statusIconLabel);
        statsPanel.add(Box.createHorizontalStrut(10));
        statsPanel.add(modStatusLabel);
        statsPanel.add(Box.createHorizontalStrut(15));

        // Add the top and stats panels to the main panel
        add(topPanel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets the status icon displayed in the panel.
     * This method updates the icon associated with the status indicator on the UI.
     * It ensures that the UI modification runs on the Event Dispatch Thread (EDT).
     *
     * @param icon the ImageIcon to be set as the status indicator. It represents the visual status of the application.
     */
    private void setStatusIcon(ImageIcon icon) {
        SwingUtilities.invokeLater(() -> statusIconLabel.setIcon(icon));
    }

    /**
     * Updates the status indicator with the specified type.
     * This method changes the icon displayed in the status indicator based on the provided status type.
     *
     * @param type the status type to be displayed. It determines the icon to be used,
     *             using the icon path associated with the corresponding {@link StatusType}.
     */
    public void setStatus(StatusType type) {
        ImageIcon icon = new ImageIcon(type.getIconPath());
        Image scaledImage = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        setStatusIcon(scaledIcon);
    }

    /**
     * Updates the status message displayed for modifications (mods).
     * This method ensures that the UI update is executed on the Event Dispatch Thread (EDT)
     * to maintain thread safety when modifying Swing components.
     *
     * @param text the status text to be displayed in the mods status label.
     */
    public void updateModsStatus(String text) {
        SwingUtilities.invokeLater(() -> modStatusLabel.setText(text));
    }

    /**
     * Sets the status indicator to "success".
     * This method updates the status of the {@code HeaderPanel} to indicate a successful state.
     * It adjusts the status icon to display the graphic corresponding to a success state,
     * as defined by the {@link StatusType#SUCCESS}.
     */
    public void setStatusSuccess() {
        setStatus(StatusType.SUCCESS);
    }

    /**
     * Sets the status indicator to "warning".
     * This method updates the status of the {@code HeaderPanel} to indicate a warning state.
     * It adjusts the status icon to display the graphic corresponding to a warning state,
     * as defined by the {@link StatusType#WARNING}.
     */
    public void setStatusWarning() {
        setStatus(StatusType.WARNING);
    }

    /**
     * Sets the status indicator to "error".
     * This method updates the status of the {@code HeaderPanel} to indicate an error state.
     * It adjusts the status icon to display the graphic corresponding to an error state,
     * as defined by the {@link StatusType#ERROR}.
     */
    public void setStatusError() {
        setStatus(StatusType.ERROR);
    }

    /**
     * Sets the status indicator to "processing".
     * This method updates the status of the {@code HeaderPanel} to indicate a processing state.
     * It adjusts the status icon to display the graphic corresponding to a processing state,
     * as defined by the {@link StatusType#PROCESSING}.
     */
    public void setStatusProcessing() {
        setStatus(StatusType.PROCESSING);
    }
}