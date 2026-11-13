package hopsync.desktop.ui;

import hopsync.core.manifest.PackInfo;
import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlan.ConfigChange;
import hopsync.core.sync.SyncPlan.ModChange;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Asks the player to confirm a sync plan before any file is written or deleted.
 */
public final class SyncPlanDialog {

    private SyncPlanDialog() {
    }

    /**
     * Shows the plan summary and returns true if the player chose to apply it. Must run on the EDT.
     */
    public static boolean confirm(Component parent, SyncPlan plan) {
        JTextArea details = new JTextArea(describe(plan));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(details);
        scroll.setPreferredSize(new Dimension(480, 260));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel(summaryHtml(plan)), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        Object[] options = {"Aplicar", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(parent, panel, "Cambios a aplicar",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return choice == 0;
    }

    static String summaryHtml(SyncPlan plan) {
        PackInfo pack = plan.manifest().pack();
        List<String> lines = new ArrayList<>();
        lines.add("<b>" + escape(pack.name()) + " " + escape(pack.version()) + "</b>");
        addCount(lines, plan.count(ModChange.NEW), "mod nuevo", "mods nuevos");
        addCount(lines, plan.count(ModChange.UPDATE), "mod a actualizar", "mods a actualizar");
        addCount(lines, plan.removals().size(), "mod a eliminar", "mods a eliminar");
        addCount(lines, plan.configDownloads().size(), "config a instalar", "configs a instalar");
        lines.add("Descarga total: " + formatBytes(plan.downloadBytes()));
        return "<html>" + String.join("<br>", lines) + "</html>";
    }

    static String describe(SyncPlan plan) {
        StringBuilder sb = new StringBuilder();
        section(sb, "Nuevos", plan.mods().stream()
            .filter(a -> a.change() == ModChange.NEW).map(a -> a.mod().file()).toList());
        section(sb, "Se actualizarán", plan.mods().stream()
            .filter(a -> a.change() == ModChange.UPDATE).map(a -> a.mod().file()).toList());
        section(sb, "Se eliminarán (los instaló HopSync y ya no están en el pack)", plan.removals());
        section(sb, "Configs que se instalarán", plan.configDownloads().stream()
            .map(a -> a.config().path()).toList());
        section(sb, "Configs que se conservan porque las modificaste", plan.configs().stream()
            .filter(a -> a.change() == ConfigChange.KEEP_EDITED).map(a -> a.config().path()).toList());
        sb.append("Los mods que instalaste por tu cuenta no se tocan.");
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, List<String> items) {
        if (items.isEmpty()) return;
        sb.append(title).append(" (").append(items.size()).append("):\n");
        items.forEach(item -> sb.append("  • ").append(item).append('\n'));
        sb.append('\n');
    }

    private static void addCount(List<String> lines, long count, String singular, String plural) {
        if (count > 0) lines.add(count + " " + (count == 1 ? singular : plural));
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
