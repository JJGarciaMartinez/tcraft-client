package hopsync.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafePathsTest {

    @TempDir
    Path base;

    @ParameterizedTest
    @ValueSource(strings = {"mod.jar", "Geophilic v3.4.5.mod.jar", "dungeons [NeoForge].jar", ".hidden"})
    void acceptsPlainFileNames(String name) {
        assertTrue(SafePaths.isSafeFileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", ".", "..", "a/b.jar", "a\\b.jar", "C:evil.jar", "mod.jar:stream", "a\u0000.jar", "a\n.jar"})
    void rejectsUnsafeFileNames(String name) {
        assertFalse(SafePaths.isSafeFileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.toml", "sub/dir/a.json", "mod-id/client.toml"})
    void acceptsSafeRelativePaths(String path) {
        assertTrue(SafePaths.isSafeRelativePath(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/etc/passwd", "../a", "a/../../b", "a/./b", "a//b", "a/", "..\\a", "C:/a", "\\\\server\\share"})
    void rejectsUnsafeRelativePaths(String path) {
        assertFalse(SafePaths.isSafeRelativePath(path));
    }

    @Test
    void resolvesInsideBase() throws IOException {
        assertEquals(base.resolve("sub").resolve("a.json"), SafePaths.resolveInside(base, "sub/a.json"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside.jar", "a/../../outside", "/abs"})
    void refusesToEscapeBase(String path) {
        assertThrows(IOException.class, () -> SafePaths.resolveInside(base, path));
    }
}
