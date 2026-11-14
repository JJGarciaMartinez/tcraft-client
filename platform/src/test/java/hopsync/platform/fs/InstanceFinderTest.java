package hopsync.platform.fs;

import hopsync.platform.fs.InstanceFinder.Instance;
import hopsync.platform.fs.InstanceFinder.Launcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceFinderTest {

    @TempDir
    Path home;

    private static Path dir(Path path) throws IOException {
        return Files.createDirectories(path);
    }

    private static void jars(Path gameDir, int count) throws IOException {
        Path mods = dir(gameDir.resolve("mods"));
        for (int i = 0; i < count; i++) Files.writeString(mods.resolve("m" + i + ".jar"), "x");
        Files.writeString(mods.resolve("notes.txt"), "not a jar");
    }

    @Test
    void linuxFindsEveryLauncher() throws IOException {
        jars(dir(home.resolve(".minecraft")), 3);
        Path prism = dir(home.resolve(".local/share/PrismLauncher/instances/valle"));
        Files.writeString(prism.resolve("instance.cfg"), "InstanceType=OneSix\nname=Valle Creativo\n");
        jars(dir(prism.resolve("minecraft")), 2);
        Path oldPrism = dir(home.resolve(".local/share/PrismLauncher/instances/old/.minecraft"));
        dir(home.resolve(".local/share/PrismLauncher/instances/broken"));
        dir(home.resolve("curseforge/minecraft/Instances/All the Mods"));
        dir(home.resolve(".local/share/ModrinthApp/profiles/Fabulously"));
        dir(home.resolve(".local/share/com.modrinth.theseus/profiles/Legacy"));

        List<Instance> found = new InstanceFinder("Linux", Map.of(), home).find();

        assertEquals(List.of(
            new Instance(Launcher.OFFICIAL, null, home.resolve(".minecraft"), 3),
            new Instance(Launcher.PRISM, "old", oldPrism, 0),
            new Instance(Launcher.PRISM, "Valle Creativo", prism.resolve("minecraft"), 2),
            new Instance(Launcher.CURSEFORGE, "All the Mods", home.resolve("curseforge/minecraft/Instances/All the Mods"), 0),
            new Instance(Launcher.MODRINTH, "Fabulously", home.resolve(".local/share/ModrinthApp/profiles/Fabulously"), 0),
            new Instance(Launcher.MODRINTH, "Legacy", home.resolve(".local/share/com.modrinth.theseus/profiles/Legacy"), 0)),
            found, "a Prism folder without minecraft/ or .minecraft/ is not an instance");
    }

    @Test
    void linuxFollowsXdgDataHome() throws IOException {
        Path data = dir(home.resolve("data"));
        dir(data.resolve("PrismLauncher/instances/a/minecraft"));

        List<Instance> found = new InstanceFinder("Linux", Map.of("XDG_DATA_HOME", data.toString()), home).find();

        assertEquals(1, found.size());
        assertEquals(Launcher.PRISM, found.getFirst().launcher());
    }

    @Test
    void windowsUsesAppData() throws IOException {
        Path appData = dir(home.resolve("AppData/Roaming"));
        dir(appData.resolve(".minecraft"));
        dir(appData.resolve("PrismLauncher/instances/a/minecraft"));
        dir(appData.resolve("ModrinthApp/profiles/b"));

        List<Instance> found = new InstanceFinder("Windows 11", Map.of("APPDATA", appData.toString()), home).find();

        assertEquals(List.of(Launcher.OFFICIAL, Launcher.PRISM, Launcher.MODRINTH),
            found.stream().map(Instance::launcher).toList());
    }

    @Test
    void macUsesApplicationSupport() throws IOException {
        Path support = dir(home.resolve("Library/Application Support"));
        dir(support.resolve("minecraft"));
        dir(support.resolve("PrismLauncher/instances/a/minecraft"));

        List<Instance> found = new InstanceFinder("Mac OS X", Map.of(), home).find();

        assertEquals(List.of(support.resolve("minecraft"), support.resolve("PrismLauncher/instances/a/minecraft")),
            found.stream().map(Instance::gameDir).toList());
    }

    @Test
    void nothingInstalledFindsNothing() {
        assertTrue(new InstanceFinder("Linux", Map.of(), home).find().isEmpty());
    }
}
