package service;
import java.io.File;

public class FileSystemService {

    public File getModsFolder() {
        // If the user running the launcher from the mods' folder, return the directory to the mods.
        File rutaActual = new File(System.getProperty("user.dir"));
        if (rutaActual.getName().equalsIgnoreCase("mods")) {
            return rutaActual;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        File mcDir;

        if (os.contains("win")) {
            mcDir = new File(userHome, "AppData/Roaming/.minecraft");
        } else if (os.contains("mac")) {
            mcDir = new File(userHome, "Library/Application Support/minecraft");
        } else {
            mcDir = new File(userHome, ".minecraft");
        }
        return new File(mcDir, "mods");
    }
}
