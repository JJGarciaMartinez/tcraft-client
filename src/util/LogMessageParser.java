package util;

/**
 * Utility class for parsing log messages related to mod downloads and errors.
 * This class provides methods to extract mod names from various log message formats.
 * It handles messages indicating download status as well as error messages.
 * <p>
 * Example log messages:
 * - "Descargando: mod-name.jar"
 * - "Completado: mod-name.jar"
 * - "OK: mod-name.jar (ya existe)"
 * - "Error en mod-name.jar: HTTP error 404..."
 */
public class LogMessageParser {

    /**
     * Extracts the mod name from a log message.
     * The log message is expected to contain a colon (:) separating the status from the mod name.
     * Additional text such as "(ya existe)" is removed from the mod name.
     * @param message The log message to parse, e.g., "Descargando: mod-name.jar"
     * @return The extracted mod name, e.g., "mod-name.jar", or null if not found
     */
    public static String extractModName(String message) {
        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            if (parts.length > 1) {
                String name = parts[1].trim();
                // Remove any additional text in parentheses
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("(")).trim();
                }
                return name; // The name includes the .jar extension
            }
        }
        return null;
    }

    /**
     * Extracts the mod name from an error log message.
     * The error message is expected to start with "Error en" followed by the mod name
     * and a colon (:).
     * @param message The error log message to parse, e.g., "Error en mod-name.jar: HTTP error 404..."
     * @return The extracted mod name, e.g., "mod-name.jar", or null if not found
     */
    public static String extractModNameFromError(String message) {
        if (message.contains("Error en")) {
            String temp = message.substring(message.indexOf("Error en") + 9);
            if (temp.contains(":")) {
                return temp.substring(0, temp.indexOf(":")).trim(); // The name includes the .jar extension
            }
        }
        return null;
    }
}
