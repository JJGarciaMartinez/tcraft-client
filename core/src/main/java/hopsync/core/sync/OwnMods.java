package hopsync.core.sync;

import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The player's own mods: jars in {@code mods/} that HopSync did not install and the pack does not list.
 * HopSync never touches them.
 */
public final class OwnMods {

    private OwnMods() {
    }

    /**
     * @param managed  files recorded in the pack's state
     * @param manifest the pack's manifest, or {@code null} when it is not known yet
     * @return the remaining jars, in the order given
     */
    public static List<String> of(Collection<String> jarsOnDisk, Set<String> managed, Manifest manifest) {
        Set<String> fromPack = manifest == null ? Set.of()
            : manifest.mods().stream().map(ModEntry::file).collect(Collectors.toSet());
        return jarsOnDisk.stream()
            .filter(file -> !managed.contains(file) && !fromPack.contains(file))
            .toList();
    }
}
