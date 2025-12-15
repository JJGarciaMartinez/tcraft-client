
package ui;

import model.ModInfo;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModListPanel extends JPanel {
    private final JPanel modsContainer;
    private final Map<String, ModCard> modCards;
    private final JLabel emptyLabel;

    public ModListPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(35, 35, 35));

        modCards = new HashMap<>();

        // Contenedor de mods
        modsContainer = new JPanel();
        modsContainer.setLayout(new BoxLayout(modsContainer, BoxLayout.Y_AXIS));
        modsContainer.setBackground(new Color(35, 35, 35));
        modsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Etiqueta para cuando no hay mods
        emptyLabel = new JLabel("No hay mods instalados");
        emptyLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        emptyLabel.setForeground(new Color(150, 150, 150));
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scrollPane = new JScrollPane(modsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(new Color(35, 35, 35));

        add(scrollPane, BorderLayout.CENTER);
    }

    public void addMod(String modName) {
        SwingUtilities.invokeLater(() -> {
            // Remover mensaje de vacío si existe
            if (modsContainer.getComponentCount() == 1 && modsContainer.getComponent(0) == emptyLabel) {
                modsContainer.remove(emptyLabel);
            }

            ModCard card = new ModCard(modName);
            modCards.put(modName, card);
            modsContainer.add(card);
            modsContainer.add(Box.createVerticalStrut(10));
            modsContainer.revalidate();
            modsContainer.repaint();
        });
    }

    /**
     * Agrega un mod con información completa
     * @param modInfo Información completa del mod
     */
    public void addMod(ModInfo modInfo) {
        SwingUtilities.invokeLater(() -> {
            addModInternal(modInfo);
        });
    }

    /**
     * Agrega un mod de forma síncrona (uso interno desde EDT)
     * @param modInfo Información completa del mod
     */
    public void addModSync(ModInfo modInfo) {
        if (SwingUtilities.isEventDispatchThread()) {
            addModInternal(modInfo);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> addModInternal(modInfo));
            } catch (Exception e) {
                System.err.println("Error agregando mod: " + e.getMessage());
            }
        }
    }

    /**
     * Implementación interna para agregar mod (debe llamarse desde EDT)
     */
    private void addModInternal(ModInfo modInfo) {
        // Remover mensaje de vacío si existe
        if (modsContainer.getComponentCount() == 1 && modsContainer.getComponent(0) == emptyLabel) {
            modsContainer.remove(emptyLabel);
        }

        System.out.println("✅ Creando ModCard con key: '" + modInfo.name() + "'");
        ModCard card = new ModCard(modInfo);
        modCards.put(modInfo.name(), card);
        modsContainer.add(card);
        modsContainer.add(Box.createVerticalStrut(10));
        modsContainer.revalidate();
        modsContainer.repaint();
    }

    /**
     * Muestra los mods actuales instalados
     * @param modNames Lista de nombres de mods
     */
    public void showCurrentMods(List<String> modNames) {
        SwingUtilities.invokeLater(() -> {
            clear();

            if (modNames.isEmpty()) {
                showEmptyMessage();
            } else {
                for (String modName : modNames) {
                    ModCard card = new ModCard(modName);
                    card.setStatus(StatusType.SUCCESS,"Instalado", new Color(100, 200, 100));
                    card.setCompleted(true);
                    modCards.put(modName, card);
                    modsContainer.add(card);
                    modsContainer.add(Box.createVerticalStrut(10));
                }
            }

            modsContainer.revalidate();
            modsContainer.repaint();
        });
    }

    /**
     * Muestra los mods actuales instalados con información completa
     * @param mods Lista de ModInfo con información completa
     */
    public void showCurrentModsWithInfo(List<ModInfo> mods) {
        SwingUtilities.invokeLater(() -> {
            clear();

            if (mods.isEmpty()) {
                showEmptyMessage();
            } else {
                for (ModInfo modInfo : mods) {
                    ModCard card = new ModCard(modInfo);
                    card.setStatus(StatusType.SUCCESS,"Instalado", new Color(100, 200, 100));
                    card.setCompleted(true);
                    modCards.put(modInfo.name(), card);
                    modsContainer.add(card);
                    modsContainer.add(Box.createVerticalStrut(10));
                }
            }

            modsContainer.revalidate();
            modsContainer.repaint();
        });
    }

    private void showEmptyMessage() {
        modsContainer.add(Box.createVerticalGlue());
        modsContainer.add(emptyLabel);
        modsContainer.add(Box.createVerticalGlue());
    }

    /**
     * Método genérico para ejecutar una acción en una tarjeta de mod específica
     * @param modName Nombre del mod
     * @param action Acción a ejecutar en la tarjeta (usando Consumer)
     */
    private void executeOnModCard(String modName, java.util.function.Consumer<ModCard> action) {
        ModCard card = modCards.get(modName);
        if (card != null) {
            action.accept(card);
        } else {
            System.err.println("⚠️ ModCard no encontrado para: '" + modName + "'");
            System.err.println("   Keys disponibles: " + modCards.keySet());
        }
    }


    public void setModCompleted(String modName, boolean success) {
        executeOnModCard(modName, card -> card.setCompleted(success));
    }

    public void setModDownloading(String modName) {
        executeOnModCard(modName, ModCard::setDownloading);
    }

    public void setModChecking(String modName) {
        executeOnModCard(modName, ModCard::setChecking);
    }

    public void setModAlreadyInstalled(String modName) {
        executeOnModCard(modName, ModCard::setAlreadyInstalled);
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            modsContainer.removeAll();
            modCards.clear();
            modsContainer.revalidate();
            modsContainer.repaint();
        });
    }

    public int getModCount() {
        return modCards.size();
    }
}