package hopsync.sync;

import java.util.Map;

/**
 * Files installed by HopSync in a game folder, keyed by mod file name and by config path.
 * Only files listed here are ever removed or treated as unmodified by the player.
 *
 * @param contentHash content hash of the last manifest fully applied, or {@code null}
 */
public record LocalState(String contentHash, Map<String, FileRecord> mods, Map<String, FileRecord> configs) {

    public LocalState {
        mods = Map.copyOf(mods);
        configs = Map.copyOf(configs);
    }

    public static LocalState empty() {
        return new LocalState(null, Map.of(), Map.of());
    }
}
