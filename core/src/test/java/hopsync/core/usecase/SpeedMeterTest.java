package hopsync.core.usecase;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeedMeterTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void averagesTheLastThreeSeconds() {
        AtomicLong now = new AtomicLong();
        SpeedMeter meter = new SpeedMeter(now::get);

        assertEquals(0, meter.add(0), "no time has passed yet");
        now.set(SECOND);
        assertEquals(1000, meter.add(1000));
        now.set(2 * SECOND);
        assertEquals(1500, meter.add(3000), "3000 bytes in 2 s");
        // A burst followed by a stall: the average keeps the burst for the whole window, then forgets it
        now.set(5 * SECOND);
        assertEquals(333, meter.add(4000), "1000 more bytes over the last 3 s window");
        now.set(9 * SECOND);
        assertEquals(0, meter.add(4000), "nothing downloaded in the last window");
    }

    @Test
    void neverNegativeWhenADownloadRestarts() {
        AtomicLong now = new AtomicLong();
        SpeedMeter meter = new SpeedMeter(now::get);
        meter.add(5000);
        now.set(SECOND);

        assertEquals(0, meter.add(2000));
    }
}
