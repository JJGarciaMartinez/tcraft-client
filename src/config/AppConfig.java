package config;

/**
 * The AppConfig class contains static constants for application-wide configuration.
 * This class serves as a central place to define key constants such as the application's
 * version and external resource URLs.
 * <p>
 * It is not intended to be instantiated as it functions as a utility class for storing
 * static configuration values.
 * <p>
 * Constants included in this class:
 * - URL_MANIFEST: The URL to the external manifest file containing a list of mods.
 * - VERSION_APP: The current version of the application.
 */
public class AppConfig {
    public static final String URL_MANIFEST = "https://raw.githubusercontent.com/JJGarciaMartinez/tcraft-mods-list/main/modList/mod-list.json";
    public static final String VERSION_APP = "1.0.0";
}
