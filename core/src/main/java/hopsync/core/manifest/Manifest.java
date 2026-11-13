package hopsync.core.manifest;

import java.util.List;

/**
 * A HopSync modpack manifest (format version 2), as published by the modpack repository.
 *
 * @param contentHash SHA-256 over mods and configs; unchanged hash means unchanged content
 */
public record Manifest(int formatVersion, PackInfo pack, String contentHash,
                       List<ModEntry> mods, List<ConfigEntry> configs) {

    public static final int SUPPORTED_FORMAT_VERSION = 2;

    public Manifest {
        mods = List.copyOf(mods);
        configs = List.copyOf(configs);
    }
}
