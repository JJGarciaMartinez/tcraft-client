package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Fonts;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.UiAction;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Building blocks shared by the sheets: titles, wrapped paragraphs, notices and the footer row.
 */
final class SheetParts {
    /** Width of the text inside a sheet; paragraphs wrap at it. */
    static final int CONTENT_WIDTH = 500;

    private SheetParts() {
    }

    /** The raised card a sheet is drawn on, in the overlay or in the whole window on first run. */
    static JPanel card(JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Palette.surface());
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 14");
        card.setBorder(BorderFactory.createCompoundBorder(
            new FlatLineBorder(new Insets(0, 0, 0, 0), Palette.edge(), 1, 14),
            BorderFactory.createEmptyBorder(22, 26, 20, 26)));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    static JPanel column() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /** Adds the parts one under the other, left aligned, with a gap between them. */
    static void stack(JPanel column, int gap, Component... parts) {
        for (Component part : parts) {
            if (column.getComponentCount() > 0) {
                column.add(Box.createVerticalStrut(UIScale.scale(gap)));
            }
            if (part instanceof JComponent c) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            column.add(part);
        }
    }

    static JLabel title(String text) {
        JLabel title = new JLabel(text);
        title.setFont(Fonts.display(title.getFont(), 6));
        return title;
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }

    /** Text that wraps at the sheet width. */
    static JLabel paragraph(String text, Color color) {
        return paragraph(text, color, CONTENT_WIDTH);
    }

    static JLabel paragraph(String text, Color color, int width) {
        JLabel paragraph = new JLabel(wrap(text, width));
        paragraph.setForeground(color);
        return paragraph;
    }

    static JLabel hint(String text) {
        JLabel hint = paragraph(text, Palette.textSecondary());
        hint.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        return hint;
    }

    /** A boxed message with a colored edge: a warning, an error, or neutral information. */
    static JComponent notice(String text, Color tone) {
        JLabel body = paragraph(text, Palette.text(), CONTENT_WIDTH - 24);
        body.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, tone),
            BorderFactory.createEmptyBorder(6, 10, 6, 6)));
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        box.setBackground(Palette.well());
        box.add(body, BorderLayout.CENTER);
        return box;
    }

    static JLabel mono(String text) {
        return toMono(new JLabel(text));
    }

    static JLabel toMono(JLabel label) {
        label.setFont(new Font(Fonts.MONO_FAMILY, Font.PLAIN, Math.round(label.getFont().getSize2D() - 1)));
        label.setForeground(Palette.textSecondary());
        return label;
    }

    static JButton button(String key, ButtonStyle style, Consumer<UiAction> dispatch, UiAction action) {
        JButton button = new JButton(Messages.get(key));
        ButtonStyle.apply(button, style);
        button.addActionListener(e -> dispatch.accept(action));
        return button;
    }

    /** Buttons on the right, the main one last. */
    static JPanel footer(AbstractButton... buttons) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(8), 0));
        footer.setOpaque(false);
        for (AbstractButton button : buttons) {
            footer.add(button);
        }
        return footer;
    }

    /**
     * Asks for a game folder with the system chooser. Called from a button handler, so its modal loop never
     * runs while the store dispatches.
     */
    static Optional<Path> chooseFolder(Component parent, Path start) {
        JFileChooser chooser = new JFileChooser(start == null ? null : start.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(Messages.get("sheet.folder.chooserTitle"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null
            ? Optional.of(chooser.getSelectedFile().toPath()) : Optional.empty();
    }

    /**
     * HTML that wraps at the given width; the text is escaped, line breaks are kept. Swing reads CSS pixels as
     * points (96 / 72 screen pixels each), so the width is converted back.
     */
    static String wrap(String text, int width) {
        return "<html><div style='width:" + Math.round(UIScale.scale(width) * 72f / 96f) + "px'>"
            + escape(text).replace("\n", "<br>") + "</div></html>";
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
