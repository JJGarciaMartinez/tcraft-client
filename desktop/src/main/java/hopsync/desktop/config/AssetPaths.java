package hopsync.desktop.config;

/**
 * A utility class containing static constants for file paths under the assets directory.
 * This class provides paths to various asset files such as images and fonts.
 * It is not intended to be instantiated.
 * <p>
 * The class organizes paths by category:
 * - Image asset file paths.
 * - Font asset file paths.
 * <p>
 * All paths are relative to the base assets' directory.
 */
public class AssetPaths {
    // Base directory
    private static final String ASSETS_DIR = "assets/";

    // Images
    public static final String MINECRAFT_TITLE = ASSETS_DIR + "minecraft_title.png";
    public static final String UPDATE_BUTTON = ASSETS_DIR + "update_button.png";
    public static final String CANCEL_BUTTON = ASSETS_DIR + "cancel_button.png";
    public static final String REFRESH_BUTTON = ASSETS_DIR + "refresh_button.png";

    // Fonts
    public static final String MINECRAFT_FONT = ASSETS_DIR + "font/minecraft-mojangles.ttf";

    /**
     * Private constructor to prevent instantiation of this utility class.
     * The {@code AssetPaths} class is designed to serve as a container for
     * static constants defining file paths for assets such as images and fonts.
     */
    private AssetPaths() {
        // A private constructor to prevent instantiation
    }
}

