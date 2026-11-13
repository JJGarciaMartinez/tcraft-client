package hopsync.desktop.screens.pack;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.UIScale;
import hopsync.core.manifest.PackInfo;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.BlockTile;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Fonts;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.kit.StatusTag;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Header of the selected pack: block tile, name and version, Minecraft · loader · mods, the server address
 * (click to copy), the status line and the actions that make sense in the pack's phase. Folder and ⋯ buttons
 * sit in the top right corner.
 */
public final class PackHero extends JPanel {
    private static final int MIN_HEIGHT = 134;
    private static final int ACTIONS_MIN_WIDTH = 220;

    private final Consumer<UiAction> dispatch;
    private final ZoneId zone;
    private final PackMenu menu;
    private final JLabel tile = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel version = new JLabel();
    private final JLabel meta = new JLabel();
    private final JButton serverIp = new JButton(Icons.get(Icons.Glyph.COPY, 12, null));
    private final JLabel stateMark = new JLabel();
    private final JLabel stateTitle = new JLabel();
    private final JLabel stateDetail = new JLabel();
    private final JPanel tags = row(FlowLayout.RIGHT);
    private final JPanel buttons = row(FlowLayout.RIGHT);
    private final JPanel progress = new JPanel(new BorderLayout(0, 4));
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JLabel percent = new JLabel();
    private final JLabel currentFile = new JLabel();
    private String packId;
    private HeroModel shown;

    public PackHero(Consumer<UiAction> dispatch, ZoneId zone) {
        super(new BorderLayout(UIScale.scale(16), 0));
        this.dispatch = dispatch;
        this.zone = zone;
        this.menu = new PackMenu(dispatch);
        setBackground(Palette.surface());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.line()),
            BorderFactory.createEmptyBorder(16, 20, 16, 14)));

        add(tile, BorderLayout.WEST);
        add(info(), BorderLayout.CENTER);
        add(actions(), BorderLayout.EAST);
    }

    private JPanel info() {
        name.putClientProperty(FlatClientProperties.STYLE, "font: bold +9");
        version.setFont(new Font(Fonts.MONO_FAMILY, Font.PLAIN, Math.round(getFont().getSize2D() - 1)));
        version.setForeground(Palette.textSecondary());
        version.setBorder(new FlatLineBorder(new Insets(1, 6, 1, 6), Palette.line(), 1, 10));
        JPanel nameRow = row(FlowLayout.LEFT);
        nameRow.add(name);
        nameRow.add(version);

        meta.setForeground(Palette.textSecondary());
        serverIp.setFont(new Font(Fonts.MONO_FAMILY, Font.PLAIN, Math.round(getFont().getSize2D() - 1.5f)));
        serverIp.setHorizontalTextPosition(JButton.LEFT);
        serverIp.setToolTipText(Messages.get("hero.copyIp"));
        ButtonStyle.apply(serverIp, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
        serverIp.addActionListener(e -> dispatchFor(UiAction.CopyServerIp::new));
        // Wraps the server button under the line when the window is narrow
        JPanel metaRow = new JPanel(new WrapLayout(FlowLayout.LEFT, UIScale.scale(6), UIScale.scale(2)));
        metaRow.setOpaque(false);
        metaRow.add(meta);
        metaRow.add(serverIp);

        stateTitle.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        stateDetail.setForeground(Palette.textSecondary());
        JPanel stateRow = row(FlowLayout.LEFT);
        stateRow.add(stateMark);
        stateRow.add(stateTitle);
        stateRow.add(stateDetail);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(Box.createVerticalGlue());
        for (JPanel line : new JPanel[] {nameRow, metaRow, stateRow}) {
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(line);
            info.add(Box.createVerticalStrut(UIScale.scale(5)));
        }
        info.add(Box.createVerticalGlue());
        return info;
    }

    private JPanel actions() {
        JButton folder = iconButton(Icons.Glyph.FOLDER, "hero.openFolder");
        folder.addActionListener(e -> dispatchFor(UiAction.OpenFolder::new));
        JButton more = iconButton(Icons.Glyph.MORE, "hero.more");
        // Right-aligned under the button, so it stays inside the window
        more.addActionListener(e -> menu.show(more, more.getWidth() - menu.getPreferredSize().width, more.getHeight()));
        JPanel tools = row(FlowLayout.RIGHT);
        tools.add(folder);
        tools.add(more);

        bar.setPreferredSize(new Dimension(UIScale.scale(280), UIScale.scale(10)));
        percent.setForeground(Palette.textSecondary());
        currentFile.setForeground(Palette.text());
        progress.setOpaque(false);
        progress.add(bar, BorderLayout.NORTH);
        JPanel progressText = new JPanel(new BorderLayout());
        progressText.setOpaque(false);
        progressText.add(currentFile, BorderLayout.CENTER);
        progressText.add(percent, BorderLayout.EAST);
        progress.add(progressText, BorderLayout.CENTER);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        for (JPanel part : new JPanel[] {tags, progress, buttons}) {
            part.setAlignmentX(Component.RIGHT_ALIGNMENT);
            stack.add(part);
        }

        JPanel actions = new JPanel(new BorderLayout(0, UIScale.scale(6))) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(Math.max(size.width, UIScale.scale(ACTIONS_MIN_WIDTH)), size.height);
            }
        };
        actions.setOpaque(false);
        actions.add(tools, BorderLayout.NORTH);
        actions.add(stack, BorderLayout.CENTER);
        return actions;
    }

    public void render(PackUiState pack, UiState state) {
        packId = pack.packId();
        PackInfo info = pack.packInfo();
        tile.setIcon(BlockTile.forPack(pack.packId(), 64));
        name.setText(pack.displayName());
        version.setText(info == null ? "" : info.version());
        version.setVisible(info != null);
        String metaText = HeroModel.meta(pack);
        meta.setText(metaText == null ? " " : metaText);
        boolean hasIp = info != null && info.serverIp() != null && !info.serverIp().isBlank();
        serverIp.setVisible(hasIp);
        serverIp.setText(hasIp ? info.serverIp() : "");
        menu.setPack(pack);

        HeroModel model = HeroModel.of(pack, state, zone);
        if (!sameLayout(shown, model)) {
            rebuild(model);
        }
        if (model.percent() != null) {
            bar.setValue(model.percent());
            percent.setText(Messages.get("hero.progress", model.percent()));
            currentFile.setText(model.currentFile() == null ? " " : Messages.get("hero.downloading", model.currentFile()));
        }
        shown = model;
        // The texts above change the height the header needs (the server button only exists with an IP)
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, UIScale.scale(MIN_HEIGHT));
        return size;
    }

    /** Everything but the progress numbers, which change many times per second while applying. */
    private static boolean sameLayout(HeroModel before, HeroModel model) {
        return before != null && before.mark() == model.mark() && before.title().equals(model.title())
            && Objects.equals(before.detail(), model.detail()) && before.tags().equals(model.tags())
            && before.actions().equals(model.actions()) && (before.percent() == null) == (model.percent() == null);
    }

    private void rebuild(HeroModel model) {
        stateMark.setIcon(StatusMark.of(model.mark()));
        stateTitle.setText(model.title());
        stateDetail.setText(model.detail() == null ? "" : "· " + model.detail());
        stateDetail.setToolTipText(model.detail());

        tags.removeAll();
        model.tags().forEach(tag -> tags.add(new StatusTag(tag.text(), tag.tone())));
        tags.setVisible(!model.tags().isEmpty());

        progress.setVisible(model.percent() != null);

        buttons.removeAll();
        for (HeroModel.Action action : model.actions()) {
            JButton button = new JButton(action.label(), action.icon() == null ? null : Icons.get(action.icon()));
            ButtonStyle.apply(button, action.style());
            button.setEnabled(action.action() != null);
            if (action.action() != null) {
                button.addActionListener(e -> dispatch.accept(action.action()));
            }
            buttons.add(button);
        }
        revalidate();
        repaint();
    }

    private void dispatchFor(Function<String, UiAction> action) {
        if (packId != null) {
            dispatch.accept(action.apply(packId));
        }
    }

    private static JButton iconButton(Icons.Glyph glyph, String key) {
        JButton button = new JButton(Icons.get(glyph));
        button.setToolTipText(Messages.get(key));
        button.getAccessibleContext().setAccessibleName(Messages.get(key));
        ButtonStyle.apply(button, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
        return button;
    }

    private static JPanel row(int align) {
        JPanel row = new JPanel(new FlowLayout(align, UIScale.scale(6), 0));
        row.setOpaque(false);
        return row;
    }
}
