package hopsync.ui;

import hopsync.model.ConfigEntry;
import hopsync.model.Manifest;
import hopsync.model.ModEntry;
import hopsync.model.ModSource;
import hopsync.model.PackInfo;
import hopsync.sync.SyncPlan;
import hopsync.sync.SyncPlan.ConfigAction;
import hopsync.sync.SyncPlan.ConfigChange;
import hopsync.sync.SyncPlan.ModAction;
import hopsync.sync.SyncPlan.ModChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncPlanDialogTest {

    private static ModEntry mod(String file, long size) {
        return new ModEntry(file, file, "1", "", "", file, "https://x.org/" + file, "a".repeat(128), size, ModSource.MODRINTH);
    }

    private static SyncPlan plan() {
        ModEntry fresh = mod("new.jar", 2 * 1024 * 1024);
        ModEntry updated = mod("upd.jar", 512 * 1024);
        ModEntry kept = mod("kept.jar", 999);
        ConfigEntry edited = new ConfigEntry("edited.toml", "https://x.org/e", "a".repeat(128), 10, false);
        Manifest manifest = new Manifest(2, new PackInfo("My <Pack>", "1.2.0", "", null, "1.21.1", "neoforge"),
            "c".repeat(64), List.of(fresh, updated, kept), List.of(edited));
        return new SyncPlan(manifest,
            List.of(new ModAction(fresh, ModChange.NEW), new ModAction(updated, ModChange.UPDATE),
                new ModAction(kept, ModChange.UP_TO_DATE)),
            List.of("gone.jar"), List.of(new ConfigAction(edited, ConfigChange.KEEP_EDITED)), Map.of(), Map.of());
    }

    @Test
    void summarizesCountsAndSize() {
        String html = SyncPlanDialog.summaryHtml(plan());

        assertTrue(html.contains("My &lt;Pack&gt; 1.2.0"));
        assertTrue(html.contains("1 mod nuevo"));
        assertTrue(html.contains("1 mod a actualizar"));
        assertTrue(html.contains("1 mod a eliminar"));
        assertFalse(html.contains("config"));
        assertTrue(html.contains("2.5 MB"));
    }

    @Test
    void listsAffectedFiles() {
        String text = SyncPlanDialog.describe(plan());

        assertTrue(text.contains("new.jar"));
        assertTrue(text.contains("upd.jar"));
        assertTrue(text.contains("gone.jar"));
        assertTrue(text.contains("edited.toml"));
        assertFalse(text.contains("kept.jar"));
    }

    @Test
    void formatsBytes() {
        assertEquals("512 B", SyncPlanDialog.formatBytes(512));
        assertEquals("1.5 KB", SyncPlanDialog.formatBytes(1536));
        assertEquals("174.4 MB", SyncPlanDialog.formatBytes(182_835_135));
    }
}
