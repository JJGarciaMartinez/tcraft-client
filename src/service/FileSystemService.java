package service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A service for accessing and managing the file system related to a Minecraft installation,
 * specifically focusing on the mods folder and its contents.
 */
public class FileSystemService {

    /**
     * Retrieves the directory where Minecraft mods are stored. If the launcher is
     * being run from a directory named "mods," that directory is returned. Otherwise,
     * it returns the "mods" folder located inside the Minecraft directory.
     *
     * @return A {@code File} object representing the path to the mods' folder. If the
     *         current working directory is named "mods," it returns that directory.
     *         Otherwise, it constructs and returns the "mods" folder within the
     *         Minecraft directory.
     */
    public File getModsFolder() {
        // If the user running the launcher from the mods' folder, return the directory to the mods.
        File currentDir = new File(System.getProperty("user.dir"));
        if (currentDir.getName().equalsIgnoreCase("mods")) {
            return currentDir;
        }

        return new File(getMinecraftDirectory(), "mods");
    }

    /**
     * Retrieves the directory where Minecraft is installed based on the user's operating system.
     * This method determines the folder path by checking the OS type and constructing the appropriate
     * directory path for Windows, macOS, or Linux.
     *
     * @return A {@code File} object representing the Minecraft installation directory.
     *         For Windows, this is typically "AppData/Roaming/.minecraft".
     *         For macOS, this is typically "Library/Application Support/minecraft".
     *         For Linux, this is typically ".minecraft" in the user's home directory.
     */
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
     * Retrieves a list of currently installed Minecraft mods by identifying all files
     * with a ".jar" extension in the mods' folder.
     *
     * @return A list of strings representing the names of the ".jar" files found
     *         in the mods folder. If the folder does not exist or no mods are found,
     *         an empty list is returned.
     */
    public List<String> getCurrentMods() {
        List<String> mods = new ArrayList<>();
        File modsFolder = getModsFolder();

        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            return mods;
        }

        File[] jarFiles = modsFolder.listFiles((_, name) -> name.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                mods.add(jarFile.getName());
            }
        }

        return mods;
    }
}
