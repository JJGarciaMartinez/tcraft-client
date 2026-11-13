package hopsync.core.manifest;

import hopsync.core.failure.SyncFailure;
import hopsync.core.util.JsonParseException;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.manifest.ModSource;
import hopsync.core.manifest.PackInfo;
import hopsync.core.util.Json;
import hopsync.core.util.SafePaths;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses and validates a manifest. Anything that could make the client write outside the
 * game folders or download from an unexpected place is rejected. Unknown fields are ignored
 * so newer manifests stay readable as long as the format version matches.
 */
public final class ManifestParser {
    private static final Pattern SHA512 = Pattern.compile("^[0-9a-f]{128}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private ManifestParser() {
    }

    public static Manifest parse(String json) throws IOException {
        Object document;
        try {
            document = Json.parse(json);
        } catch (JsonParseException e) {
            throw new ManifestException(e.getMessage(), e);
        }
        Map<String, Object> root = asObject(document, "manifest");

        long format = requireLong(root, "format_version", "manifest");
        if (format != Manifest.SUPPORTED_FORMAT_VERSION) {
            throw new ManifestException(new SyncFailure.UnsupportedFormat(format, Manifest.SUPPORTED_FORMAT_VERSION));
        }

        PackInfo pack = parsePack(asObject(require(root, "pack", "manifest"), "pack"));
        String contentHash = requireMatching(root, "content_hash", "manifest", SHA256);
        List<ModEntry> mods = parseMods(asArray(require(root, "mods", "manifest"), "mods"));
        List<ConfigEntry> configs = parseConfigs(asArray(require(root, "configs", "manifest"), "configs"));

        return new Manifest((int) format, pack, contentHash, mods, configs);
    }

    private static PackInfo parsePack(Map<String, Object> pack) throws ManifestException {
        return new PackInfo(
            requireText(pack, "name", "pack"),
            requireText(pack, "version", "pack"),
            optionalString(pack, "description", "pack", ""),
            optionalString(pack, "server_ip", "pack", null),
            requireText(pack, "minecraft", "pack"),
            requireText(pack, "loader", "pack")
        );
    }

    private static List<ModEntry> parseMods(List<Object> array) throws ManifestException {
        List<ModEntry> mods = new ArrayList<>();
        Set<String> files = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            String where = "mods[" + i + "]";
            Map<String, Object> mod = asObject(array.get(i), where);

            String file = requireText(mod, "file", where);
            if (!SafePaths.isSafeFileName(file) || !file.endsWith(".jar")) {
                throw new ManifestException(where + ".file is not a valid .jar file name: " + file);
            }
            if (!files.add(file.toLowerCase(Locale.ROOT))) {
                throw new ManifestException(where + ".file is duplicated: " + file);
            }

            mods.add(new ModEntry(
                requireText(mod, "id", where),
                optionalString(mod, "name", where, file),
                optionalString(mod, "version", where, ""),
                optionalString(mod, "author", where, ""),
                optionalString(mod, "description", where, ""),
                file,
                requireHttpsUrl(mod, where),
                requireMatching(mod, "sha512", where, SHA512),
                requireSize(mod, where),
                parseSource(requireText(mod, "source", where), where)
            ));
        }
        return mods;
    }

    private static List<ConfigEntry> parseConfigs(List<Object> array) throws ManifestException {
        List<ConfigEntry> configs = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            String where = "configs[" + i + "]";
            Map<String, Object> config = asObject(array.get(i), where);

            String path = requireText(config, "path", where);
            if (!SafePaths.isSafeRelativePath(path)) {
                throw new ManifestException(where + ".path is not a valid relative path: " + path);
            }
            if (!paths.add(path.toLowerCase(Locale.ROOT))) {
                throw new ManifestException(where + ".path is duplicated: " + path);
            }

            Object overwrite = require(config, "overwrite", where);
            if (!(overwrite instanceof Boolean flag)) {
                throw new ManifestException(where + ".overwrite must be true or false");
            }

            configs.add(new ConfigEntry(
                path,
                requireHttpsUrl(config, where),
                requireMatching(config, "sha512", where, SHA512),
                requireSize(config, where),
                flag
            ));
        }
        return configs;
    }

    private static ModSource parseSource(String source, String where) throws ManifestException {
        return switch (source) {
            case "modrinth" -> ModSource.MODRINTH;
            case "local" -> ModSource.LOCAL;
            default -> throw new ManifestException(where + ".source is unknown: " + source);
        };
    }

    // ------------------------------------------------------------------ field helpers

    private static Object require(Map<String, Object> obj, String key, String where) throws ManifestException {
        if (!obj.containsKey(key) || obj.get(key) == null) {
            throw new ManifestException("Missing field " + where + "." + key);
        }
        return obj.get(key);
    }

    private static String requireText(Map<String, Object> obj, String key, String where) throws ManifestException {
        Object value = require(obj, key, where);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new ManifestException(where + "." + key + " must be a non-empty string");
        }
        return s;
    }

    private static String optionalString(Map<String, Object> obj, String key, String where, String fallback)
            throws ManifestException {
        Object value = obj.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof String s)) {
            throw new ManifestException(where + "." + key + " must be a string");
        }
        return s;
    }

    private static String requireMatching(Map<String, Object> obj, String key, String where, Pattern pattern)
            throws ManifestException {
        String value = requireText(obj, key, where);
        if (!pattern.matcher(value).matches()) {
            throw new ManifestException(where + "." + key + " has an invalid format");
        }
        return value;
    }

    private static long requireLong(Map<String, Object> obj, String key, String where) throws ManifestException {
        if (!(require(obj, key, where) instanceof Long n)) {
            throw new ManifestException(where + "." + key + " must be an integer");
        }
        return n;
    }

    private static long requireSize(Map<String, Object> obj, String where) throws ManifestException {
        long size = requireLong(obj, "size", where);
        if (size < 0) {
            throw new ManifestException(where + ".size cannot be negative");
        }
        return size;
    }

    private static String requireHttpsUrl(Map<String, Object> obj, String where) throws ManifestException {
        String url = requireText(obj, "url", where);
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new ManifestException(where + ".url must be an https URL: " + url);
            }
        } catch (URISyntaxException e) {
            throw new ManifestException(where + ".url is not a valid URL: " + url);
        }
        return url;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value, String where) throws ManifestException {
        if (!(value instanceof Map<?, ?>)) {
            throw new ManifestException(where + " must be a JSON object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asArray(Object value, String where) throws ManifestException {
        if (!(value instanceof List<?>)) {
            throw new ManifestException(where + " must be a JSON array");
        }
        return (List<Object>) value;
    }
}
