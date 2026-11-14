package hopsync.desktop.store;

import java.nio.file.Path;

/**
 * A folder the player can bind a pack to: a launcher instance found on disk or one they picked.
 *
 * @param name     instance name, or {@code null} for the official launcher and folders picked by hand
 * @param jarCount jars already in its {@code mods/} folder
 * @param usedBy   name of the pack that already uses it (same, inside or containing), or {@code null}
 */
public record GameFolder(Kind kind, String name, Path path, int jarCount, String usedBy) {

    public enum Kind { OFFICIAL, PRISM, CURSEFORGE, MODRINTH, OTHER }

    public boolean isFree() {
        return usedBy == null;
    }
}
