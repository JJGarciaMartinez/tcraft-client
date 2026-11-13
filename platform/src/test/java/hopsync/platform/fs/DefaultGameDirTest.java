package hopsync.platform.fs;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultGameDirTest {

    @Test
    void defaultGameDirPerOs() {
        Path cwd = Path.of("/somewhere");
        assertEquals(Path.of("/home/p/.minecraft"), DefaultGameDir.forSystem("Linux", "/home/p", cwd));
        assertEquals(Path.of("/Users/p/Library/Application Support/minecraft"),
            DefaultGameDir.forSystem("Mac OS X", "/Users/p", cwd));
        assertEquals(Path.of("/u/p/AppData/Roaming/.minecraft"), DefaultGameDir.forSystem("Windows 11", "/u/p", cwd));
    }

    @Test
    void launchedFromModsFolderUsesItsParent() {
        assertEquals(Path.of("/games/instance"),
            DefaultGameDir.forSystem("Linux", "/home/p", Path.of("/games/instance/mods")));
    }
}
