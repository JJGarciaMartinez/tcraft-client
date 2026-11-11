package hopsync.sync;

import hopsync.model.ConfigEntry;
import hopsync.model.Manifest;
import hopsync.model.ModEntry;
import hopsync.model.ModSource;
import hopsync.model.PackInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class Fixtures {
    static final String CONTENT_HASH = "c".repeat(64);

    private Fixtures() {
    }

    static String sha512(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static ModEntry mod(String file, String content) {
        return new ModEntry(file, file, "1.0", "", "", file, "https://example.org/" + file, sha512(content),
            content.getBytes(StandardCharsets.UTF_8).length, ModSource.MODRINTH);
    }

    static ConfigEntry config(String path, String content, boolean overwrite) {
        return new ConfigEntry(path, "https://example.org/config/" + path, sha512(content),
            content.getBytes(StandardCharsets.UTF_8).length, overwrite);
    }

    static Manifest manifest(List<ModEntry> mods, List<ConfigEntry> configs) {
        return new Manifest(2, new PackInfo("Pack", "1.0.0", "", null, "1.21.1", "neoforge"),
            CONTENT_HASH, mods, configs);
    }

    static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /** Record of a file as HopSync would have saved it right after installing it. */
    static FileRecord recordOf(Path file) throws IOException {
        return FileHasher.describe(file, null);
    }
}
