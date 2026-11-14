package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.SegmentedControl;
import hopsync.desktop.store.LinkCheck;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.Sheet.PackSettings.Section;
import hopsync.desktop.store.UiAction;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Settings of one pack: its link, its game folder, and removing it from HopSync. Saving a new link needs it
 * checked first, because it may point to another pack.
 */
public final class PackSettingsView extends JPanel {
    private final Consumer<UiAction> dispatch;
    private final JLabel title = SheetParts.title("");
    private final SegmentedControl<Section> sections;
    private final LinkField link;
    private final JPanel linkPane = SheetParts.column();
    private final JPanel linkNotes = SheetParts.column();
    private final JPanel folderPane = SheetParts.column();
    private final JPanel removePane = SheetParts.column();
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel bottom = SheetParts.column();
    private Sheet.PackSettings shown;
    private Section shownSection;

    public PackSettingsView(Consumer<UiAction> dispatch) {
        super(new BorderLayout(0, UIScale.scale(16)));
        this.dispatch = dispatch;
        setOpaque(false);
        body.setOpaque(false);
        sections = new SegmentedControl<>(Arrays.asList(Section.values()),
            section -> dispatch.accept(new UiAction.SelectPackSection(section)));
        for (Section section : Section.values()) {
            sections.setLabel(section, Messages.get("sheet.pack.section." + section.name()));
        }
        link = new LinkField(dispatch, new UiAction.SavePackSettings());
        // Built once, so typing in the field keeps the focus
        SheetParts.stack(linkPane, 8, SheetParts.label(Messages.get("sheet.link.label")), link, linkNotes);

        JPanel header = SheetParts.column();
        SheetParts.stack(header, 12, title, sections);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void render(Sheet.PackSettings sheet, PackUiState pack) {
        if (sheet.equals(shown) || pack == null) return;
        shown = sheet;
        title.setText(Messages.get("sheet.pack.title", pack.displayName()));
        sections.setSelected(sheet.section());

        switch (sheet.section()) {
            case LINK -> renderLink(sheet, pack);
            case FOLDER -> renderFolder(sheet, pack);
            case REMOVE -> renderRemove(sheet, pack);
        }
        if (sheet.section() != shownSection) {
            shownSection = sheet.section();
            body.removeAll();
            body.add(switch (sheet.section()) {
                case LINK -> linkPane;
                case FOLDER -> folderPane;
                case REMOVE -> removePane;
            }, BorderLayout.NORTH);
        }

        bottom.removeAll();
        if (sheet.error() != null) {
            SheetParts.stack(bottom, 10, SheetParts.notice(
                Messages.get("sheet.pack.saveFailed", Messages.failure(sheet.error())), Palette.error()));
        }
        SheetParts.stack(bottom, 12, footer(sheet, pack));
        revalidate();
        repaint();
    }

    private void renderLink(Sheet.PackSettings sheet, PackUiState pack) {
        link.render(sheet.url(), sheet.link(), Messages.get("sheet.pack.linkHint"));
        linkNotes.removeAll();
        if (newLink(sheet, pack) && sheet.link() instanceof LinkCheck.Valid) {
            SheetParts.stack(linkNotes, 0, SheetParts.notice(
                Messages.get("sheet.pack.linkChanged", pack.displayName()), Palette.update()));
        }
    }

    private void renderFolder(Sheet.PackSettings sheet, PackUiState pack) {
        folderPane.removeAll();
        JButton change = new JButton(Messages.get("sheet.pack.changeFolder"), Icons.get(Icons.Glyph.FOLDER));
        ButtonStyle.apply(change, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
        change.addActionListener(e -> SheetParts.chooseFolder(this, sheet.folder().path())
            .ifPresent(path -> dispatch.accept(new UiAction.ChooseFolder(path))));
        SheetParts.stack(folderPane, 8, SheetParts.label(Messages.get("sheet.folder.label")),
            new FolderCard(sheet.folder(), true, () -> { }), change);
        if (!sheet.folder().isFree()) {
            SheetParts.stack(folderPane, 8, SheetParts.notice(
                Messages.get("sheet.folder.taken", sheet.folder().usedBy()), Palette.error()));
        } else if (!sheet.folder().path().equals(pack.gameDir())) {
            SheetParts.stack(folderPane, 8, SheetParts.notice(Messages.get("sheet.pack.folderChanged"),
                Palette.update()));
        } else {
            SheetParts.stack(folderPane, 8, SheetParts.hint(Messages.get("sheet.folder.hint")));
        }
    }

    private void renderRemove(Sheet.PackSettings sheet, PackUiState pack) {
        removePane.removeAll();
        JButton remove = SheetParts.button("sheet.pack.remove", ButtonStyle.DANGER, dispatch,
            new UiAction.RemovePack(sheet.packId()));
        remove.setIcon(Icons.get(Icons.Glyph.TRASH));
        // The pack may have started writing files since the sheet opened
        remove.setEnabled(!pack.phase().isWriting());
        SheetParts.stack(removePane, 12,
            SheetParts.paragraph(Messages.get("sheet.pack.removeBody", pack.displayName()), Palette.text()),
            remove);
    }

    private JPanel footer(Sheet.PackSettings sheet, PackUiState pack) {
        JButton cancel = SheetParts.button("sheet.cancel", ButtonStyle.SECONDARY, dispatch, new UiAction.CloseSheet());
        if (sheet.section() == Section.REMOVE) {
            return SheetParts.footer(cancel);
        }
        JButton save = SheetParts.button("sheet.save", ButtonStyle.PRIMARY, dispatch, new UiAction.SavePackSettings());
        save.setEnabled(canSave(sheet, pack));
        return SheetParts.footer(cancel, save);
    }

    /** A new link must have been checked; the folder must not belong to another pack. */
    static boolean canSave(Sheet.PackSettings sheet, PackUiState pack) {
        boolean linkOk = !newLink(sheet, pack)
            || sheet.link() instanceof LinkCheck.Valid valid && valid.url().equals(sheet.url().trim());
        return linkOk && sheet.folder().isFree() && !pack.phase().isWriting();
    }

    private static boolean newLink(Sheet.PackSettings sheet, PackUiState pack) {
        return !sheet.url().trim().equals(pack.manifestUrl());
    }
}
