package ui;

import config.AppConfig;
import config.AssetPaths;
import service.FileSystemService;
import service.SystemInfoService;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private final JLabel modStatusLabel;
    private final JLabel statusIconLabel;

    /**
     * Constructor for HeaderPanel.
     */
    public HeaderPanel(FileSystemService fileSystemService) {
        Font smallMinecraftFont = FontLoader.getMinecraftFont(11f);
        Font normalMinecraftFont = FontLoader.getMinecraftFont(14f);
        Font bigMinecraftFont = FontLoader.getMinecraftFont(20f);

        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));
        setBorder(BorderFactory.createEmptyBorder(35, 35, 35, 35));

        // Panel superior que contiene banner (izquierda) e info del sistema (derecha)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        // Panel izquierdo con banner
        JPanel bannerPanel = new JPanel();
        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setOpaque(false);

        // Cargar imagen desde assets usando AssetPaths
        ImageIcon originalBanner = new ImageIcon(AssetPaths.MINECRAFT_TITLE);
        Image scaledImage = originalBanner.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel labelBanner = new JLabel(scaledIcon);
        labelBanner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Mod & Config Manager - v" + AppConfig.VERSION_APP);
        subtitleLabel.setFont(smallMinecraftFont);
        subtitleLabel.setForeground(new Color(180, 180, 180));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        bannerPanel.add(labelBanner);
        bannerPanel.add(Box.createVerticalStrut(8));
        bannerPanel.add(subtitleLabel);

        // Panel derecho con información del sistema
        JPanel systemInfoPanel = new JPanel();
        systemInfoPanel.setLayout(new BoxLayout(systemInfoPanel, BoxLayout.Y_AXIS));
        systemInfoPanel.setOpaque(false);
        systemInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

        JLabel systemInfoTitle = new JLabel("Información del Sistema");
        systemInfoTitle.setFont(bigMinecraftFont);
        systemInfoTitle.setForeground(new Color(255, 170, 0));
        systemInfoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        String osName = SystemInfoService.getOS();
        ImageIcon systemIconOg = new ImageIcon(AssetPaths.SYSTEM_ICON);
        Image systemImg = systemIconOg.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon systemIcon = new ImageIcon(systemImg);
        JLabel osLabel = new JLabel(" Sistema: " + osName);
        osLabel.setIcon(systemIcon);
        osLabel.setFont(normalMinecraftFont);
        osLabel.setForeground(new Color(180, 180, 180));
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String javaVersion = System.getProperty("java.version");
        JLabel javaVersionLabel = new JLabel("☕ Java: " + javaVersion);
        javaVersionLabel.setFont(normalMinecraftFont);
        javaVersionLabel.setForeground(new Color(180, 180, 180));
        javaVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String modsPath = fileSystemService.getModsFolder().getAbsolutePath();
        JLabel modPathLabel = new JLabel("📁 Ruta: " + modsPath);
        modPathLabel.setFont(normalMinecraftFont);
        modPathLabel.setForeground(new Color(150, 150, 150));
        modPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        systemInfoPanel.add(systemInfoTitle);
        systemInfoPanel.add(Box.createVerticalStrut(10));
        systemInfoPanel.add(osLabel);
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(javaVersionLabel);
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(modPathLabel);

        topPanel.add(bannerPanel, BorderLayout.WEST);
        topPanel.add(systemInfoPanel, BorderLayout.CENTER);

        // Panel inferior con estadísticas
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.X_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        statusIconLabel = new JLabel("");
        statusIconLabel.setForeground(new Color(100, 200, 100));

        modStatusLabel = new JLabel("0 mods instalados");
        modStatusLabel.setFont(normalMinecraftFont);
        modStatusLabel.setForeground(new Color(255, 170, 0));


        statsPanel.add(statusIconLabel);
        statsPanel.add(Box.createHorizontalStrut(10));
        statsPanel.add(modStatusLabel);
        statsPanel.add(Box.createHorizontalStrut(15));

        add(topPanel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }

    private void setStatusIcon(ImageIcon icon) {
        SwingUtilities.invokeLater(() -> statusIconLabel.setIcon(icon));
    }

    /**
     * Establece el ícono de estado usando el enum StatusType
     */
    public void setStatus(StatusType type) {
        ImageIcon icon = new ImageIcon(type.getIconPath());
        Image scaledImage = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        setStatusIcon(scaledIcon);
    }

    public void updateModsStatus(String text) {
        SwingUtilities.invokeLater(() -> modStatusLabel.setText(text));
    }

    // Métodos de conveniencia que mantienen la API existente
    public void setStatusSuccess() {
        setStatus(StatusType.SUCCESS);
    }

    public void setStatusWarning() {
        setStatus(StatusType.WARNING);
    }

    public void setStatusError() {
        setStatus(StatusType.ERROR);
    }

    public void setStatusProcessing() {
        setStatus(StatusType.PROCESSING);
    }
}