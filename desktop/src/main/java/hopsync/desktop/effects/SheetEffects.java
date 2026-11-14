package hopsync.desktop.effects;

import hopsync.core.failure.SyncException;
import hopsync.core.failure.SyncFailure;
import hopsync.core.failure.SyncFailures;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.desktop.config.AppVersion;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.store.GameFolder;
import hopsync.desktop.store.LinkCheck;
import hopsync.desktop.store.Preferences;
import hopsync.desktop.store.Sheet;
import hopsync.desktop.store.Store;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.platform.fs.InstanceFinder;
import hopsync.platform.manifest.ManifestService;
import hopsync.platform.system.SystemInfoService;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Side effects of the sheets: checking a pack link, finding launcher instances, telling which pack uses a
 * folder, and saving a new pack, a pack's settings or the app preferences.
 */
final class SheetEffects {
    private static final System.Logger LOG = System.getLogger(SheetEffects.class.getName());

    private final Store store;
    private final PackRegistry registry;
    private final ManifestService manifests;
    private final InstanceFinder instances;
    private final java.util.concurrent.Executor background;
    private final Path settingsFile;
    private final Runnable reloadPacks;
    private final Consumer<String> checkPack;
    private final Consumer<String> dropUpdate;
    private final Predicate<String> writing;

    /**
     * @param reloadPacks reloads the registry into the store after a save
     * @param checkPack   starts checking a newly added pack
     * @param dropUpdate  drops a pack's check, plan or place in the queue, made for its old link or folder
     * @param writing     whether a pack is writing files right now, when its settings cannot change
     */
    SheetEffects(Store store, PackRegistry registry, ManifestService manifests, InstanceFinder instances,
                 java.util.concurrent.Executor background, Path settingsFile, Runnable reloadPacks,
                 Consumer<String> checkPack, Consumer<String> dropUpdate, Predicate<String> writing) {
        this.store = store;
        this.registry = registry;
        this.manifests = manifests;
        this.instances = instances;
        this.background = background;
        this.settingsFile = settingsFile;
        this.reloadPacks = reloadPacks;
        this.checkPack = checkPack;
        this.dropUpdate = dropUpdate;
        this.writing = writing;
    }

    void run(UiAction action, UiState state) {
        switch (action) {
            case UiAction.CheckLink a -> fetchLink(state.sheet());
            case UiAction.WizardNext a when state.sheet() instanceof Sheet.AddPack wizard && wizard.step() == 2 ->
                findFolders();
            case UiAction.ChooseFolder a -> checkFolder(a.path(), state.sheet());
            case UiAction.FinishAddPack a when state.sheet() instanceof Sheet.AddPack wizard -> addPack(wizard);
            case UiAction.SavePackSettings a when state.sheet() instanceof Sheet.PackSettings settings ->
                savePackSettings(settings);
            case UiAction.SaveAppSettings a when state.sheet() instanceof Sheet.AppSettings settings ->
                saveAppSettings(settings.draft());
            case UiAction.CopyDiagnostics a -> copyDiagnostics();
            default -> { }
        }
    }

    private void fetchLink(Sheet sheet) {
        LinkCheck link = switch (sheet) {
            case Sheet.AddPack wizard -> wizard.link();
            case Sheet.PackSettings settings -> settings.link();
            case null, default -> null;
        };
        if (!(link instanceof LinkCheck.Checking checking)) return;
        background.execute(() -> {
            try {
                store.dispatch(new UiAction.LinkChecked(checking.url(), manifests.fetchManifest(checking.url())));
            } catch (Exception e) {
                store.dispatch(new UiAction.LinkFailed(checking.url(), SyncFailures.from(e, "")));
            }
        });
    }

    private void findFolders() {
        background.execute(() -> {
            List<GameFolder> found = instances.find().stream()
                .map(instance -> new GameFolder(kindOf(instance.launcher()), instance.name(), instance.gameDir(),
                    instance.jarCount(), usedBy(instance.gameDir(), null)))
                .toList();
            store.dispatch(new UiAction.FoldersFound(found));
        });
    }

    private void checkFolder(Path path, Sheet sheet) {
        String ownPackId = sheet instanceof Sheet.PackSettings settings ? settings.packId() : null;
        background.execute(() -> store.dispatch(new UiAction.FolderChecked(new GameFolder(GameFolder.Kind.OTHER, null,
            path, InstanceFinder.jarCount(path), usedBy(path, ownPackId)))));
    }

    private String usedBy(Path folder, String exceptPackId) {
        return registry.packUsing(folder, exceptPackId)
            .map(pack -> pack.name().isEmpty() ? pack.manifestUrl() : pack.name())
            .orElse(null);
    }

    private void addPack(Sheet.AddPack wizard) {
        if (!(wizard.link() instanceof LinkCheck.Valid valid) || wizard.chosen() == null || !wizard.chosen().isFree()) {
            return;
        }
        Path folder = wizard.chosen().path();
        background.execute(() -> {
            try {
                checkWritable(folder);
                Pack added = registry.add(valid.url(), folder);
                registry.rememberName(added.id(), valid.info().name());
                registry.select(added.id());
                store.dispatch(new UiAction.SheetSaved());
                reloadPacks.run();
                checkPack.accept(added.id());
            } catch (IOException | RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not add the pack " + valid.url(), e);
                store.dispatch(new UiAction.SheetFailed(SyncFailures.from(e, folder.toString())));
            }
        });
    }

    private void savePackSettings(Sheet.PackSettings settings) {
        Pack pack = registry.find(settings.packId()).orElse(null);
        if (pack == null) return;
        String url = settings.url().trim();
        boolean newLink = !url.equals(pack.manifestUrl());
        // A different link must have been checked: it may be another pack altogether
        if (newLink && !(settings.link() instanceof LinkCheck.Valid valid && valid.url().equals(url))) return;
        Path folder = settings.folder().path();
        boolean moved = newLink || !folder.equals(pack.gameDir());
        background.execute(() -> {
            try {
                if (writing.test(pack.id())) {
                    // The sheet disables saving; this covers an apply that started in between
                    store.dispatch(new UiAction.ShowToast(Messages.get("toast.waitForApply")));
                    return;
                }
                checkWritable(folder);
                if (moved) {
                    dropUpdate.accept(pack.id());
                }
                Pack changed = newLink ? pack.withManifestUrl(url).withName("") : pack;
                registry.update(changed.withGameDir(folder));
                store.dispatch(new UiAction.SheetSaved());
                reloadPacks.run();
            } catch (IOException | RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not save the settings of pack " + pack.id(), e);
                store.dispatch(new UiAction.SheetFailed(SyncFailures.from(e, folder.toString())));
            }
        });
    }

    private void saveAppSettings(Preferences draft) {
        background.execute(() -> {
            try {
                registry.updatePreferences(draft.checkOnStart(), draft.chainQueue(), draft.parallelDownloads());
                store.dispatch(new UiAction.SheetSaved());
                reloadPacks.run();
            } catch (IOException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not save the preferences", e);
                store.dispatch(new UiAction.ShowToast(Messages.get("settings.saveFailed",
                    Messages.failure(SyncFailures.from(e, settingsFile.toString())))));
            }
        });
    }

    /** The game folder must exist and accept new files; HopSync never creates it on its own. */
    private static void checkWritable(Path folder) throws SyncException {
        if (!Files.isDirectory(folder)) {
            throw new SyncException(new SyncFailure.FolderUnavailable(folder.toString()));
        }
        if (!Files.isWritable(folder)) {
            throw new SyncException(new SyncFailure.PermissionDenied(folder.toString()));
        }
    }

    private void copyDiagnostics() {
        String text = diagnostics();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        store.dispatch(new UiAction.ShowToast(Messages.get("toast.diagnosticsCopied")));
    }

    /** What a bug report needs: versions, system, scale, paths and packs, without personal data beyond paths. */
    String diagnostics() {
        StringBuilder text = new StringBuilder();
        text.append("HopSync ").append(AppVersion.VERSION_APP).append('\n');
        text.append("OS: ").append(SystemInfoService.getOS()).append(' ').append(System.getProperty("os.version"))
            .append('\n');
        text.append("Java: ").append(System.getProperty("java.version")).append(' ')
            .append(System.getProperty("java.vendor")).append('\n');
        text.append("UI scale: ").append(System.getProperty("sun.java2d.uiScale", "auto")).append('\n');
        text.append("Settings: ").append(settingsFile).append('\n');
        text.append("Packs: ").append(registry.packs().size()).append('\n');
        for (Pack pack : registry.packs()) {
            text.append("- ").append(pack.name().isEmpty() ? "?" : pack.name()).append(" | ").append(pack.manifestUrl())
                .append(" | ").append(pack.gameDir()).append('\n');
        }
        return text.toString();
    }

    private static GameFolder.Kind kindOf(InstanceFinder.Launcher launcher) {
        return switch (launcher) {
            case OFFICIAL -> GameFolder.Kind.OFFICIAL;
            case PRISM -> GameFolder.Kind.PRISM;
            case CURSEFORGE -> GameFolder.Kind.CURSEFORGE;
            case MODRINTH -> GameFolder.Kind.MODRINTH;
        };
    }
}
