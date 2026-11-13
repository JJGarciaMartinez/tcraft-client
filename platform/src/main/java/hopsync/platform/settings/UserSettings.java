package hopsync.platform.settings;

import java.nio.file.Path;

/**
 * Choices the player makes in the settings dialog.
 *
 * @param manifestUrl https URL of the modpack manifest
 * @param gameDir     Minecraft game folder (or launcher instance folder) that holds mods/ and config/
 */
public record UserSettings(String manifestUrl, Path gameDir) {
}
