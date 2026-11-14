package hopsync.desktop.kit;

import com.formdev.flatlaf.util.UIScale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HopSyncDarkTest {

    @BeforeAll
    static void installTheme() {
        HopSyncDark.setup();
    }

    @Test
    void installsTheThemeWithItsPalette() {
        assertEquals(HopSyncDark.NAME, UIManager.getLookAndFeel().getName());
        assertEquals(new Color(0x141317), Palette.background());
        assertEquals(new Color(0x0D0C10), UIManager.getColor("TextField.background"));
        assertEquals(new Color(0xE9E6EF), Palette.brand());
        assertEquals(new Color(0x2EC4B6), Palette.mine());
        assertEquals(new Color(0xFF6B4A), Palette.action());
        assertEquals(Palette.action(), Palette.added());
        assertEquals(new Color(0xF2B24A), Palette.update());
        assertEquals(new Color(0xF2668B), Palette.error());
        assertEquals(Palette.mine(), UIManager.getColor("HopSync.focus"));
        assertEquals(Palette.mine(), UIManager.getColor("ProgressBar.foreground"));
    }

    @Test
    void usesFigtree() {
        Font font = UIManager.getFont("defaultFont");
        assertEquals(Fonts.UI_FAMILY, font.getFamily());
        assertEquals(Fonts.UI_SEMIBOLD_FAMILY, UIManager.getFont("semibold.font").getFamily());
        assertEquals(Fonts.MONO_FAMILY, UIManager.getFont("monospaced.font").getFamily());
    }

    @Test
    void buttonsAreRaisedAndFieldsSunken() {
        assertInstanceOf(RaisedButtonUI.class, new JButton("Aplicar").getUI());
        assertInstanceOf(SunkenBorder.class, new JTextField().getBorder());
    }

    @Test
    void raisedButtonsReserveRoomForTheBevel() {
        JButton raised = new JButton("Aplicar");
        JButton quiet = new JButton("Aplicar");
        ButtonStyle.apply(quiet, ButtonStyle.QUIET);
        assertTrue(RaisedButtonUI.isRaised(raised));
        assertFalse(RaisedButtonUI.isRaised(quiet));
        assertEquals(quiet.getPreferredSize().height + UIScale.scale(UIManager.getInt("HopSync.bevelWidth")),
            raised.getPreferredSize().height);
    }

    @Test
    void styleClassesCombine() {
        JButton button = new JButton("Quitar");
        ButtonStyle.apply(button, ButtonStyle.DANGER, ButtonStyle.SMALL);
        assertTrue(ButtonStyle.DANGER.isAppliedTo(button));
        assertTrue(ButtonStyle.SMALL.isAppliedTo(button));
        assertFalse(ButtonStyle.PRIMARY.isAppliedTo(button));
    }

    @Test
    void primaryAndDisabledButtonsLookDifferent() {
        JButton primary = new JButton("Aplicar");
        ButtonStyle.apply(primary, ButtonStyle.PRIMARY);
        JButton disabled = new JButton("Aplicar");
        ButtonStyle.apply(disabled, ButtonStyle.PRIMARY);
        disabled.setEnabled(false);
        assertNotEquals(centerColor(primary), centerColor(disabled));
    }

    @ParameterizedTest
    @EnumSource(Icons.Glyph.class)
    void everyIconDrawsSomething(Icons.Glyph glyph) {
        Icon icon = Icons.get(glyph, 16, Color.WHITE);
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        boolean painted = false;
        for (int x = 0; x < image.getWidth() && !painted; x++) {
            for (int y = 0; y < image.getHeight() && !painted; y++) {
                painted = (image.getRGB(x, y) >>> 24) != 0;
            }
        }
        assertTrue(painted, glyph.name());
    }

    private static int centerColor(JComponent c) {
        c.setSize(c.getPreferredSize());
        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        c.paint(g);
        g.dispose();
        return image.getRGB(c.getWidth() / 2, UIScale.scale(4 + UIManager.getInt("Component.focusWidth")));
    }
}
