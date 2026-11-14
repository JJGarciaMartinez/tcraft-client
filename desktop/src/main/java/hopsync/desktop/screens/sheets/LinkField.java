package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.core.manifest.PackInfo;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.BlockTile;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.screens.pack.PlanSummary;
import hopsync.desktop.store.LinkCheck;
import hopsync.desktop.store.UiAction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The pack link: text field, "Comprobar enlace" and, under them, what HopSync knows about the link (the
 * pack preview when it works, or what is wrong in plain words). Typing dispatches the text; rendering never
 * does, so the caret is not disturbed.
 */
final class LinkField extends JPanel {
    private final Consumer<UiAction> dispatch;
    private final JTextField field = new JTextField();
    private final JButton check;
    private final JPanel status = new JPanel(new BorderLayout());
    private boolean rendering;
    private LinkCheck shownLink;
    private LinkCheck link = new LinkCheck.Unchecked();

    /** @param onEnter what Enter does once the link works, e.g. go to the next step */
    LinkField(Consumer<UiAction> dispatch, UiAction onEnter) {
        super(new BorderLayout(0, UIScale.scale(10)));
        this.dispatch = dispatch;
        setOpaque(false);
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "https://…/manifest.json");
        field.getAccessibleContext().setAccessibleName(Messages.get("sheet.link.label"));
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                typed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                typed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                typed();
            }
        });
        field.addActionListener(e -> dispatch.accept(
            link instanceof LinkCheck.Valid && onEnter != null ? onEnter : new UiAction.CheckLink()));
        check = SheetParts.button("sheet.link.check", ButtonStyle.SECONDARY, dispatch, new UiAction.CheckLink());

        JPanel row = new JPanel(new BorderLayout(UIScale.scale(8), 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(check, BorderLayout.EAST);
        status.setOpaque(false);
        add(row, BorderLayout.NORTH);
        add(status, BorderLayout.CENTER);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    void render(String url, LinkCheck newLink, String hint) {
        link = newLink;
        if (!field.getText().equals(url)) {
            rendering = true;
            try {
                field.setText(url);
            } finally {
                rendering = false;
            }
        }
        boolean checking = newLink instanceof LinkCheck.Checking;
        check.setEnabled(!checking);
        check.setText(Messages.get(checking ? "sheet.link.checking" : "sheet.link.check"));
        if (Objects.equals(shownLink, newLink) && status.getComponentCount() > 0) return;
        shownLink = newLink;
        status.removeAll();
        status.add(statusOf(newLink, hint), BorderLayout.CENTER);
        status.revalidate();
        status.repaint();
    }

    JTextField field() {
        return field;
    }

    private void typed() {
        if (!rendering) {
            dispatch.accept(new UiAction.SetLinkUrl(field.getText()));
        }
    }

    private static JComponent statusOf(LinkCheck link, String hint) {
        return switch (link) {
            case LinkCheck.Unchecked u -> SheetParts.hint(hint);
            case LinkCheck.Checking c -> SheetParts.hint(Messages.get("sheet.link.checkingHint"));
            case LinkCheck.Valid valid -> preview(valid);
            case LinkCheck.Invalid invalid -> SheetParts.notice(switch (invalid.problem()) {
                case EMPTY -> Messages.get("sheet.link.empty");
                case NOT_HTTPS -> Messages.get("sheet.link.notHttps");
                case ALREADY_ADDED -> Messages.get("sheet.link.alreadyAdded");
                case UNREADABLE -> Messages.get("sheet.link.unreadable", Messages.failure(invalid.failure()));
            }, Palette.error());
        };
    }

    /** What the link points to: the same tile, name and line as the pack header will show. */
    private static JComponent preview(LinkCheck.Valid valid) {
        PackInfo info = valid.info();
        JLabel name = new JLabel(info.name() + "  ");
        name.putClientProperty(FlatClientProperties.STYLE, "font: semibold +2");
        JLabel version = SheetParts.mono(info.version());
        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setOpaque(false);
        nameRow.add(name, BorderLayout.WEST);
        nameRow.add(version, BorderLayout.CENTER);
        JLabel meta = new JLabel(Messages.get("sheet.link.meta", info.minecraft(), info.loader(), valid.mods(),
            PlanSummary.formatBytes(valid.bytes())));
        meta.setForeground(Palette.textSecondary());

        JPanel text = SheetParts.column();
        SheetParts.stack(text, 3, nameRow, meta);
        if (info.description() != null && !info.description().isBlank()) {
            SheetParts.stack(text, 4, SheetParts.paragraph(info.description(), Palette.textSecondary(),
                SheetParts.CONTENT_WIDTH - 80));
        }
        JPanel card = new JPanel(new BorderLayout(UIScale.scale(12), 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setBackground(Palette.raised());
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.add(new JLabel(BlockTile.forPack(valid.url(), 44)), BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }
}
