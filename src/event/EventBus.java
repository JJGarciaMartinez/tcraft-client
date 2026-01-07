package event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple event bus for decoupling components.
 * Thread-safe for concurrent publishing and subscribing.
 */
public class EventBus {
    private final List<Consumer<UpdateEvent>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<UpdateEvent> listener) {
        listeners.add(listener);
    }

    public void publish(UpdateEvent event) {
        for (Consumer<UpdateEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("Error in event listener: " + e.getMessage());
            }
        }
    }
}