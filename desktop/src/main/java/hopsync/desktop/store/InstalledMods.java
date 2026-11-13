package hopsync.desktop.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What is in a pack's {@code mods/} folder and what HopSync's state says about it.
 *
 * @param jars     jar file names on disk
 * @param sizes    size in bytes of each jar
 * @param managed  files HopSync installed (from {@code .hopsync/state.json})
 * @param versions version recorded for managed files, when known
 */
public record InstalledMods(List<String> jars, Map<String, Long> sizes, Set<String> managed,
                            Map<String, String> versions) {

    public static final InstalledMods NONE = new InstalledMods(List.of(), Map.of(), Set.of(), Map.of());

    public InstalledMods {
        jars = List.copyOf(jars);
        sizes = Map.copyOf(sizes);
        managed = Set.copyOf(managed);
        versions = Map.copyOf(versions);
    }

    /** Only jar names, for tests and callers that know nothing else. */
    public static InstalledMods of(List<String> jars) {
        return new InstalledMods(jars, Map.of(), Set.of(), Map.of());
    }
}
