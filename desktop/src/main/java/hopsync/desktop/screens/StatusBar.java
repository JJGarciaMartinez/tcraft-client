package hopsync.desktop.screens;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.config.AppVersion;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.PackUiState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Flat bar at the bottom: the selected pack's folder, when it was last checked and the app version.
 */
final class StatusBar extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId zone;
    private final JLabel folder = label(Icons.Glyph.FOLDER);
    private final JLabel checked = label(Icons.Glyph.CLOCK);

    StatusBar(ZoneId zone) {
        this.zone = zone;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Palette.statusBar());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.line()),
            BorderFactory.createEmptyBorder(0, 16, 0, 16)));
        setPreferredSize(new Dimension(0, UIScale.scale(28)));
        JLabel version = label(null);
        version.setText(AppVersion.VERSION_APP);
        add(folder);
        add(Box.createHorizontalStrut(UIScale.scale(16)));
        add(checked);
        add(Box.createHorizontalGlue());
        add(version);
    }

    void render(PackUiState pack) {
        if (pack == null) {
            folder.setText(Messages.get("statusBar.noPack"));
            folder.setToolTipText(null);
            checked.setText(Messages.get("statusBar.notChecked"));
            return;
        }
        folder.setText(pack.gameDir().toString());
        folder.setToolTipText(pack.gameDir().toString());
        checked.setText(pack.lastChecked() == null ? Messages.get("statusBar.notChecked")
            : Messages.get("statusBar.checkedAt", TIME.withZone(zone).format(pack.lastChecked())));
    }

    private static JLabel label(Icons.Glyph glyph) {
        JLabel label = new JLabel(glyph == null ? null : Icons.get(glyph, 12, Palette.textFaint()));
        label.setForeground(Palette.textFaint());
        label.putClientProperty(FlatClientProperties.STYLE, "font: -2");
        label.setIconTextGap(UIScale.scale(5));
        return label;
    }
}
