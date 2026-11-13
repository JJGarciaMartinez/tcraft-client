package hopsync.core.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StateStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTrips() throws IOException {
        Path file = dir.resolve(".hopsync/state.json");
        LocalState state = new LocalState("abc",
            Map.of("a.jar", new FileRecord("1".repeat(128), 10, 1234, "6.0.8"), "b c [x].jar", new FileRecord("2".repeat(128), 0, 0)),
            Map.of("sub/x.toml", new FileRecord("3".repeat(128), 5, 99)));

        new StateStore(file).save(state);

        assertEquals(state, new StateStore(file).load());
        assertFalse(Files.exists(file.resolveSibling("state.json.tmp")));
    }

    @Test
    void recordsWithoutVersionStillLoad() throws IOException {
        Path file = Files.writeString(dir.resolve("state.json"), "{\"format\": 1, \"content_hash\": \"abc\", "
            + "\"mods\": {\"a.jar\": {\"sha512\": \"" + "1".repeat(128) + "\", \"size\": 10, \"modified\": 5}}, \"configs\": {}}");

        FileRecord record = new StateStore(file).load().mods().get("a.jar");

        assertEquals(new FileRecord("1".repeat(128), 10, 5), record);
        assertEquals(null, record.version());
    }

    @Test
    void missingFileIsEmptyState() {
        assertEquals(LocalState.empty(), new StateStore(dir.resolve("none.json")).load());
    }

    @Test
    void corruptFileIsEmptyState() throws IOException {
        Path file = Files.writeString(dir.resolve("state.json"), "{not json");
        assertEquals(LocalState.empty(), new StateStore(file).load());

        Files.writeString(file, "{\"format\": 1, \"mods\": {\"a.jar\": {\"sha512\": 5}}}");
        assertEquals(LocalState.empty(), new StateStore(file).load());
    }

    @Test
    void unknownFormatIsEmptyState() throws IOException {
        Path file = Files.writeString(dir.resolve("state.json"), "{\"format\": 99, \"mods\": {}, \"configs\": {}}");
        assertEquals(LocalState.empty(), new StateStore(file).load());
    }
}
