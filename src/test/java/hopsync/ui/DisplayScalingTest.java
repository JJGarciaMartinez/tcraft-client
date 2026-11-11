package hopsync.ui;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayScalingTest {

    @Test
    void readsXftDpi() {
        String xrdb = "Xft.antialias:\t1\nXft.dpi:\t192\nXft.hinting:\t1\n";
        assertEquals(Optional.of(2), DisplayScaling.scaleFromXrdb(xrdb));
        assertEquals(Optional.of(1), DisplayScaling.scaleFromXrdb("Xft.dpi: 96\n"));
        assertEquals(Optional.of(3), DisplayScaling.scaleFromXrdb("Xft.dpi:\t288.0\n"));
    }

    @Test
    void missingXftDpiIsUnknown() {
        assertEquals(Optional.empty(), DisplayScaling.scaleFromXrdb("Xft.antialias:\t1\n"));
        assertEquals(Optional.empty(), DisplayScaling.scaleFromXrdb(""));
    }

    @Test
    void roundsFractionalScalesToIntegers() {
        assertEquals(1, DisplayScaling.scaleFromDpi(120));  // 125%
        assertEquals(2, DisplayScaling.scaleFromDpi(144));  // 150%
        assertEquals(2, DisplayScaling.scaleFromDpi(168));  // 175%
        assertEquals(1, DisplayScaling.scaleFromDpi(48));
        assertEquals(4, DisplayScaling.scaleFromDpi(960));
    }

    @Test
    void readsGnomeScalingFactor() {
        assertEquals(Optional.of(2), DisplayScaling.scaleFromGsettings("uint32 2\n"));
        assertEquals(Optional.empty(), DisplayScaling.scaleFromGsettings("uint32 0\n"));
        assertEquals(Optional.empty(), DisplayScaling.scaleFromGsettings("No such key"));
    }
}
