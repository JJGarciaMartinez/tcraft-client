package util;

import model.ModInfo;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    public static List<ModInfo> parsearJsonSimple(String json) {
        List<ModInfo> modsList = new ArrayList<>();
        // Primero eliminar solo los corchetes externos
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        // Dividir por las llaves de cierre seguidas de coma
        String[] modEntries = json.split("},\\s*\\{");

        int modsIgnore = 0;
        for (int i = 0; i < modEntries.length; i++) {
            String modEntry = modEntries[i].trim();
            System.out.println("Procesando entrada de mod " + (i + 1) + ": " + modEntry.substring(0, Math.min(50, modEntry.length())) + "...");

            if (modEntry.isEmpty()) continue;

            // Limpiar las llaves restantes si existen
            modEntry = modEntry.replace("{", "").replace("}", "");

            String name = extraerValor(modEntry, "\"name\":");
            String url = extraerValor(modEntry, "\"url\":");

            if (name != null && url != null) {
                modsList.add(new ModInfo(name, url));
                System.out.println("✓ Mod añadido: " + name);
            } else {
                modsIgnore++;
                System.err.println("Advertencia: Mod mal formado ignorado (name o url faltante): " + modEntry.substring(0, Math.min(50, modEntry.length())));
            }
        }

        if (modsIgnore > 0) {
            System.err.println("Total de mods ignorados por estar mal formados: " + modsIgnore);
        }

        System.out.println("Total de mods parseados correctamente: " + modsList.size());

        return modsList;
    }

    private static String extraerValor(String texto, String key) {
        int index = texto.indexOf(key);
        if (index == -1) return null;
        int start = texto.indexOf("\"", index + key.length()) + 1;
        int end = texto.indexOf("\"", start);
        return texto.substring(start, end);
    }
}
