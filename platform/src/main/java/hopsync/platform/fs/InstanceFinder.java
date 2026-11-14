package hopsync.platform.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Finds the game folders a player is likely to use: the official launcher's {@code .minecraft} and the
 * instances of Prism Launcher, CurseForge and the Modrinth App, in each operating system's usual place.
 */
public final class InstanceFinder {
    private static final System.Logger LOG = System.getLogger(InstanceFinder.class.getName());

    public enum Launcher { OFFICIAL, PRISM, CURSEFORGE, MODRINTH }

    /**
     * @param name     instance name, or {@code null} for the official launcher's single folder
     * @param jarCount jars already in its {@code mods/} folder
     */
    public record Instance(Launcher launcher, String name, Path gameDir, int jarCount) {
    }

    private final String osName;
    private final Map<String, String> env;
    private final Path home;

    public InstanceFinder(String osName, Map<String, String> env, Path home) {
        this.osName = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        this.env = env;
        this.home = home;
    }

    public static InstanceFinder forCurrentUser() {
        return new InstanceFinder(System.getProperty("os.name"), System.getenv(), Path.of(System.getProperty("user.home")));
    }

    /** Every folder found, official first, then each launcher's instances by name. */
    public List<Instance> find() {
        List<Instance> found = new ArrayList<>();
        Path official = DefaultGameDir.forSystem(osName, home.toString(), home);
        if (Files.isDirectory(official)) {
            found.add(new Instance(Launcher.OFFICIAL, null, official, jarCount(official)));
        }
        for (Path dir : List.of(dataDir().resolve("PrismLauncher").resolve("instances"))) {
            for (Path instance : children(dir)) {
                // Newer instances keep the game in minecraft/, older ones in .minecraft/
                Path game = Files.isDirectory(instance.resolve("minecraft")) ? instance.resolve("minecraft")
                    : instance.resolve(".minecraft");
                if (Files.isDirectory(game)) {
                    found.add(new Instance(Launcher.PRISM, prismName(instance), game, jarCount(game)));
                }
            }
        }
        for (Path instance : children(home.resolve("curseforge").resolve("minecraft").resolve("Instances"))) {
            found.add(new Instance(Launcher.CURSEFORGE, instance.getFileName().toString(), instance, jarCount(instance)));
        }
        for (String app : List.of("ModrinthApp", "com.modrinth.theseus")) {
            for (Path profile : children(dataDir().resolve(app).resolve("profiles"))) {
                found.add(new Instance(Launcher.MODRINTH, profile.getFileName().toString(), profile, jarCount(profile)));
            }
        }
        return found;
    }

    /** Where launchers keep their data: %APPDATA%, ~/Library/Application Support or $XDG_DATA_HOME. */
    Path dataDir() {
        if (osName.contains("win")) {
            String appData = env.get("APPDATA");
            return appData != null && !appData.isBlank() ? Path.of(appData) : home.resolve("AppData").resolve("Roaming");
        }
        if (osName.contains("mac")) {
            return home.resolve("Library").resolve("Application Support");
        }
        String xdg = env.get("XDG_DATA_HOME");
        return xdg != null && !xdg.isBlank() && Path.of(xdg).isAbsolute() ? Path.of(xdg) : home.resolve(".local").resolve("share");
    }

    /** Prism keeps the display name in instance.cfg; the folder name is the fallback. */
    private static String prismName(Path instance) {
        Path config = instance.resolve("instance.cfg");
        try (Stream<String> lines = Files.lines(config, StandardCharsets.UTF_8)) {
            return lines.filter(line -> line.startsWith("name="))
                .map(line -> line.substring("name=".length()).trim())
                .filter(name -> !name.isEmpty())
                .findFirst().orElse(instance.getFileName().toString());
        } catch (IOException | RuntimeException e) {
            return instance.getFileName().toString();
        }
    }

    private static List<Path> children(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory)) {
            stream.forEach(children::add);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not list " + dir, e);
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        return children;
    }

    /** Jars in the folder's mods/, so the player can tell an empty instance from one in use. */
    public static int jarCount(Path gameDir) {
        Path mods = gameDir.resolve("mods");
        if (!Files.isDirectory(mods)) return 0;
        try (Stream<Path> files = Files.list(mods)) {
            return (int) files.filter(file -> file.getFileName().toString().endsWith(".jar")).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
