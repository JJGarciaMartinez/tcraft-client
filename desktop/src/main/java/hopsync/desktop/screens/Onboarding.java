package hopsync.desktop.screens;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.UiAction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

/**
 * Shown while there are no packs. The add-pack wizard will live here; for now it opens the settings.
 */
final class Onboarding extends JPanel {

    Onboarding(Consumer<UiAction> dispatch) {
        super(new GridBagLayout());
        setBackground(Palette.background());

        JLabel title = new JLabel(Messages.get("onboarding.title"));
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +8");
        JLabel body = new JLabel(Messages.get("onboarding.body"));
        body.setForeground(Palette.textSecondary());
        JButton add = new JButton(Messages.get("onboarding.add"), Icons.get(Icons.Glyph.PLUS));
        ButtonStyle.apply(add, ButtonStyle.PRIMARY);
        add.addActionListener(e -> dispatch.accept(new UiAction.OpenSettings()));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Palette.surface());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Palette.line()),
            BorderFactory.createEmptyBorder(26, 28, 26, 28)));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(UIScale.scale(6)));
        card.add(body);
        card.add(Box.createVerticalStrut(UIScale.scale(20)));
        card.add(add);
        add(card);
    }
}
