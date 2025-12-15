package util;

import model.ModInfo;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    @org.jetbrains.annotations.NotNull
    public static List<ModInfo> parsearJsonSimple(String json) {
        List<ModInfo> modsList = new ArrayList<>();

        // Eliminar espacios en blanco y validar que no esté vacío
        json = json.trim();
        if (json.isEmpty()) {
            System.err.println("Error: JSON vacío");
            return modsList;
        }

        System.out.println("Parsing JSON...");
        System.out.println("JSON: " + json);

        // Buscar el array de mods dentro del JSON
        String modsArrayKey = "\"mods\":";
        int modsArrayStart = json.indexOf(modsArrayKey);
        if (modsArrayStart == -1) {
            System.err.println("Error: No se encontró el array 'mods' en el JSON");
            return modsList;
        }

        // Encontrar el inicio del array después de "mods":
        int arrayBracketStart = json.indexOf("[", modsArrayStart);
        if (arrayBracketStart == -1) {
            System.err.println("Error: No se encontró el inicio del array de mods");
            return modsList;
        }

        // Encontrar el final del array
        int arrayBracketEnd = json.indexOf("]", arrayBracketStart);
        if (arrayBracketEnd == -1) {
            System.err.println("Error: No se encontró el final del array de mods");
            return modsList;
        }

        // Extraer solo el contenido del array de mods
        String modsArray = json.substring(arrayBracketStart + 1, arrayBracketEnd).trim();

        if (modsArray.isEmpty()) {
            System.out.println("Advertencia: El array de mods está vacío");
            return modsList;
        }

        // Dividir por las llaves de cierre seguidas de coma
        String[] modEntries = modsArray.split("},\\s*\\{");

        int modsIgnorados = 0;
        for (int i = 0; i < modEntries.length; i++) {
            String modEntry = modEntries[i].trim();
            System.out.println("Procesando entrada de mod " + (i + 1) + ": " + modEntry.substring(0, Math.min(50, modEntry.length())) + "...");

            if (modEntry.isEmpty()) continue;

            // Limpiar las llaves restantes si existen
            modEntry = modEntry.replace("{", "").replace("}", "");

            String name = extraerValor(modEntry, "\"name\":");
            String version = extraerValor(modEntry, "\"version\":");
            String description = extraerValor(modEntry, "\"description\":");
            String author = extraerValor(modEntry, "\"author\":");
            String url = extraerValor(modEntry, "\"url\":");

            // Validar que al menos tenga nombre y url (campos esenciales)
            if (name != null && url != null) {
                // Si faltan campos opcionales, usar valores por defecto
                if (version == null) version = "desconocida";
                if (description == null) description = "Sin descripción";
                if (author == null) author = "Desconocido";

                modsList.add(new ModInfo(name, version, description, author, url));
                System.out.println("✓ Mod añadido: " + name + " v" + version + " por " + author);
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

    private static String extraerValor(String texto, String key) {
        int index = texto.indexOf(key);
        if (index == -1) return null;
        int start = texto.indexOf("\"", index + key.length()) + 1;
        int end = texto.indexOf("\"", start);
        if (start == 0 || end == -1) return null;
        return texto.substring(start, end);
    }
}


