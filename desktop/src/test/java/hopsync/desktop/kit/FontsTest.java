package hopsync.desktop.kit;

import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontsTest {

    @Test
    void registersEveryBundledFont() {
        assertTrue(Fonts.registerBundled());
        assertEquals(Fonts.UI_FAMILY, new Font(Fonts.UI_FAMILY, Font.PLAIN, 13).getFamily());
        assertEquals(Fonts.MONO_FAMILY, new Font(Fonts.MONO_FAMILY, Font.PLAIN, 13).getFamily());
    }

    @Test
    void missingFontIsReportedWithoutFailing() {
        assertFalse(Fonts.register("assets/font/missing.ttf"));
    }

    @Test
    void mediumKeepsTheSize() {
        Fonts.registerBundled();
        Font medium = Fonts.medium(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        assertEquals(Fonts.UI_MEDIUM_FAMILY, medium.getFamily());
        assertEquals(12, medium.getSize());
    }
}
