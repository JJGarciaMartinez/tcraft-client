package hopsync.core.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellationTokenTest {

    @Test
    void callbacksRunOnceOnCancel() {
        CancellationToken token = new CancellationToken();
        AtomicInteger calls = new AtomicInteger();
        token.onCancel(calls::incrementAndGet);

        assertFalse(token.isCancelled());
        assertDoesNotThrow(token::throwIfCancelled);
        token.cancel();
        token.cancel();

        assertTrue(token.isCancelled());
        assertEquals(1, calls.get());
        assertThrows(CancellationException.class, token::throwIfCancelled);
    }

    @Test
    void registeringAfterCancelRunsImmediately() {
        CancellationToken token = new CancellationToken();
        token.cancel();
        AtomicInteger calls = new AtomicInteger();

        token.onCancel(calls::incrementAndGet);

        assertEquals(1, calls.get());
    }

    @Test
    void closedRegistrationsAreSkipped() {
        CancellationToken token = new CancellationToken();
        AtomicInteger calls = new AtomicInteger();

        token.onCancel(calls::incrementAndGet).close();
        token.cancel();

        assertEquals(0, calls.get());
    }

    @Test
    void aFailingCallbackDoesNotStopTheOthers() {
        CancellationToken token = new CancellationToken();
        AtomicInteger calls = new AtomicInteger();
        token.onCancel(() -> { throw new IllegalStateException("boom"); });
        token.onCancel(calls::incrementAndGet);

        token.cancel();

        assertEquals(1, calls.get());
    }
}
