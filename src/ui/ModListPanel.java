package ui;

import config.AppProperties;
import model.ModInfo;
import util.FontLoader;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that displays a list of mods in a scrollable container.
 * It includes functionality to add mods, show the current list of mods,
 * and display a message when no mods are installed.
 */
public class ModListPanel extends JPanel {
    private final JPanel modsContainer;
    private final Map<String, ModCard> modCards;
    private final JLabel emptyLabel;

    /**
     * Constructs the ModListPanel object, initializing its layout, components,
     * and styles. This panel is designed to display a list of mods with a scrollable
     *  container and includes a placeholder message when no mods are installed.
     * <p>
     * The panel consists of the following components:
     * - A scrollable `modsContainer` holding mod cards.
     * - A `JLabel` (`emptyLabel`) used to display a message when no mods are available.
     * <p>
     * Key setup includes:
     * - Using a dark theme with a custom background color.
     * - Configuring a BoxLayout for the mods' container.
     * - Adding a scroll pane for smooth scrolling.
     * <p>
     * The created ModListPanel is ready to accept mod cards or display a no-mods
     * message as appropriate, ensuring a responsive and user-friendly UI.
     */
    public ModListPanel() {
        // Set up panel layout and styles
        setLayout(new BorderLayout());
        setBackground(AppProperties.getBackgroundDark());

        // Create a HashMap to store ModCards by name
        modCards = new HashMap<>();

        // Container for mod cards
        modsContainer = new JPanel();
        modsContainer.setLayout(new BoxLayout(modsContainer, BoxLayout.Y_AXIS));
        modsContainer.setBackground(AppProperties.getBackgroundDark());
        modsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Label for when there are no mods installed
        emptyLabel = new JLabel("No hay mods instalados");
        emptyLabel.setFont(FontLoader.getMinecraftFont(20f));
        emptyLabel.setForeground(AppProperties.getTextTertiary());
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Create a scroll pane for mod cards
        JScrollPane scrollPane = new JScrollPane(modsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(AppProperties.getBackgroundDark());

        // Add a scroll pane to the main panel
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adds a mod synchronously to the mod panel.
     * This method ensures that the addition of a new mod occurs on the Event Dispatch Thread (EDT)
     * to maintain thread safety when working with Swing components. If called from a thread other
     * than the EDT, it will block until the operation is completed on the EDT.
     *
     * @param modInfo Information about the mod to be added, encapsulated in a ModInfo record.
     *                This includes details such as the mod's name, version, description,
     *                author, and URL.
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
     * Adds a mod internally to the mods' container.
     * This method handles the creation and addition of a new ModCard to the UI
     * and ensures that the empty label is removed if present. The UI is then
     * updated to reflect the changes.
     *
     * @param modInfo Information about the mod to be added, encapsulated in a
     *                ModInfo record. This includes details such as the mod's
     *                name, version, description, author, and URL.
     */
    private void addModInternal(ModInfo modInfo) {
        // Remove the empty label if it's present
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
     * Displays the current list of mods in the mod panel.
     * This method updates the UI to reflect the provided list of mod names. If the list
     * is empty, a message indicating that no mods are available is displayed. Otherwise,
     * it creates a visual representation for each mod and updates the mod panel accordingly.
     * The operation is performed on the Event Dispatch Thread (EDT) to ensure thread safety
     * with Swing components.
     *
     * @param modNames A list of mod names to be displayed in the panel. Each mod name
     *                 corresponds to a mod to be represented by a visual ModCard component.
     */
    public void showCurrentMods(List<String> modNames) {
        SwingUtilities.invokeLater(() -> {
            modsContainer.removeAll();
            modCards.clear();

            if (modNames.isEmpty()) {
                showEmptyMessage();
            } else {
                // If there are mods, create ModCards for each
                for (String modName : modNames) {
                    ModCard card = new ModCard(modName);
                    card.setStatus(StatusType.IN_SYSTEM,"Instalado", AppProperties.getAccentSuccess());
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
     * Displays a message indicating that the mod list is empty.
     * This method clears the mod list panel and adds an empty label centered
     * vertically to communicate to the user that no mods are currently available.
     * It uses vertical glue components to achieve centering.
     */
    private void showEmptyMessage() {
        modsContainer.add(Box.createVerticalGlue());
        modsContainer.add(emptyLabel);
        modsContainer.add(Box.createVerticalGlue());
    }


    /**
     * Executes the given action on a ModCard associated with the specified mod name.
     * If no ModCard is found for the given modName, a warning is logged to the error stream.
     *
     * @param modName The name of the mod whose associated ModCard the action should be executed on.
     * @param action The action to perform on the ModCard, expressed as a Consumer functional interface.
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


    /**
     * Updates the completion status of a specified mod.
     * This method executes an action on the ModCard associated with the given mod name,
     * setting its completion status based on the provided success value.
     *
     * @param modName The name of the mod whose completion status needs to be updated.
     * @param success A boolean indicating whether the mod has been successfully completed (true)
     *                or encountered an error (false).
     */
    public void setModCompleted(String modName, boolean success) {
        executeOnModCard(modName, card -> card.setCompleted(success));
    }

    /**
     * Updates the state of the specified mod to indicate it is being downloaded.
     * This method uses a helper function to locate the corresponding ModCard for the given mod name
     * and marks it as downloading.
     *
     * @param modName The name of the mod to set as downloading.
     */
    public void setModDownloading(String modName) {
        executeOnModCard(modName, ModCard::setDownloading);
    }

    /**
     * Updates the state of the specified mod to indicate it is being checked.
     * This method uses a helper function to locate the corresponding ModCard for the given mod name
     * and marks it as checking.
     *
     * @param modName The name of the mod to set as checking.
     */
    public void setModChecking(String modName) {
        executeOnModCard(modName, ModCard::setChecking);
    }

    /**
     * Updates the state of the specified mod to indicate it is already installed.
     * This method uses a helper function to locate the corresponding ModCard for the given mod name
     * and marks it as already installed.
     *
     * @param modName The name of the mod to set as already installed.
     */
    public void setModAlreadyInstalled(String modName) {
        executeOnModCard(modName, ModCard::setAlreadyInstalled);
    }

    /**
     * Clears all mod-related components displayed in the panel.
     * <p>
     * This method removes all components from the modsContainer, clears the list of mod cards,
     * and refreshes the UI to reflect the changes. The operation is performed on the Event Dispatch
     * Thread (EDT) to ensure thread safety with Swing components.
     */
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            modsContainer.removeAll();
            modCards.clear();
            modsContainer.revalidate();
            modsContainer.repaint();
        });
    }
}