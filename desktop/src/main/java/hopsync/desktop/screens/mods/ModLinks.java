package hopsync.desktop.screens.mods;

import hopsync.core.manifest.ModEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a player can read about a mod: its Modrinth project, or the release of the pack's repository that
 * hosts it.
 */
final class ModLinks {
    private static final Pattern MODRINTH_CDN = Pattern.compile("https://cdn\\.modrinth\\.com/data/([A-Za-z0-9]+)/.*");
    private static final Pattern GITHUB_RELEASE =
        Pattern.compile("(https://github\\.com/[^/]+/[^/]+)/releases/download/([^/]+)/.*");

    private ModLinks() {
    }

    /** @return the page URL, or {@code null} when the download URL has no known page */
    static String pageOf(ModEntry mod) {
        Matcher modrinth = MODRINTH_CDN.matcher(mod.url());
        if (modrinth.matches()) {
            return "https://modrinth.com/project/" + modrinth.group(1);
        }
        Matcher github = GITHUB_RELEASE.matcher(mod.url());
        if (github.matches()) {
            return github.group(1) + "/releases/tag/" + github.group(2);
        }
        return null;
    }
}
