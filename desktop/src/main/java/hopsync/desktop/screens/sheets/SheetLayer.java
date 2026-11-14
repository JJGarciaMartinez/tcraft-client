package hopsync.desktop.screens.sheets;

import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Draws {@link UiState#sheet()} inside the window: a dimmed layer over everything with the sheet's card in
 * the middle. The layer takes every click, Esc closes the sheet, and Tab stays inside it. The first-run
 * wizard is not drawn here: it fills the window instead (see {@link #firstRunWizard()}).
 */
public final class SheetLayer extends JPanel {
    private static final Color SCRIM = new Color(0, 0, 0, 150);

    private final JLayeredPane owner;
    private final AddPackView wizard;
    private final AddPackView firstRunWizard;
    private final PackSettingsView packSettings;
    private final AppSettingsView appSettings;
    private final JPanel wizardCard;
    private final JPanel packSettingsCard;
    private final JPanel appSettingsCard;
    private JComponent shownCard;

    public SheetLayer(JLayeredPane owner, Consumer<UiAction> dispatch, String demoUrl, Path settingsFile) {
        super(new GridBagLayout());
        this.owner = owner;
        wizard = new AddPackView(dispatch, demoUrl);
        firstRunWizard = new AddPackView(dispatch, demoUrl);
        packSettings = new PackSettingsView(dispatch);
        appSettings = new AppSettingsView(dispatch, settingsFile);
        wizardCard = SheetParts.card(wizard);
        packSettingsCard = SheetParts.card(packSettings);
        appSettingsCard = SheetParts.card(appSettings);
        setOpaque(false);
        setVisible(false);
        setFocusCycleRoot(true);
        // Nothing under the layer reacts while a sheet is open
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeSheet");
        getActionMap().put("closeSheet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible()) dispatch.accept(new UiAction.CloseSheet());
            }
        });

        owner.add(this, JLayeredPane.MODAL_LAYER);
        owner.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(0, 0, owner.getWidth(), owner.getHeight());
            }
        });
    }

    /** The wizard shown in the whole window while there are no packs; call it once. */
    public JComponent firstRunWizard() {
        return SheetParts.card(firstRunWizard);
    }

    public void render(UiState state) {
        Sheet sheet = state.sheet();
        JComponent card = switch (sheet) {
            case Sheet.AddPack add when add.firstRun() -> {
                firstRunWizard.render(add);
                yield null;
            }
            case Sheet.AddPack add -> {
                wizard.render(add);
                yield wizardCard;
            }
            case Sheet.PackSettings settings -> {
                PackUiState pack = state.packs().get(settings.packId());
                packSettings.render(settings, pack);
                yield pack == null ? null : packSettingsCard;
            }
            case Sheet.AppSettings settings -> {
                appSettings.render(settings);
                yield appSettingsCard;
            }
            case null -> null;
        };
        if (card == shownCard) return;
        shownCard = card;
        removeAll();
        if (card != null) {
            add(card);
            setBounds(0, 0, owner.getWidth(), owner.getHeight());
        }
        setVisible(card != null);
        revalidate();
        repaint();
        if (card != null) {
            SwingUtilities.invokeLater(() -> {
                Component first = getFocusTraversalPolicy().getDefaultComponent(this);
                if (first != null) first.requestFocusInWindow();
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(SCRIM);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
