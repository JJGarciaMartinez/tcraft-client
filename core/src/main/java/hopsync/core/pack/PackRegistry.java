package hopsync.core.pack;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.port.PackSettingsRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The packs the player keeps in sync. Changes are saved right away; removing a pack only forgets it
 * and never touches its files. Each pack needs its own game folder.
 */
public class PackRegistry {
    private static final System.Logger LOG = System.getLogger(PackRegistry.class.getName());

    private final PackSettingsRepository repository;
    private PackSettings settings;

    public PackRegistry(PackSettingsRepository repository) {
        this.repository = repository;
        this.settings = repository.load();
    }

    public synchronized PackSettings settings() {
        return settings;
    }

    public synchronized List<Pack> packs() {
        return settings.packs();
    }

    public synchronized boolean isEmpty() {
        return settings.packs().isEmpty();
    }

    public synchronized Optional<Pack> find(String id) {
        return settings.find(id);
    }

    public synchronized Optional<Pack> active() {
        return settings.active();
    }

    public synchronized int parallelDownloads() {
        return settings.parallelDownloads();
    }

    public synchronized boolean chainQueue() {
        return settings.chainQueue();
    }

    /**
     * Adds a pack and selects it when it is the first one.
     *
     * @throws SyncException with {@link SyncFailure.FolderUsedByPack} when another pack uses the folder
     */
    public synchronized Pack add(String manifestUrl, Path gameDir) throws IOException {
        Pack pack = new Pack(UUID.randomUUID().toString(), manifestUrl, gameDir, "");
        checkFolderIsFree(pack);
        List<Pack> packs = new ArrayList<>(settings.packs());
        packs.add(pack);
        save(settings.withPacks(packs, settings.activeId()));
        return pack;
    }

    /**
     * Replaces the pack with the same id.
     *
     * @throws SyncException with {@link SyncFailure.FolderUsedByPack} when another pack uses the new folder
     */
    public synchronized void update(Pack pack) throws IOException {
        if (settings.find(pack.id()).isEmpty()) {
            throw new IllegalArgumentException("Unknown pack " + pack.id());
        }
        checkFolderIsFree(pack);
        List<Pack> packs = settings.packs().stream()
            .map(existing -> existing.id().equals(pack.id()) ? pack : existing)
            .toList();
        save(settings.withPacks(packs, settings.activeId()));
    }

    /** Forgets the pack; its game folder is left as it is. */
    public synchronized void remove(String id) throws IOException {
        List<Pack> packs = settings.packs().stream().filter(pack -> !pack.id().equals(id)).toList();
        save(settings.withPacks(packs, settings.activeId()));
    }

    public synchronized void select(String id) throws IOException {
        if (settings.find(id).isEmpty()) {
            throw new IllegalArgumentException("Unknown pack " + id);
        }
        save(settings.withPacks(settings.packs(), id));
    }

    /**
     * Keeps the name from the latest manifest for display. A failure to save is only logged: the name is
     * fetched again on the next check.
     */
    public synchronized void rememberName(String id, String name) {
        Optional<Pack> pack = settings.find(id);
        if (pack.isEmpty() || pack.get().name().equals(name)) {
            return;
        }
        try {
            update(pack.get().withName(name));
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not save the name of pack " + id, e);
        }
    }

    private void save(PackSettings next) throws IOException {
        repository.save(next);
        settings = next;
    }

    private void checkFolderIsFree(Pack candidate) throws SyncException {
        Path folder = canonical(candidate.gameDir());
        for (Pack other : settings.packs()) {
            if (other.id().equals(candidate.id())) continue;
            Path otherFolder = canonical(other.gameDir());
            if (folder.startsWith(otherFolder) || otherFolder.startsWith(folder)) {
                String name = other.name().isEmpty() ? other.manifestUrl() : other.name();
                throw new SyncException(new SyncFailure.FolderUsedByPack(candidate.gameDir().toString(), name));
            }
        }
    }

    /**
     * The real location of a folder, following symlinks of the part that exists, so two spellings of the
     * same folder compare equal even before the folder is created.
     */
    static Path canonical(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return absolute;
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(absolute));
        } catch (IOException e) {
            return absolute;
        }
    }
}
