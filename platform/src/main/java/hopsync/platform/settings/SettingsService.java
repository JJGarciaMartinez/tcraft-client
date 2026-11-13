package hopsync.platform.settings;


import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Current player settings, shared by the services that need the manifest URL or the game folder.
 */
public class SettingsService {
    private final SettingsStore store;
    private final String presetManifestUrl;
    private final AtomicReference<Optional<UserSettings>> current;

    /**
     * @param presetManifestUrl manifest used until the player saves their own
     */
    public SettingsService(SettingsStore store, String presetManifestUrl) {
        this.presetManifestUrl = presetManifestUrl;
        this.store = store;
        this.current = new AtomicReference<>(store.load());
    }

    /** True once the player has saved settings at least once. */
    public boolean isConfigured() {
        return current.get().isPresent();
    }

    public Optional<UserSettings> get() {
        return current.get();
    }

    /**
     * The manifest URL to use: the saved one, otherwise the preset shipped in app.properties (may be empty).
     */
    public String getManifestUrl() {
        return current.get().map(UserSettings::manifestUrl).orElse(presetManifestUrl);
    }

    public void save(UserSettings settings) throws IOException {
        store.save(settings);
        current.set(Optional.of(settings));
    }
}
