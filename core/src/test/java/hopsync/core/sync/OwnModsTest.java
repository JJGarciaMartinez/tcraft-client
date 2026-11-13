package hopsync.core.sync;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static hopsync.core.sync.Fixtures.manifest;
import static hopsync.core.sync.Fixtures.mod;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnModsTest {

    @Test
    void excludesWhatThePackListsAndWhatHopSyncInstalled() {
        var manifest = manifest(List.of(mod("pack.jar", "P")), List.of());

        List<String> own = OwnMods.of(List.of("pack.jar", "left-the-pack.jar", "mine.jar", "also-mine.jar"),
            Set.of("left-the-pack.jar"), manifest);

        assertEquals(List.of("mine.jar", "also-mine.jar"), own);
    }

    @Test
    void withoutTheManifestOnlyManagedFilesAreExcluded() {
        assertEquals(List.of("pack.jar", "mine.jar"),
            OwnMods.of(List.of("pack.jar", "managed.jar", "mine.jar"), Set.of("managed.jar"), null));
    }
}
