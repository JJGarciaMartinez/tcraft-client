package hopsync.desktop.screens.pack;

import org.junit.jupiter.api.Test;

import static hopsync.desktop.store.StoreFixtures.plan;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanSummaryTest {

    @Test
    void countsWhatThePlanWillDo() {
        PlanSummary summary = PlanSummary.of(plan());

        assertEquals(1, summary.newMods());
        assertEquals(1, summary.updatedMods());
        assertEquals(1, summary.removedMods());
        assertEquals(0, summary.configs());
        assertEquals(3, summary.changes());
        assertEquals(200, summary.bytes(), "only the new and updated files are downloaded");
    }

    @Test
    void formatsSizes() {
        assertEquals("512 B", PlanSummary.formatBytes(512));
        assertEquals("1.5 KB", PlanSummary.formatBytes(1536));
        assertEquals("174.4 MB", PlanSummary.formatBytes(182_835_135));
    }
}
