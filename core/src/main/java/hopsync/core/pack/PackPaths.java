package hopsync.core.pack;

import java.nio.file.Path;

/**
 * The folders and files HopSync uses inside one pack's game folder.
 */
public record PackPaths(Path gameDir) {

    public Path mods() {
        return gameDir.resolve("mods");
    }

    public Path config() {
        return gameDir.resolve("config");
    }

    /** HopSync's record of the files it installed in this folder. */
    public Path stateFile() {
        return gameDir.resolve(".hopsync").resolve("state.json");
    }

    /** Held while applying, so two HopSync processes never write the same folder at once. */
    public Path applyLock() {
        return gameDir.resolve(".hopsync").resolve("apply.lock");
    }
}
