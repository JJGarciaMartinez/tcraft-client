package hopsync.desktop.screens.mods;

import hopsync.core.manifest.PackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Short form of a mod version for the list: drops the pack's Minecraft version and loader and tags such as
 * {@code +mod}, so {@code mc1.21.1-6.0.8} reads {@code 6.0.8} in a 1.21.1 pack. What to drop comes from the
 * pack's manifest, not from a list of known versions. The full string stays in the expanded row.
 */
public final class VersionLabel {
    /** Loader names and tags that never belong to a mod's own version, whatever the pack uses. */
    private static final Set<String> ALWAYS_NOISE = Set.of("forge", "neoforge", "fabric", "neo", "quilt", "mod", "full");
    private static final Pattern PART = Pattern.compile("([-+_ ]*)([^-+_ ]+)");

    private VersionLabel() {
    }

    /**
     * @param pack the pack the mod belongs to, or {@code null} to drop only loader names and tags
     */
    public static String clean(String version, PackInfo pack) {
        if (version == null || version.isBlank()) return "";
        Pattern minecraft = minecraftOf(pack);
        Set<String> noise = new HashSet<>(ALWAYS_NOISE);
        if (pack != null && pack.loader() != null) {
            noise.add(pack.loader().toLowerCase(Locale.ROOT));
        }
        List<String> kept = new ArrayList<>();
        Matcher part = PART.matcher(version.trim());
        while (part.find()) {
            String token = part.group(2);
            if (noise.contains(token.toLowerCase(Locale.ROOT))
                    || minecraft != null && minecraft.matcher(token).matches()) {
                continue;
            }
            // The separator in front of the first kept part is dropped
            kept.add((kept.isEmpty() ? "" : separator(part.group(1))) + stripV(token));
        }
        return kept.isEmpty() ? version.trim() : String.join("", kept);
    }

    /**
     * The pack's Minecraft release line with or without "mc" and any patch: for 1.21.1 it matches 1.21,
     * 1.21.0, 1.21.1 and mc1.21.1; for 26.1 it matches 26.1 and 26.1.2.
     */
    private static Pattern minecraftOf(PackInfo pack) {
        if (pack == null || pack.minecraft() == null || pack.minecraft().isBlank()) return null;
        String[] parts = pack.minecraft().trim().split("\\.");
        String line = parts.length >= 2 ? parts[0] + "." + parts[1] : parts[0];
        return Pattern.compile("(?i)(mc)?" + Pattern.quote(line) + "(\\.\\d+)*");
    }

    private static String separator(String run) {
        return run.isEmpty() ? "-" : run.substring(run.length() - 1);
    }

    /** "v4.4.4" → "4.4.4", but not words that start with v. */
    private static String stripV(String token) {
        return token.length() > 1 && (token.charAt(0) == 'v' || token.charAt(0) == 'V') && Character.isDigit(token.charAt(1))
            ? token.substring(1) : token;
    }
}
