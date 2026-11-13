package hopsync.core.sync;

import hopsync.core.util.AtomicFiles;
import hopsync.core.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads and writes {@link LocalState} as JSON. A missing or unreadable file yields an empty state,
 * which is the safe default: with no record of installed files, nothing is ever deleted.
 */
public class StateStore {
    private static final System.Logger LOG = System.getLogger(StateStore.class.getName());
    private static final int FORMAT = 1;

    private final Path file;

    public StateStore(Path file) {
        this.file = file;
    }

    public LocalState load() {
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return LocalState.empty();
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not read " + file + ", starting fresh", e);
            return LocalState.empty();
        }
        try {
            Map<?, ?> root = (Map<?, ?>) Json.parse(json);
            if (!Long.valueOf(FORMAT).equals(root.get("format"))) {
                LOG.log(System.Logger.Level.WARNING, "Unsupported state format in " + file + ", starting fresh");
                return LocalState.empty();
            }
            return new LocalState(
                (String) root.get("content_hash"),
                readRecords(root.get("mods")),
                readRecords(root.get("configs"))
            );
        } catch (IOException | ClassCastException | NullPointerException e) {
            LOG.log(System.Logger.Level.WARNING, "Corrupt state file " + file + ", starting fresh", e);
            return LocalState.empty();
        }
    }

    /**
     * Writes the state atomically so a crash never leaves a half-written file.
     */
    public void save(LocalState state) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", FORMAT);
        root.put("content_hash", state.contentHash());
        root.put("mods", writeRecords(state.mods()));
        root.put("configs", writeRecords(state.configs()));

        AtomicFiles.writeString(file, Json.write(root));
    }

    private static Map<String, FileRecord> readRecords(Object value) {
        Map<String, FileRecord> records = new HashMap<>();
        if (value == null) {
            return records;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Map<?, ?> record = (Map<?, ?>) entry.getValue();
            records.put((String) entry.getKey(), new FileRecord(
                (String) record.get("sha512"),
                (Long) record.get("size"),
                (Long) record.get("modified"),
                // Absent in files written before versions were kept
                (String) record.get("version")
            ));
        }
        return records;
    }

    private static Map<String, Object> writeRecords(Map<String, FileRecord> records) {
        Map<String, Object> out = new TreeMap<>();
        records.forEach((key, record) -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sha512", record.sha512());
            r.put("size", record.size());
            r.put("modified", record.modifiedMillis());
            if (record.version() != null) {
                r.put("version", record.version());
            }
            out.put(key, r);
        });
        return out;
    }
}
