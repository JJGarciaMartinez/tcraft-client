package ui;

import service.SystemInfoService;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private final JLabel subtitleLabel;
    private final JLabel modCountLabel;
    private final JLabel statusIconLabel;

    public HeaderPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));
        setBorder(BorderFactory.createEmptyBorder(35,35,35,35));

        // Panel izquierdo con texto principal
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        // Cargar imagen desde assets
        ImageIcon originalIcon = new ImageIcon("assets/minecraft_title.png");
        Image scaledImage = originalIcon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel titleLabel = new JLabel(scaledIcon);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String osName = SystemInfoService.getOS();
        JLabel osLabel = new JLabel("🖥️ " + osName);
        osLabel.setFont(FontLoader.getMinecraftFont(16f));
        osLabel.setForeground(new Color(180, 180, 180));

        subtitleLabel = new JLabel("Mod & Config Manager");
        subtitleLabel.setFont(FontLoader.getMinecraftFont(16f));
        subtitleLabel.setForeground(new Color(180, 180, 180));

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(osLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);

        // Panel derecho con estadísticas
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        statusIconLabel = new JLabel("✓");
        statusIconLabel.setFont(FontLoader.getMinecraftFont(Font.BOLD, 36f));
        statusIconLabel.setForeground(new Color(100, 200, 100));
        statusIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        modCountLabel = new JLabel("0 mods instalados");
        modCountLabel.setFont(FontLoader.getMinecraftFont(Font.BOLD, 18f));
        modCountLabel.setForeground(new Color(255, 170, 0));
        modCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(FontLoader.getMinecraftFont(12f));
        versionLabel.setForeground(new Color(150, 150, 150));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statsPanel.add(statusIconLabel);
        statsPanel.add(Box.createVerticalStrut(5));
        statsPanel.add(modCountLabel);
        statsPanel.add(Box.createVerticalStrut(5));
        statsPanel.add(versionLabel);

        add(textPanel, BorderLayout.WEST);
        add(statsPanel, BorderLayout.EAST);
    }

    public void updateSubtitle(String text) {
        SwingUtilities.invokeLater(() -> subtitleLabel.setText(text));
    }

    public void updateModCount(int count) {
        SwingUtilities.invokeLater(() -> {
            modCountLabel.setText(count + (count == 1 ? " mod instalado" : " mods instalados"));
        });
    }

    public void setStatusIcon(String icon, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusIconLabel.setText(icon);
            statusIconLabel.setForeground(color);
        });
    }

    public void setStatusSuccess() {
        setStatusIcon("✓", new Color(100, 200, 100));
    }

    public void setStatusWarning() {
        setStatusIcon("⚠", new Color(255, 200, 50));
    }

    public void setStatusError() {
        setStatusIcon("✗", new Color(230, 80, 80));
    }

    public void setStatusProcessing() {
        setStatusIcon("⟳", new Color(100, 150, 255));
    }
}