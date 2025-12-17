package ui;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the state of the update process including counters, error tracking,
 * and thread management.
 */
public class UpdateStateManager {
    private Thread updateThread;
    private volatile boolean hasErrors = false;
    private final AtomicInteger completedMods = new AtomicInteger(0);
    private final AtomicInteger failedMods = new AtomicInteger(0);

    /**
     * Resets all counters and error state to initial values.
     */
    public void reset() {
        hasErrors = false;
        completedMods.set(0);
        failedMods.set(0);
    }

    /**
     * Increments the count of completed mods by 1.
     */
    public void incrementCompleted() {
        completedMods.incrementAndGet();
    }

    /**
     * Increments the count of failed mods by 1.
     */
    public void incrementFailed() {
        failedMods.incrementAndGet();
    }

    /**
     * Sets the error state.
     *
     * @param hasErrors true if there are errors, false otherwise
     */
    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    /**
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * @return the number of completed mods
     */
    public int getCompletedCount() {
        return completedMods.get();
    }

    /**
     * @return the number of failed mods
     */
    public int getFailedCount() {
        return failedMods.get();
    }

    /**
     * Sets the update thread.
     *
     * @param thread the thread performing the update
     */
    public void setUpdateThread(Thread thread) {
        this.updateThread = thread;
    }

    /**
     * @return the current update thread
     */
    public Thread getUpdateThread() {
        return updateThread;
    }

    /**
     * Checks if the update thread is currently running.
     *
     * @return true if the thread is alive, false otherwise
     */
    public boolean isUpdateRunning() {
        return updateThread != null && updateThread.isAlive();
    }

    /**
     * Interrupts the update thread if it's running.
     */
    public void cancelUpdate() {
        if (isUpdateRunning()) {
            updateThread.interrupt();
        }
    }
}
