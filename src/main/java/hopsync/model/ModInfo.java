package hopsync.model;

/**
 * What the UI shows for a mod, either from the manifest or from a jar found on disk.
 *
 * @param file        the jar file name; identifies the mod card across update events
 * @param name        display name
 * @param version     version label, may be empty
 * @param description short description, may be empty
 * @param author      author, may be empty
 */
public record ModInfo(String file, String name, String version, String description, String author) {

    public static ModInfo of(ModEntry entry) {
        return new ModInfo(entry.file(), entry.name(), entry.version(), entry.description(), entry.author());
    }

    public static ModInfo installed(String file) {
        return new ModInfo(file, file, "Instalado", "", "");
    }
}
