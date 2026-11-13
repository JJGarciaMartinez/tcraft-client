package hopsync.desktop.store;

import javax.swing.SwingUtilities;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Holds the UI state and applies actions to it on the EDT, one at a time. Subscribers render the new
 * state; effects see each action with the state it produced and may dispatch more actions, which run
 * after the current one has been fully handled.
 */
public final class Store {
    private static final System.Logger LOG = System.getLogger(Store.class.getName());

    private final List<Consumer<UiState>> subscribers = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<UiAction, UiState>> effects = new CopyOnWriteArrayList<>();
    private final Queue<UiAction> pending = new ArrayDeque<>();
    private UiState state;
    private boolean dispatching;

    public Store(UiState initial) {
        this.state = initial;
    }

    /** Current state; read it on the EDT. */
    public UiState state() {
        return state;
    }

    /** Called with the new state after every action, on the EDT. */
    public void subscribe(Consumer<UiState> subscriber) {
        subscribers.add(subscriber);
    }

    /** Called with every action and the state it produced, after the subscribers, on the EDT. */
    public void addEffect(BiConsumer<UiAction, UiState> effect) {
        effects.add(effect);
    }

    /** Safe from any thread: actions from other threads are queued onto the EDT in order. */
    public void dispatch(UiAction action) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> dispatch(action));
            return;
        }
        pending.add(action);
        if (dispatching) {
            return;
        }
        dispatching = true;
        try {
            UiAction next;
            while ((next = pending.poll()) != null) {
                handle(next);
            }
        } finally {
            dispatching = false;
        }
    }

    private void handle(UiAction action) {
        state = Reducer.reduce(state, action);
        for (Consumer<UiState> subscriber : subscribers) {
            try {
                subscriber.accept(state);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Rendering failed after " + action, e);
            }
        }
        for (BiConsumer<UiAction, UiState> effect : effects) {
            try {
                effect.accept(action, state);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Effect failed for " + action, e);
            }
        }
    }
}
