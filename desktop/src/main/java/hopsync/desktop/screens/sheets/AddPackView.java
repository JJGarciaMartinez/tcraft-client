package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.GameFolder;
import hopsync.desktop.store.LinkCheck;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.UiAction;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The add-pack wizard. Step 1 checks the link and shows the pack; step 2 picks the game folder among the
 * launcher instances found on disk or any other. Fills the window on first run and sits in a sheet later.
 */
public final class AddPackView extends JPanel {
    private final Consumer<UiAction> dispatch;
    private final String demoUrl;
    private final JLabel title = SheetParts.title("");
    private final JLabel subtitle = new JLabel();
    private final JLabel step = new JLabel();
    private final LinkField link;
    private final JButton demo = new JButton(Messages.get("sheet.wizard.demo"));
    private final JPanel linkStep = SheetParts.column();
    private final JPanel folderStep = SheetParts.column();
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel bottom = SheetParts.column();
    private Sheet.AddPack shown;

    /** @param demoUrl the preset pack link, offered to try HopSync; blank to hide it */
    public AddPackView(Consumer<UiAction> dispatch, String demoUrl) {
        super(new BorderLayout(0, UIScale.scale(18)));
        this.dispatch = dispatch;
        this.demoUrl = demoUrl;
        setOpaque(false);
        link = new LinkField(dispatch, new UiAction.WizardNext());
        subtitle.setForeground(Palette.textSecondary());
        step.setForeground(Palette.textFaint());
        step.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        body.setOpaque(false);
        // Built once: rebuilding it while the player types would take the focus away from the field
        ButtonStyle.apply(demo, ButtonStyle.QUIET, ButtonStyle.SMALL);
        demo.addActionListener(e -> {
            dispatch.accept(new UiAction.SetLinkUrl(demoUrl));
            dispatch.accept(new UiAction.CheckLink());
        });
        SheetParts.stack(linkStep, 8, SheetParts.label(Messages.get("sheet.link.label")), link, demo);

        JPanel header = SheetParts.column();
        SheetParts.stack(header, 4, step, title, subtitle);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void render(Sheet.AddPack sheet) {
        if (sheet.equals(shown)) return;
        boolean stepChanged = shown == null || shown.step() != sheet.step();
        shown = sheet;
        step.setText(Messages.get("sheet.wizard.step", sheet.step()));
        title.setText(Messages.get(sheet.firstRun() ? "sheet.wizard.firstRunTitle" : "sheet.wizard.title"));
        subtitle.setText(Messages.get(sheet.step() == 1 ? "sheet.wizard.linkSubtitle" : "sheet.wizard.folderSubtitle"));

        if (sheet.step() == 1) {
            link.render(sheet.url(), sheet.link(), Messages.get("sheet.link.hint"));
            demo.setVisible(demoUrl != null && !demoUrl.isBlank() && !demoUrl.equals(sheet.url()));
        } else {
            folderStep.removeAll();
            SheetParts.stack(folderStep, 8, SheetParts.label(Messages.get("sheet.folder.label")));
            folders(sheet).forEach(part -> SheetParts.stack(folderStep, 8, part));
        }
        if (stepChanged) {
            body.removeAll();
            body.add(sheet.step() == 1 ? linkStep : folderStep, BorderLayout.NORTH);
        }

        bottom.removeAll();
        if (sheet.error() != null) {
            SheetParts.stack(bottom, 10, SheetParts.notice(
                Messages.get("sheet.wizard.saveFailed", Messages.failure(sheet.error())), Palette.error()));
        }
        SheetParts.stack(bottom, 12, footer(sheet));
        revalidate();
        repaint();
        if (stepChanged && sheet.step() == 1) {
            link.field().requestFocusInWindow();
        }
    }

    private List<JComponent> folders(Sheet.AddPack sheet) {
        List<JComponent> parts = new ArrayList<>();
        if (sheet.folders() == null) {
            parts.add(SheetParts.hint(Messages.get("sheet.folder.searching")));
        } else if (sheet.folders().isEmpty()) {
            parts.add(SheetParts.hint(Messages.get("sheet.folder.none")));
        }
        List<GameFolder> shownFolders = new ArrayList<>(sheet.folders() == null ? List.of() : sheet.folders());
        GameFolder chosen = sheet.chosen();
        // A folder picked by hand gets its own card under the ones found
        if (chosen != null && shownFolders.stream().noneMatch(folder -> folder.path().equals(chosen.path()))) {
            shownFolders.add(chosen);
        }
        for (GameFolder folder : shownFolders) {
            boolean selected = chosen != null && chosen.path().equals(folder.path());
            parts.add(new FolderCard(selected ? chosen : folder, selected,
                () -> dispatch.accept(new UiAction.ChooseFolder(folder.path()))));
        }

        JButton other = new JButton(Messages.get("sheet.folder.other"), Icons.get(Icons.Glyph.FOLDER));
        ButtonStyle.apply(other, ButtonStyle.SECONDARY, ButtonStyle.SMALL);
        other.addActionListener(e -> SheetParts.chooseFolder(this, chosen == null ? null : chosen.path())
            .ifPresent(path -> dispatch.accept(new UiAction.ChooseFolder(path))));
        parts.add(other);

        if (chosen != null && !chosen.isFree()) {
            parts.add(SheetParts.notice(Messages.get("sheet.folder.taken", chosen.usedBy()), Palette.error()));
            boolean allTaken = !shownFolders.isEmpty() && shownFolders.stream().noneMatch(GameFolder::isFree);
            if (allTaken) {
                parts.add(SheetParts.hint(Messages.get("sheet.folder.allTaken")));
            }
        } else if (chosen != null && chosen.jarCount() > 0) {
            parts.add(SheetParts.notice(Messages.get("sheet.folder.existingMods", chosen.jarCount()),
                Palette.update()));
        } else {
            parts.add(SheetParts.hint(Messages.get("sheet.folder.hint")));
        }
        return parts;
    }

    private JPanel footer(Sheet.AddPack sheet) {
        List<JButton> buttons = new ArrayList<>();
        if (sheet.step() == 1) {
            if (!sheet.firstRun()) {
                buttons.add(SheetParts.button("sheet.cancel", ButtonStyle.SECONDARY, dispatch, new UiAction.CloseSheet()));
            }
            JButton next = SheetParts.button("sheet.wizard.next", ButtonStyle.PRIMARY, dispatch,
                new UiAction.WizardNext());
            next.setEnabled(sheet.link() instanceof LinkCheck.Valid);
            buttons.add(next);
        } else {
            buttons.add(SheetParts.button("sheet.wizard.back", ButtonStyle.SECONDARY, dispatch,
                new UiAction.WizardBack()));
            JButton finish = SheetParts.button(sheet.firstRun() ? "sheet.wizard.start" : "sheet.wizard.finish",
                ButtonStyle.PRIMARY, dispatch, new UiAction.FinishAddPack());
            finish.setEnabled(sheet.chosen() != null && sheet.chosen().isFree());
            buttons.add(finish);
        }
        return SheetParts.footer(buttons.toArray(JButton[]::new));
    }
}
