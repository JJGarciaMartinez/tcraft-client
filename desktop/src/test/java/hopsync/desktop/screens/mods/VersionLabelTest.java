package hopsync.desktop.screens.mods;

import hopsync.core.manifest.PackInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionLabelTest {
    private static final PackInfo NEOFORGE_1_21_1 = pack("1.21.1", "neoforge");

    private static PackInfo pack(String minecraft, String loader) {
        return new PackInfo("Pack", "1.0.0", "", null, minecraft, loader);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "mc1.21.1-6.0.8 | 6.0.8",
        "1.21-2.0.13-neoforge | 2.0.13",
        "13.0.8+neoforge | 13.0.8",
        "1.21.1-3.1-fabric+forge+neo | 3.1",
        "2.0.0-beta.12+1.21.1 | 2.0.0-beta.12",
        "1.1.10-mc1.21.1 | 1.1.10",
        "neoforge-1.21.1-1.5.9 | 1.5.9",
        "v4.4.4+mod | 4.4.4",
        "0.115.6+2.1.4+1.21.1 | 0.115.6+2.1.4",
        "neoforge-4.0.18+mc1.21.1 | 4.0.18",
        "1.21.0-2.1 | 2.1",
        "1.1.0-1.21-1.21.1 | 1.1.0",
        "1.39.12_NeoForge_1.21 | 1.39.12",
        "1.21.1-NeoForge-5.1.6 | 5.1.6",
        "19.25.1.332 | 19.25.1.332",
        "1.5.0 | 1.5.0",
        "3.0.6.1+mod | 3.0.6.1",
    })
    void dropsThePacksMinecraftAndLoader(String version, String expected) {
        assertEquals(expected, VersionLabel.clean(version, NEOFORGE_1_21_1));
    }

    @Test
    void followsWhateverMinecraftThePackUses() {
        assertEquals("6.0.8", VersionLabel.clean("mc26.1-6.0.8", pack("26.1", "neoforge")));
        assertEquals("2.3", VersionLabel.clean("26.1.2-2.3", pack("26.1.2", "neoforge")));
        assertEquals("2.3", VersionLabel.clean("1.20.1-2.3-forge", pack("1.20.1", "forge")));
        // Another release line is part of the mod's own version, not the pack's Minecraft
        assertEquals("1.20.1-2.3", VersionLabel.clean("1.20.1-2.3", NEOFORGE_1_21_1));
    }

    @Test
    void usesThePacksLoaderName() {
        assertEquals("1.4.2", VersionLabel.clean("1.4.2+liteloader", pack("1.21.1", "liteloader")));
    }

    @Test
    void withoutPackOnlyLoaderNamesAndTagsGo() {
        assertEquals("1.21.1-6.0.8", VersionLabel.clean("1.21.1-6.0.8-neoforge", null));
    }

    @Test
    void emptyStaysEmptyAndOnlyNoiseIsKeptAsIs() {
        assertEquals("", VersionLabel.clean(null, NEOFORGE_1_21_1));
        assertEquals("", VersionLabel.clean(" ", NEOFORGE_1_21_1));
        assertEquals("1.21.1", VersionLabel.clean("1.21.1", NEOFORGE_1_21_1));
    }

    /** Real versions, copied from the hopsync-demo manifest (a 1.21.1 NeoForge pack) as a fixed sample. */
    @Test
    void realVersionsKeepTheirOwnNumbersAndDropTheNoise() throws IOException {
        for (String version : sampleVersions()) {
            String clean = VersionLabel.clean(version, NEOFORGE_1_21_1);
            assertFalse(clean.isBlank(), version);
            assertTrue(clean.length() <= version.length(), version);
            String lower = clean.toLowerCase(Locale.ROOT);
            assertFalse(lower.contains("neoforge") || lower.contains("fabric") || lower.contains("1.21"),
                version + " -> " + clean);
            assertTrue(Character.isDigit(clean.charAt(0)), version + " -> " + clean);
        }
    }

    private static List<String> sampleVersions() throws IOException {
        try (InputStream in = VersionLabelTest.class.getResourceAsStream("demo-versions.txt")) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        }
    }
}
