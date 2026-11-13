package hopsync.core.manifest;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.manifest.ModSource;
import hopsync.core.util.JsonParseException;
import hopsync.core.failure.SyncFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestParserTest {
    private static final String SHA512 = "a".repeat(128);
    private static final String SHA256 = "b".repeat(64);

    private static String mod(String file, String url, String sha, String source) {
        return """
            {"id": "m", "name": "Mod", "version": "1.0", "author": "me", "description": "d",
             "file": "%s", "url": "%s", "sha512": "%s", "size": 10, "source": "%s"}
            """.formatted(file, url, sha, source);
    }

    private static String config(String path, String url, String overwrite) {
        return """
            {"path": "%s", "url": "%s", "sha512": "%s", "size": 5, "overwrite": %s}
            """.formatted(path, url, SHA512, overwrite);
    }

    private static String manifest(String format, String mods, String configs) {
        return """
            {"format_version": %s,
             "pack": {"name": "Pack", "version": "1.0.0", "minecraft": "1.21.1", "loader": "neoforge"},
             "content_hash": "%s", "mods": [%s], "configs": [%s]}
            """.formatted(format, SHA256, mods, configs);
    }

    private static String validMod() {
        return mod("mod.jar", "https://cdn.modrinth.com/data/x/mod.jar", SHA512, "modrinth");
    }

    private static String validConfig() {
        return config("sub/a.toml", "https://github.com/o/r/releases/download/hopsync/a.toml", "false");
    }

    private static ManifestException assertRejected(String json) {
        return assertThrows(ManifestException.class, () -> ManifestParser.parse(json));
    }

    @Test
    void parsesPublishedDemoManifest() throws IOException {
        String json;
        try (InputStream in = getClass().getResourceAsStream("/manifest/hopsync-demo.json")) {
            assertNotNull(in);
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Manifest manifest = ManifestParser.parse(json);

        assertEquals(2, manifest.formatVersion());
        assertEquals("HopSync Demo", manifest.pack().name());
        assertEquals("neoforge", manifest.pack().loader());
        assertEquals(77, manifest.mods().size());
        assertEquals(1, manifest.configs().size());
        assertTrue(manifest.mods().stream().anyMatch(m -> m.source() == ModSource.LOCAL));
        assertTrue(manifest.mods().stream().anyMatch(m -> m.source() == ModSource.MODRINTH));
        assertFalse(manifest.configs().get(0).overwrite());
    }

    @Test
    void parsesAllFields() throws IOException {
        Manifest manifest = ManifestParser.parse(manifest("2", validMod(), validConfig()));

        ModEntry mod = manifest.mods().get(0);
        assertEquals("m", mod.id());
        assertEquals("Mod", mod.name());
        assertEquals("mod.jar", mod.file());
        assertEquals(SHA512, mod.sha512());
        assertEquals(10, mod.size());
        assertEquals(ModSource.MODRINTH, mod.source());

        ConfigEntry config = manifest.configs().get(0);
        assertEquals("sub/a.toml", config.path());
        assertEquals(5, config.size());
        assertFalse(config.overwrite());

        assertNull(manifest.pack().serverIp());
        assertEquals("", manifest.pack().description());
        assertEquals(SHA256, manifest.contentHash());
    }

    @Test
    void ignoresUnknownFields() throws IOException {
        String json = manifest("2", validMod(), "").replace("\"mods\":", "\"future_field\": {\"x\": 1}, \"mods\":");
        assertEquals(1, ManifestParser.parse(json).mods().size());
    }

    @Test
    void rejectsUnsupportedFormatVersion() {
        ManifestException e = assertRejected(manifest("3", validMod(), ""));
        assertEquals(new SyncFailure.UnsupportedFormat(3, Manifest.SUPPORTED_FORMAT_VERSION), e.failure());
    }

    @Test
    void rejectsLegacyTcraftManifest() {
        String legacy = """
            {"mod_pack_name": "TCraft", "mods": [{"name": "a.jar", "url": "https://x/a.jar"}]}
            """;
        ManifestException e = assertRejected(legacy);
        assertTrue(e.getMessage().contains("format_version"));
    }

    @Test
    void rejectsInvalidJson() {
        ManifestException e = assertThrows(ManifestException.class, () -> ManifestParser.parse("{\"format_version\": 2,"));
        assertInstanceOf(SyncFailure.ManifestInvalid.class, e.failure());
        assertInstanceOf(JsonParseException.class, e.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../escape.jar", "sub/mod.jar", "..\\\\evil.jar", "C:evil.jar", "mod.zip", ".."})
    void rejectsUnsafeModFileNames(String file) {
        assertRejected(manifest("2", mod(file, "https://x.org/a.jar", SHA512, "modrinth"), ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../.bashrc", "/etc/passwd", "a/../../b.toml", "C:/x.toml", ""})
    void rejectsUnsafeConfigPaths(String path) {
        assertRejected(manifest("2", "", config(path, "https://x.org/a", "true")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://insecure.org/a.jar", "file:///etc/passwd", "ftp://x.org/a.jar", "not a url", "https:///nohost"})
    void rejectsNonHttpsUrls(String url) {
        assertRejected(manifest("2", mod("mod.jar", url, SHA512, "modrinth"), ""));
    }

    @Test
    void rejectsBadHash() {
        assertRejected(manifest("2", mod("mod.jar", "https://x.org/a.jar", "ABC", "modrinth"), ""));
        assertRejected(manifest("2", mod("mod.jar", "https://x.org/a.jar", "A".repeat(128), "modrinth"), ""));
    }

    @Test
    void rejectsDuplicateFilesIgnoringCase() {
        String a = mod("Mod.jar", "https://x.org/a.jar", SHA512, "modrinth");
        String b = mod("mod.jar", "https://x.org/b.jar", SHA512, "local");
        ManifestException e = assertRejected(manifest("2", a + "," + b, ""));
        assertTrue(detail(e).contains("duplicated"), detail(e));
    }

    @Test
    void rejectsUnknownSource() {
        assertRejected(manifest("2", mod("mod.jar", "https://x.org/a.jar", SHA512, "curseforge"), ""));
    }

    @Test
    void rejectsNonBooleanOverwrite() {
        assertRejected(manifest("2", "", config("a.toml", "https://x.org/a", "\"yes\"")));
    }

    @Test
    void rejectsMissingPackFields() {
        assertRejected(manifest("2", "", "").replace("\"loader\": \"neoforge\"", "\"loader\": \"\""));
    }

    @Test
    void namesTheOffendingField() {
        String bad = mod("mod.jar", "https://x.org/a.jar", SHA512, "modrinth").replace("\"size\": 10", "\"size\": -1");
        ManifestException e = assertRejected(manifest("2", validMod().replace("mod.jar", "ok.jar") + "," + bad, ""));
        assertTrue(detail(e).startsWith("mods[1].size"), detail(e));
    }

    private static String detail(ManifestException e) {
        return assertInstanceOf(SyncFailure.ManifestInvalid.class, e.failure()).detail();
    }
}
