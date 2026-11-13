package hopsync.desktop.screens.pack;

import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.UiAction;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The ⋯ menu of the pack header. Actions that would change a pack while it updates are disabled.
 */
final class PackMenu extends JPopupMenu {
    private final JMenuItem check;
    private final JMenuItem settings;
    private final JMenuItem remove;
    private String packId;

    PackMenu(Consumer<UiAction> dispatch) {
        check = item("menu.check", Icons.Glyph.REFRESH, () -> dispatch.accept(forPack(UiAction.CheckPack::new)));
        add(check);
        add(item("menu.openFolder", Icons.Glyph.FOLDER, () -> dispatch.accept(forPack(UiAction.OpenFolder::new))));
        add(item("menu.copyLink", Icons.Glyph.COPY, () -> dispatch.accept(forPack(UiAction.CopyPackLink::new))));
        settings = item("menu.settings", Icons.Glyph.GEAR, () -> dispatch.accept(new UiAction.OpenSettings()));
        add(settings);
        addSeparator();
        remove = item("menu.remove", Icons.Glyph.TRASH, () -> dispatch.accept(new UiAction.AskRemovePack()));
        remove.setForeground(Palette.error());
        remove.setIcon(Icons.get(Icons.Glyph.TRASH, 16, Palette.error()));
        add(remove);
    }

    void setPack(PackUiState pack) {
        packId = pack.packId();
        boolean busy = pack.phase().isBusy();
        check.setEnabled(!busy);
        settings.setEnabled(!busy);
        remove.setEnabled(!busy);
    }

    private UiAction forPack(Function<String, UiAction> action) {
        return action.apply(packId);
    }

    private static JMenuItem item(String key, Icons.Glyph glyph, Runnable onClick) {
        JMenuItem item = new JMenuItem(Messages.get(key), Icons.get(glyph));
        item.addActionListener(e -> onClick.run());
        return item;
    }
}
