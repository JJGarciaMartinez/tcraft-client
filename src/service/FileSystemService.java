package service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSystemService {

    public File getModsFolder() {
        // If the user running the launcher from the mods' folder, return the directory to the mods.
        File currentDir = new File(System.getProperty("user.dir"));
        if (currentDir.getName().equalsIgnoreCase("mods")) {
            return currentDir;
        }

        return new File(getMinecraftDirectory(), "mods");
    }

    private File getMinecraftDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            return new File(userHome, "AppData/Roaming/.minecraft");
        } else if (os.contains("mac")) {
            return new File(userHome, "Library/Application Support/minecraft");
        } else {
            return new File(userHome, ".minecraft");
        }
    }

    /**
     * Obtiene la lista de mods actuales en la carpeta de mods
     * @return Lista con los nombres de los archivos .jar encontrados
     */
    public List<String> getCurrentMods() {
        List<String> mods = new ArrayList<>();
        File modsFolder = getModsFolder();

        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            return mods;
        }

        File[] jarFiles = modsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                mods.add(jarFile.getName());
            }
        }

        return mods;
    }

    /**
     * Cuenta cuántos mods hay actualmente instalados
     * @return Número de archivos .jar en la carpeta de mods
     */
    public int getModCount() {
        return getCurrentMods().size();
    }
}
