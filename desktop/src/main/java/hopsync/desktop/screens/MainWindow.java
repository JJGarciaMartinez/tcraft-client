package hopsync.desktop.screens;

import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.LogoIcon;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.screens.library.PackLibrary;
import hopsync.desktop.screens.mods.ModsPanel;
import hopsync.desktop.screens.pack.PackHero;
import hopsync.desktop.screens.sheets.SheetLayer;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The HopSync window: pack library on the left, the selected pack's header and mods on the right, status
 * bar at the bottom. With no packs the add-pack wizard fills it instead; other sheets open on top of it.
 * Draws only from {@link UiState}.
 */
public final class MainWindow extends JFrame {
    private static final String ONBOARDING = "onboarding";
    private static final String MAIN = "main";

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final PackLibrary library;
    private final PackHero hero;
    private final ModsPanel mods;
    private final StatusBar statusBar;
    private final Toast toast;
    private final SheetLayer sheets;
    private String shownCard;
    private UiState shown;

    /** @param demoUrl the preset pack link the wizard offers to try */
    public MainWindow(Consumer<UiAction> dispatch, String demoUrl, Path settingsFile, ZoneId zone) {
        super(Messages.get("window.titleEmpty"));
        setIconImages(LogoIcon.windowImages());
        // Closing goes through the store, so a running apply can be confirmed and stopped first
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestClose(dispatch);
            }
        });
        library = new PackLibrary(dispatch);
        hero = new PackHero(dispatch, zone);
        mods = new ModsPanel(dispatch);
        statusBar = new StatusBar(zone);
        sheets = new SheetLayer(getLayeredPane(), dispatch, demoUrl, settingsFile);

        JPanel packArea = new JPanel(new BorderLayout());
        packArea.setBackground(Palette.background());
        packArea.add(hero, BorderLayout.NORTH);
        packArea.add(mods, BorderLayout.CENTER);
        JPanel main = new JPanel(new BorderLayout());
        main.add(library, BorderLayout.WEST);
        main.add(packArea, BorderLayout.CENTER);
        JPanel onboarding = new JPanel(new GridBagLayout());
        onboarding.setBackground(Palette.background());
        onboarding.add(sheets.firstRunWizard());
        body.add(onboarding, ONBOARDING);
        body.add(main, MAIN);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Palette.background());
        content.add(body, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setContentPane(content);
        toast = new Toast(getLayeredPane());

        setMinimumSize(new Dimension(UIScale.scale(820), UIScale.scale(540)));
        setSize(UIScale.scale(960), UIScale.scale(620));
        setLocationRelativeTo(null);
    }

    /**
     * Asks before closing while a pack is writing files; stopping is safe (what finished stays installed, the
     * rest is pending), but the player should know. Cancelling an update from the window never asks.
     */
    private void requestClose(Consumer<UiAction> dispatch) {
        Optional<PackUiState> applying = shown == null ? Optional.empty() : shown.applyingPack();
        if (applying.isPresent()) {
            Object[] options = {Messages.get("close.confirm"), Messages.get("close.cancel")};
            int choice = JOptionPane.showOptionDialog(this,
                Messages.get("close.message", applying.get().displayName()), Messages.get("close.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (choice != 0) return;
        }
        dispatch.accept(new UiAction.Quit());
    }

    public void render(UiState state) {
        shown = state;
        PackUiState pack = state.selected().orElse(null);
        String card = pack == null ? ONBOARDING : MAIN;
        if (!card.equals(shownCard)) {
            cards.show(body, card);
            shownCard = card;
        }
        setTitle(pack == null ? Messages.get("window.titleEmpty") : Messages.get("window.title", pack.displayName()));
        library.render(state);
        if (pack != null) {
            hero.render(pack, state);
            mods.render(pack, state);
        }
        statusBar.render(pack);
        toast.render(state.toast());
        sheets.render(state);
    }
}
