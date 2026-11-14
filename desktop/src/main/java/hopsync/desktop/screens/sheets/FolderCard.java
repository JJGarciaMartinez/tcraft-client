package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.StatusTag;
import hopsync.desktop.store.GameFolder;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One game folder to pick: launcher and instance name, the path, and a tag with its mods or the pack that
 * already uses it. The whole card is clickable; the radio button carries focus and the accessible name.
 */
final class FolderCard extends JPanel {
    private static final int PATH_WIDTH = 360;

    FolderCard(GameFolder folder, boolean selected, Runnable onPick) {
        super(new BorderLayout(UIScale.scale(10), 0));
        setBackground(selected ? Palette.raised() : Palette.surface());
        putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        setBorder(BorderFactory.createCompoundBorder(
            new FlatLineBorder(new Insets(0, 0, 0, 0), selected ? Palette.accent() : Palette.line(), 1, 10),
            BorderFactory.createEmptyBorder(8, 8, 8, 12)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JRadioButton radio = new JRadioButton(title(folder), selected);
        radio.setOpaque(false);
        radio.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        radio.addActionListener(e -> onPick.run());
        // Long paths end in "…" instead of widening the sheet; the tooltip keeps them whole
        JLabel path = new JLabel(folder.path().toString()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(Math.min(size.width, UIScale.scale(PATH_WIDTH)), size.height);
            }
        };
        SheetParts.toMono(path);
        path.setToolTipText(folder.path().toString());
        path.setBorder(BorderFactory.createEmptyBorder(0, UIScale.scale(26), 0, 0));
        path.setMinimumSize(new Dimension(0, 0));

        JPanel text = new JPanel(new BorderLayout(0, UIScale.scale(2)));
        text.setOpaque(false);
        text.add(radio, BorderLayout.NORTH);
        text.add(path, BorderLayout.CENTER);
        add(text, BorderLayout.CENTER);
        // Centered on the right, at its own height
        JPanel tagCell = new JPanel(new GridBagLayout());
        tagCell.setOpaque(false);
        tagCell.add(tag(folder));
        add(tagCell, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!selected) onPick.run();
            }
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    static String title(GameFolder folder) {
        String kind = Messages.get("sheet.folder.kind." + folder.kind().name());
        return folder.name() == null || folder.name().isBlank() ? kind : kind + " · " + folder.name();
    }

    private static StatusTag tag(GameFolder folder) {
        if (!folder.isFree()) {
            return new StatusTag(Messages.get("sheet.folder.usedBy", folder.usedBy()), StatusTag.Tone.ERROR);
        }
        return new StatusTag(Messages.get("sheet.folder.jars", folder.jarCount()), StatusTag.Tone.NEUTRAL);
    }
}
