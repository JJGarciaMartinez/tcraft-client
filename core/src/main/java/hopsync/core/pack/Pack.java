package hopsync.core.pack;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A modpack the player keeps in sync, bound to its own game folder.
 *
 * @param id          random UUID, stable for the pack's life; events and sessions are keyed by it
 * @param manifestUrl https URL of the pack manifest
 * @param gameDir     absolute game folder (or launcher instance folder) holding {@code mods/} and {@code config/}
 * @param name        name from the last manifest fetched, only for display before the network answers;
 *                    empty until then
 */
public record Pack(String id, String manifestUrl, Path gameDir, String name) {

    public Pack {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(manifestUrl, "manifestUrl");
        gameDir = gameDir.toAbsolutePath();
        name = name == null ? "" : name;
    }

    public PackPaths paths() {
        return new PackPaths(gameDir);
    }

    public Pack withManifestUrl(String url) {
        return new Pack(id, url, gameDir, name);
    }

    public Pack withGameDir(Path dir) {
        return new Pack(id, manifestUrl, dir, name);
    }

    public Pack withName(String newName) {
        return new Pack(id, manifestUrl, gameDir, newName);
    }
}
