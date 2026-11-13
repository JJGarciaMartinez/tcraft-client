package hopsync.desktop.store;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreTest {
    private final List<String> log = new CopyOnWriteArrayList<>();

    @Test
    void subscribersSeeTheNewStateBeforeEffectsRun() throws Exception {
        Store store = new Store(UiState.INITIAL);
        store.subscribe(state -> log.add("render " + state.selectedPackId()));
        store.addEffect((action, state) -> log.add("effect " + action.getClass().getSimpleName()));

        SwingUtilities.invokeAndWait(() ->
            store.dispatch(new UiAction.PacksLoaded(List.of(DEMO, OTHER), OTHER.id())));

        assertEquals(List.of("render other", "effect PacksLoaded"), log);
    }

    @Test
    void actionsDispatchedByAnEffectRunAfterTheCurrentOne() throws Exception {
        Store store = new Store(StoreFixtures.loaded());
        store.subscribe(state -> log.add("render " + state.selectedPackId()));
        store.addEffect((action, state) -> {
            log.add("effect " + action.getClass().getSimpleName());
            if (action instanceof UiAction.SelectPack select && select.packId().equals(OTHER.id())) {
                store.dispatch(new UiAction.SelectPack(DEMO.id()));
                log.add("effect returned");
            }
        });

        SwingUtilities.invokeAndWait(() -> store.dispatch(new UiAction.SelectPack(OTHER.id())));

        assertEquals(List.of("render other", "effect SelectPack", "effect returned",
            "render demo", "effect SelectPack"), log);
    }

    @Test
    void actionsFromOtherThreadsAreHandledOnTheEdt() throws Exception {
        Store store = new Store(UiState.INITIAL);
        CountDownLatch rendered = new CountDownLatch(1);
        store.subscribe(state -> {
            log.add(SwingUtilities.isEventDispatchThread() ? "edt" : "other");
            rendered.countDown();
        });

        Thread.ofVirtual().start(() -> store.dispatch(new UiAction.PacksLoaded(List.of(DEMO), DEMO.id()))).join();

        assertTrue(rendered.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("edt"), log);
        SwingUtilities.invokeAndWait(() -> assertEquals(DEMO.id(), store.state().selectedPackId()));
    }

    @Test
    void aFailingSubscriberDoesNotStopTheOthers() throws Exception {
        Store store = new Store(UiState.INITIAL);
        store.subscribe(state -> {
            throw new IllegalStateException("broken view");
        });
        store.subscribe(state -> log.add("render"));
        store.addEffect((action, state) -> log.add("effect"));

        SwingUtilities.invokeAndWait(() -> store.dispatch(new UiAction.OpenSettings()));

        assertEquals(List.of("render", "effect"), log);
    }
}
