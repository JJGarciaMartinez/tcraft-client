package hopsync.core.session;

import hopsync.core.event.SyncEvent;
import hopsync.core.pack.PackRegistry;
import hopsync.core.usecase.ModUpdateService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * One {@link PackSession} per pack, created the first time the pack is used and shared by every caller.
 */
public class PackSessions {
    private final PackRegistry registry;
    private final ModUpdateService modUpdateService;
    private final ApplyQueue applyQueue;
    private final Consumer<SyncEvent> events;
    private final Executor executor;
    private final Map<String, PackSession> sessions = new ConcurrentHashMap<>();

    public PackSessions(PackRegistry registry, ModUpdateService modUpdateService, ApplyQueue applyQueue,
                        Consumer<SyncEvent> events, Executor executor) {
        this.registry = registry;
        this.modUpdateService = modUpdateService;
        this.applyQueue = applyQueue;
        this.events = events;
        this.executor = executor;
    }

    public PackSession get(String packId) {
        return sessions.computeIfAbsent(packId,
            id -> new PackSession(id, registry, modUpdateService, applyQueue, events, executor));
    }

    /** Stops whatever the pack is doing and forgets its session, e.g. when the pack is removed. */
    public void drop(String packId) {
        PackSession session = sessions.remove(packId);
        if (session != null) {
            session.cancelUpdate();
        }
    }

    /** Stops every check and apply, e.g. when the app closes. */
    public void cancelAll() {
        sessions.values().forEach(PackSession::cancelUpdate);
    }

    public boolean anyRunning() {
        return sessions.values().stream().anyMatch(PackSession::isRunning);
    }
}
