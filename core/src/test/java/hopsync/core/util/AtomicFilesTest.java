package hopsync.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtomicFilesTest {

    @TempDir
    Path dir;

    @Test
    void createsParentsAndReplacesContent() throws IOException {
        Path file = dir.resolve("a/b/state.json");

        AtomicFiles.writeString(file, "first");
        AtomicFiles.writeString(file, "second ñ");

        assertEquals("second ñ", Files.readString(file));
        assertFalse(Files.exists(file.resolveSibling("state.json.tmp")));
    }
}
