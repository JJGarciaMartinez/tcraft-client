package util;

import model.ModInfo;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    public static List<ModInfo> parsearJsonSimple(String json) {
        List<ModInfo> lista = new ArrayList<>();
        json = json.replace("{", "").replace("}", "").replace("[", "").replace("]", "");
        String[] partes = json.split("},");

        for (String parte : partes) {
            String nombre = extraerValor(parte, "\"nombre\":");
            String url = extraerValor(parte, "\"url\":");
            if (nombre != null && url != null) {
                lista.add(new ModInfo(nombre, url));
            }
        }
        return lista;
    }

    private static String extraerValor(String texto, String llave) {
        int index = texto.indexOf(llave);
        if (index == -1) return null;
        int start = texto.indexOf("\"", index + llave.length()) + 1;
        int end = texto.indexOf("\"", start);
        return texto.substring(start, end);
    }
}
