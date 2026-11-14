package hopsync.desktop.screens.sheets;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import hopsync.core.pack.PackSettings;
import hopsync.desktop.config.AppVersion;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons;
import hopsync.desktop.kit.Palette;
import hopsync.desktop.kit.SegmentedControl;
import hopsync.desktop.store.Preferences;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.Sheet.AppSettings.Section;
import hopsync.desktop.store.UiAction;
import hopsync.platform.system.SystemInfoService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Preferences for every pack (check on start, chain the queue, parallel downloads) and what a bug report
 * needs to know about this copy of HopSync.
 */
public final class AppSettingsView extends JPanel {
    private final Consumer<UiAction> dispatch;
    private final SegmentedControl<Section> sections;
    private final JCheckBox checkOnStart = new JCheckBox(Messages.get("sheet.app.checkOnStart"));
    private final JCheckBox chainQueue = new JCheckBox(Messages.get("sheet.app.chainQueue"));
    private final JSpinner parallel = new JSpinner(new SpinnerNumberModel(Preferences.DEFAULT.parallelDownloads(),
        PackSettings.MIN_PARALLEL_DOWNLOADS, PackSettings.MAX_PARALLEL_DOWNLOADS, 1));
    private final JPanel general = SheetParts.column();
    private final JPanel about = SheetParts.column();
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel bottom = new JPanel(new BorderLayout());
    private boolean rendering;
    private Sheet.AppSettings shown;

    public AppSettingsView(Consumer<UiAction> dispatch, Path settingsFile) {
        super(new BorderLayout(0, UIScale.scale(16)));
        this.dispatch = dispatch;
        setOpaque(false);
        body.setOpaque(false);
        bottom.setOpaque(false);
        sections = new SegmentedControl<>(Arrays.asList(Section.values()),
            section -> dispatch.accept(new UiAction.OpenAppSettings(section)));
        for (Section section : Section.values()) {
            sections.setLabel(section, Messages.get("sheet.app.section." + section.name()));
        }

        checkOnStart.addActionListener(e -> changed());
        chainQueue.addActionListener(e -> changed());
        parallel.addChangeListener(e -> changed());
        parallel.getAccessibleContext().setAccessibleName(Messages.get("sheet.app.parallel"));
        JPanel parallelRow = new JPanel(new BorderLayout(UIScale.scale(12), 0));
        parallelRow.setOpaque(false);
        parallelRow.add(SheetParts.label(Messages.get("sheet.app.parallel")), BorderLayout.CENTER);
        parallelRow.add(parallel, BorderLayout.EAST);
        SheetParts.stack(general, 4, option(checkOnStart, "sheet.app.checkOnStartHint"));
        SheetParts.stack(general, 14, option(chainQueue, "sheet.app.chainQueueHint"));
        SheetParts.stack(general, 14, parallelRow);
        SheetParts.stack(general, 4, SheetParts.hint(Messages.get("sheet.app.parallelHint")));

        JButton copy = SheetParts.button("sheet.app.copyDiagnostics", ButtonStyle.SECONDARY, dispatch,
            new UiAction.CopyDiagnostics());
        copy.setIcon(Icons.get(Icons.Glyph.COPY));
        SheetParts.stack(about, 14, facts(settingsFile), copy);

        JPanel header = SheetParts.column();
        SheetParts.stack(header, 12, SheetParts.title(Messages.get("sheet.app.title")), sections);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void render(Sheet.AppSettings sheet) {
        if (sheet.equals(shown)) return;
        boolean sectionChanged = shown == null || shown.section() != sheet.section();
        shown = sheet;
        rendering = true;
        try {
            sections.setSelected(sheet.section());
            checkOnStart.setSelected(sheet.draft().checkOnStart());
            chainQueue.setSelected(sheet.draft().chainQueue());
            parallel.setValue(sheet.draft().parallelDownloads());
        } finally {
            rendering = false;
        }
        if (sectionChanged) {
            body.removeAll();
            body.add(sheet.section() == Section.GENERAL ? general : about, BorderLayout.NORTH);
            bottom.removeAll();
            JButton cancel = SheetParts.button(sheet.section() == Section.GENERAL ? "sheet.cancel" : "sheet.close",
                ButtonStyle.SECONDARY, dispatch, new UiAction.CloseSheet());
            bottom.add(sheet.section() == Section.GENERAL
                ? SheetParts.footer(cancel, SheetParts.button("sheet.save", ButtonStyle.PRIMARY, dispatch,
                    new UiAction.SaveAppSettings()))
                : SheetParts.footer(cancel), BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void changed() {
        if (rendering) return;
        dispatch.accept(new UiAction.SetPreferences(new Preferences(checkOnStart.isSelected(),
            chainQueue.isSelected(), (Integer) parallel.getValue())));
    }

    private static JComponent option(JCheckBox box, String hintKey) {
        box.setOpaque(false);
        box.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        JLabel hint = SheetParts.hint(Messages.get(hintKey));
        // Lines up with the check box text
        hint.setBorder(BorderFactory.createEmptyBorder(0, UIScale.scale(26), 0, 0));
        JPanel option = SheetParts.column();
        SheetParts.stack(option, 2, box, hint);
        return option;
    }

    private static JComponent facts(Path settingsFile) {
        String[][] rows = {
            {"sheet.app.version", AppVersion.VERSION_APP},
            {"sheet.app.system", SystemInfoService.getOS() + " " + System.getProperty("os.version")},
            {"sheet.app.java", System.getProperty("java.version") + " · " + System.getProperty("java.vendor")},
            {"sheet.app.scale", String.format(Locale.ROOT, "%.2f×", UIScale.getUserScaleFactor())
                + " · sun.java2d.uiScale=" + System.getProperty("sun.java2d.uiScale", "auto")},
            {"sheet.app.settingsFile", settingsFile.toString()},
        };
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, UIScale.scale(6), UIScale.scale(16));
        for (int row = 0; row < rows.length; row++) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            JLabel key = new JLabel(Messages.get(rows[row][0]));
            key.setForeground(Palette.textSecondary());
            grid.add(key, c);
            c.gridx = 1;
            c.weightx = 1;
            JLabel value = SheetParts.mono(rows[row][1]);
            value.setForeground(Palette.text());
            value.setToolTipText(rows[row][1]);
            grid.add(value, c);
        }
        return grid;
    }
}
