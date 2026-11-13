package hopsync.core.port;

import hopsync.core.pack.PackSettings;

import java.io.IOException;

/**
 * Where the player's packs and preferences are kept between runs.
 */
public interface PackSettingsRepository {

    /**
     * @return the saved settings, or {@link PackSettings#EMPTY} when there are none or they cannot be read
     */
    PackSettings load();

    void save(PackSettings settings) throws IOException;
}
