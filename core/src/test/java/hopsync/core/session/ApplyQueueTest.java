package hopsync.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyQueueTest {
    final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    final List<String> log = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void runsJobsOneAtATimeInOrder() throws InterruptedException {
        ApplyQueue queue = new ApplyQueue(executor, () -> true);
        FakeJob first = new FakeJob("a");
        FakeJob second = new FakeJob("b");
        FakeJob third = new FakeJob("c");

        queue.submit(first);
        first.awaitStarted();
        queue.submit(second);
        queue.submit(third);
        assertEquals(List.of("start a", "queued b", "queued c"), log);

        first.finish();
        second.awaitStarted();
        second.finish();
        third.awaitStarted();
        third.finish();
        third.awaitDone();
        assertEquals(List.of("start a", "queued b", "queued c", "end a", "start b", "end b", "start c", "end c"), log);
    }

    @Test
    void cancellingAWaitingJobRemovesIt() throws InterruptedException {
        ApplyQueue queue = new ApplyQueue(executor, () -> true);
        FakeJob first = new FakeJob("a");
        FakeJob second = new FakeJob("b");
        FakeJob third = new FakeJob("c");
        queue.submit(first);
        first.awaitStarted();
        queue.submit(second);
        queue.submit(third);

        assertTrue(queue.cancel("b"));
        first.finish();
        third.awaitStarted();
        third.finish();
        third.awaitDone();

        assertFalse(log.contains("start b"));
    }

    @Test
    void cancellingTheRunningJobContinuesWithTheNextWhenChaining() throws InterruptedException {
        ApplyQueue queue = new ApplyQueue(executor, () -> true);
        FakeJob first = new FakeJob("a");
        FakeJob second = new FakeJob("b");
        queue.submit(first);
        first.awaitStarted();
        queue.submit(second);

        assertFalse(queue.cancel("a"), "the running job is stopped by its session, not removed");
        first.finish();

        second.awaitStarted();
        second.finish();
        assertFalse(queue.isPaused());
    }

    @Test
    void cancellingTheRunningJobPausesTheQueueWithoutChaining() throws InterruptedException {
        ApplyQueue queue = new ApplyQueue(executor, () -> false);
        FakeJob first = new FakeJob("a");
        FakeJob second = new FakeJob("b");
        queue.submit(first);
        first.awaitStarted();
        queue.submit(second);

        queue.cancel("a");
        first.finish();

        // The queue decides right after the job returns, on the job's thread
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!queue.isPaused() && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(queue.isPaused());
        assertFalse(second.started.await(200, TimeUnit.MILLISECONDS));

        queue.resume();
        second.awaitStarted();
        second.finish();
    }

    @Test
    void finishingWithoutCancelDoesNotPause() throws InterruptedException {
        ApplyQueue queue = new ApplyQueue(executor, () -> false);
        FakeJob first = new FakeJob("a");
        FakeJob second = new FakeJob("b");
        queue.submit(first);
        first.awaitStarted();
        queue.submit(second);

        first.finish();
        second.awaitStarted();
        second.finish();
        assertFalse(queue.isPaused());
    }

    private final class FakeJob implements ApplyQueue.Job {
        private final String id;
        final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch done = new CountDownLatch(1);

        FakeJob(String id) {
            this.id = id;
        }

        @Override
        public String packId() {
            return id;
        }

        @Override
        public void queued() {
            log.add("queued " + id);
        }

        @Override
        public void run() {
            log.add("start " + id);
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.add("end " + id);
            done.countDown();
        }

        void finish() {
            release.countDown();
        }

        void awaitStarted() throws InterruptedException {
            assertTrue(started.await(5, TimeUnit.SECONDS), id + " never started");
        }

        void awaitDone() throws InterruptedException {
            assertTrue(done.await(5, TimeUnit.SECONDS), id + " never finished");
        }
    }
}
