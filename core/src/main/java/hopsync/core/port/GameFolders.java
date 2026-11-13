package hopsync.core.port;

import java.io.File;

/**
 * Where the game folder being synced keeps its mods, its configs and HopSync's record of installed files.
 */
public interface GameFolders {
    File getModsFolder();

    File getConfigFolder();

    File getStateFile();
}
