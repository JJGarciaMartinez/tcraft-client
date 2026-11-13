package hopsync.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple event bus for decoupling components.
 * Thread-safe for concurrent publishing and subscribing.
 */
public class EventBus {
    private static final System.Logger LOG = System.getLogger(EventBus.class.getName());

    private final List<Consumer<SyncEvent>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<SyncEvent> listener) {
        listeners.add(listener);
    }

    public void publish(SyncEvent event) {
        for (Consumer<SyncEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.ERROR, "Error in event listener for " + event, e);
            }
        }
    }
}