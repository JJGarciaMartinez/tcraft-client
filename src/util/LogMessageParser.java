package util;

public class LogMessageParser {

    public static String extractModName(String message) {
        // Extrae el nombre del mod de mensajes como:
        // "Descargando: mod-name.jar"
        // "Completado: mod-name.jar"
        // "OK: mod-name.jar (ya existe)"

        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            if (parts.length > 1) {
                String name = parts[1].trim();
                // Remover texto adicional como "(ya existe)"
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("(")).trim();
                }
                // Remover la extensión .jar para mostrar
                name = name.replace(".jar", "");
                return name;
            }
        }
        return null;
    }

    public static String extractModNameFromError(String message) {
        // Extrae el nombre del mod de mensajes como:
        // "Error en mod-name.jar: HTTP error 404..."

        if (message.contains("Error en")) {
            String temp = message.substring(message.indexOf("Error en") + 9);
            if (temp.contains(":")) {
                String name = temp.substring(0, temp.indexOf(":")).trim();
                name = name.replace(".jar", "");
                return name;
            }
        }
        return null;
    }
}
