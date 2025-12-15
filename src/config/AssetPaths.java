package config;

/**
 * Routes static to assets folder
 */
public class AssetPaths {
    // Base directory
    private static final String ASSETS_DIR = "assets/";

    // Images
    public static final String MINECRAFT_TITLE = ASSETS_DIR + "minecraft_title.png";
    public static final String CHECKMARK_ICON = ASSETS_DIR + "checkmark_icon.png";
    public static final String DANGERMARK_ICON = ASSETS_DIR + "dangermark_icon.png";
    public static final String LOAD_ICON = ASSETS_DIR + "load_icon.png";
    public static final String WARNINGMARK_ICON = ASSETS_DIR + "warningmark_icon.png";
    public static final String XMARK_ICON = ASSETS_DIR + "xmark_icon.png";
    public static final String SYSTEM_ICON = ASSETS_DIR + "system_icon.png";

    // Fonts
    public static final String MINECRAFT_FONT = ASSETS_DIR + "font/minecraft-mojangles.ttf";

    private AssetPaths() {
        // A private constructor to prevent instantiation
    }
}

