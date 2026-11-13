package hopsync.desktop.screens;

import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.screens.library.PackLibrary;
import hopsync.desktop.screens.mods.ModsPanel;
import hopsync.desktop.screens.pack.PackHero;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * The HopSync window: pack library on the left, the selected pack's header and mods on the right, status
 * bar at the bottom. With no packs it shows the onboarding card instead. Draws only from {@link UiState}.
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
    private final Sheets sheets;
    private String shownCard;

    public MainWindow(Consumer<UiAction> dispatch, Sheets.SettingsOpener settings, Path settingsFile, ZoneId zone) {
        super(Messages.get("window.titleEmpty"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        library = new PackLibrary(dispatch);
        hero = new PackHero(dispatch, zone);
        mods = new ModsPanel(dispatch);
        statusBar = new StatusBar(zone);
        sheets = new Sheets(this, dispatch, settings, settingsFile);

        JPanel packArea = new JPanel(new BorderLayout());
        packArea.setBackground(Palette.background());
        packArea.add(hero, BorderLayout.NORTH);
        packArea.add(mods, BorderLayout.CENTER);
        JPanel main = new JPanel(new BorderLayout());
        main.add(library, BorderLayout.WEST);
        main.add(packArea, BorderLayout.CENTER);
        body.add(new Onboarding(dispatch), ONBOARDING);
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

    public void render(UiState state) {
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
