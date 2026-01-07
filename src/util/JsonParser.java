package util;

import model.ModInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple JSON parser for extracting mod information from a JSON string.
 * This parser is designed to handle a specific JSON structure containing an array of mods.
 * Each mod is expected to have fields such as name, version, description, author, and url.
 * <p>
 * The parser does not rely on external libraries and performs basic string manipulation
 * to extract the required information. It includes error handling for malformed entries
 * and logs warnings for any mods that cannot be parsed correctly.
 */
public class JsonParser {

    /**
     * Parses a JSON string to extract a list of ModInfo objects.
     * The JSON is expected to contain an array of mods under the "mods" key.
     * @param json the JSON string to parse
     * @return a list of ModInfo objects extracted from the JSON
     */
    public static List<ModInfo> jsonSimpleParse(String json) {
        List<ModInfo> modsList = new ArrayList<>();

        // Delete whitespaces at the beginning and the end
        json = json.trim();
        if (json.isEmpty()) {
            System.err.println("Error: JSON vacío");
            return modsList;
        }

        System.out.println("Parsing JSON...");
        System.out.println("JSON: " + json);

        // Search for the "mods" array key
        String modsArrayKey = "\"mods\":";
        int modsArrayStart = json.indexOf(modsArrayKey);
        if (modsArrayStart == -1) {
            System.err.println("Error: No se encontró el array 'mods' en el JSON");
            return modsList;
        }

        // Find the start of the array
        int arrayBracketStart = json.indexOf("[", modsArrayStart);
        if (arrayBracketStart == -1) {
            System.err.println("Error: No se encontró el inicio del array de mods");
            return modsList;
        }

        // Find the end of the array
        int arrayBracketEnd = json.indexOf("]", arrayBracketStart);
        if (arrayBracketEnd == -1) {
            System.err.println("Error: No se encontró el final del array de mods");
            return modsList;
        }

        // Extract the mods array content
        String modsArray = json.substring(arrayBracketStart + 1, arrayBracketEnd).trim();

        if (modsArray.isEmpty()) {
            System.out.println("Advertencia: El array de mods está vacío");
            return modsList;
        }

        // Split the mods array into individual mod entries
        String[] modEntries = modsArray.split("},\\s*\\{");

        int modsIgnorados = 0;
        for (int i = 0; i < modEntries.length; i++) {
            String modEntry = modEntries[i].trim();
            System.out.println("Procesando entrada de mod " + (i + 1) + ": " + modEntry.substring(0, Math.min(50, modEntry.length())) + "...");

            if (modEntry.isEmpty()) continue;

            // Adjust braces for the first and last entries
            modEntry = modEntry.replace("{", "").replace("}", "");

            String name = extractValue(modEntry, "\"name\":");
            String version = extractValue(modEntry, "\"version\":");
            String description = extractValue(modEntry, "\"description\":");
            String author = extractValue(modEntry, "\"author\":");
            String url = extractValue(modEntry, "\"url\":");
            String configUrl = extractValue(modEntry, "\"configUrl\":");
            String configName = extractValue(modEntry, "\"configName\":");

            // Validate required fields
            if (name != null && url != null) {
                // If optional fields are missing, set default values
                if (version == null) version = "desconocida";
                if (description == null) description = "Sin descripción";
                if (author == null) author = "Desconocido";

                modsList.add(new ModInfo(name, version, description, author, url, configUrl, configName));
                System.out.println("✓ Mod añadido: " + name + " v" + version + " por " + author +
                                   (configUrl != null ? " [con config]" : ""));
            } else {
                modsIgnorados++;
                System.err.println("Advertencia: Mod mal formado ignorado (name o url faltante): " + modEntry.substring(0, Math.min(50, modEntry.length())));
            }
        }

        if (modsIgnorados > 0) {
            System.err.println("Total de mods ignorados por estar mal formados: " + modsIgnorados);
        }

        System.out.println("Total de mods parseados correctamente: " + modsList.size());

        return modsList;
    }

    /**
     * Extracts the value associated with a given key from a JSON-like string.
     * @param texto the input string containing key-value pairs
     * @param key the key whose value needs to be extracted
     * @return the extracted value, or null if the key is not found
     */
    private static String extractValue(String texto, String key) {
        int index = texto.indexOf(key);
        if (index == -1) return null;
        int start = texto.indexOf("\"", index + key.length()) + 1;
        int end = texto.indexOf("\"", start);
        if (start == 0 || end == -1) return null;
        return texto.substring(start, end);
    }
}


