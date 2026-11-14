package hopsync.desktop.screens.mods;

import hopsync.core.manifest.ConfigEntry;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.ModEntry;
import hopsync.core.manifest.ModSource;
import hopsync.core.manifest.PackInfo;
import hopsync.core.sync.OwnMods;
import hopsync.core.sync.SyncPlan;
import hopsync.core.sync.SyncPlan.ConfigAction;
import hopsync.core.sync.SyncPlan.ModChange;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Icons.Glyph;
import hopsync.desktop.kit.StatusTag.Tone;
import hopsync.desktop.screens.pack.HeroModel;
import hopsync.desktop.screens.pack.PlanSummary;
import hopsync.desktop.store.InstalledMods;
import hopsync.desktop.store.PackPhase;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.RowStatus;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import hopsync.desktop.store.UiState.Filter;
import hopsync.desktop.store.UiState.Tab;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The rows of the mod list for the selected pack, tab, filter and search. Pure, so every combination is
 * tested without a window.
 *
 * @param packCount   mods the pack lists
 * @param ownCount    the player's own jars
 * @param configCount configs the pack lists
 * @param showsFilter whether "Solo cambios / Todos" applies: there is a plan with changes to show
 */
public record ModRowModel(List<ModRow> rows, int packCount, int ownCount, int configCount, boolean showsFilter) {

    /** Phases in which the list reflects the last plan. */
    private static final Set<PackPhase> SHOWS_PLAN = EnumSet.of(PackPhase.REVIEW, PackPhase.QUEUED,
        PackPhase.APPLYING, PackPhase.UP_TO_DATE, PackPhase.DONE, PackPhase.DONE_WITH_ERRORS, PackPhase.CANCELLED);

    public static ModRowModel of(PackUiState pack, UiState state) {
        Manifest manifest = pack.manifest();
        List<String> own = OwnMods.of(pack.installed().jars(), pack.installed().managed(), manifest);
        SyncPlan plan = pack.plan() != null && SHOWS_PLAN.contains(pack.phase()) ? pack.plan() : null;
        boolean hasChanges = plan != null && PlanSummary.of(plan).changes() > 0;
        Predicate<String> matches = matcher(state.query());

        List<ModRow> rows = switch (state.tab()) {
            case OWN_MODS -> ownRows(own, pack.installed(), matches);
            case CONFIGS -> configRows(manifest, plan, matches);
            case PACK -> {
                if (manifest == null) {
                    yield List.of(new ModRow.Message(pack.manifestError() != null
                        ? Messages.failure(pack.manifestError()) : Messages.get("mods.loading")));
                }
                yield packRows(pack, plan, hasChanges && state.filter() == Filter.CHANGES, matches,
                    state.expandedRow());
            }
        };
        if (rows.isEmpty() && !state.query().isBlank()) {
            rows = List.of(new ModRow.Message(Messages.get("mods.noMatch", state.query())));
        }
        return new ModRowModel(rows, manifest == null ? 0 : manifest.mods().size(), own.size(),
            manifest == null ? 0 : manifest.configs().size(), hasChanges && state.tab() == Tab.PACK);
    }

    private static List<ModRow> packRows(PackUiState pack, SyncPlan plan, boolean changesOnly,
                                         Predicate<String> matches, String expanded) {
        Map<String, ModChange> changes = new HashMap<>();
        if (plan != null) {
            plan.mods().forEach(action -> changes.put(action.mod().file(), action.change()));
        }
        List<ModRow> rows = new ArrayList<>();
        if (changesOnly) {
            group(rows, "mods.group.update", modsWith(pack.manifest(), changes, ModChange.UPDATE, matches), pack, changes,
                expanded);
            group(rows, "mods.group.new", modsWith(pack.manifest(), changes, ModChange.NEW, matches), pack, changes,
                expanded);
            List<String> removals = plan.removals().stream().filter(matches).toList();
            if (!removals.isEmpty()) {
                rows.add(new ModRow.Group(Messages.get("mods.group.remove", removals.size()),
                    Messages.get("mods.group.removeNote")));
                removals.forEach(file -> rows.add(removalRow(pack, file)));
            }
            long upToDate = changes.values().stream().filter(change -> change == ModChange.UP_TO_DATE).count();
            if (upToDate > 0) {
                rows.add(new ModRow.More(Messages.get("mods.upToDateMore", upToDate), new UiAction.SetFilter(Filter.ALL)));
            }
            return rows;
        }
        for (ModEntry mod : pack.manifest().mods()) {
            if (matches.test(mod.name())) {
                addWithDetail(rows, modRow(pack, mod, changes.get(mod.file())), mod, pack.packInfo(), expanded);
            }
        }
        if (plan != null) {
            plan.removals().stream().filter(matches).forEach(file -> rows.add(removalRow(pack, file)));
        }
        return rows;
    }

    private static List<ModEntry> modsWith(Manifest manifest, Map<String, ModChange> changes, ModChange change,
                                           Predicate<String> matches) {
        return manifest.mods().stream()
            .filter(mod -> changes.get(mod.file()) == change && matches.test(mod.name()))
            .toList();
    }

    private static void group(List<ModRow> rows, String titleKey, List<ModEntry> mods, PackUiState pack,
                              Map<String, ModChange> changes, String expanded) {
        if (mods.isEmpty()) return;
        rows.add(new ModRow.Group(Messages.get(titleKey, mods.size()), null));
        mods.forEach(mod -> addWithDetail(rows, modRow(pack, mod, changes.get(mod.file())), mod, pack.packInfo(),
            expanded));
    }

    private static void addWithDetail(List<ModRow> rows, ModRow.Mod row, ModEntry mod, PackInfo pack,
                                      String expanded) {
        rows.add(row);
        if (!row.key().equals(expanded)) return;
        String url = ModLinks.pageOf(mod);
        rows.add(new ModRow.Detail(row.key(),
            mod.description() == null || mod.description().isBlank()
                ? Messages.get("mods.detail.noDescription") : mod.description(),
            mod.file(), mod.version(), PlanSummary.formatBytes(mod.size()),
            url == null ? null : Messages.get(mod.source() == ModSource.MODRINTH
                ? "mods.detail.modrinth" : "mods.detail.repository"),
            url));
    }

    /**
     * A pack mod: what the plan will do to it, or what happened to it while applying.
     *
     * @param change the plan's change, or {@code null} without a plan
     */
    static ModRow.Mod modRow(PackUiState pack, ModEntry mod, ModChange change) {
        String version = VersionLabel.clean(mod.version(), pack.packInfo());
        String oldVersion = change == ModChange.UPDATE ? oldVersion(pack, mod.file()) : null;
        String size = PlanSummary.formatBytes(mod.size());
        String author = mod.author() == null ? "" : mod.author();
        return switch (pack.rowStatus().get(mod.file())) {
            case RowStatus.Downloading s -> new ModRow.Mod(mod.file(), Glyph.SPINNER, ModRow.IconTone.ACCENT, mod.name(),
                version, oldVersion, author, size, null, s.percent(), null, ModRow.Look.CURRENT, true);
            case RowStatus.Done s -> row(mod, Glyph.CHECK, ModRow.IconTone.ACCENT, version, oldVersion, author, size,
                tag(change == ModChange.UPDATE ? "mods.status.updated" : "mods.status.installed", Tone.ACCENT),
                ModRow.Look.NORMAL);
            // The reason takes the author's place, as in the design
            // Retrying re-checks the pack and applies what failed, unless the pack changed meanwhile
            case RowStatus.Failed s -> new ModRow.Mod(mod.file(), Glyph.WARN, ModRow.IconTone.ERROR, mod.name(), version,
                oldVersion, s.failure() == null ? Messages.get("mods.status.failed") : Messages.failure(s.failure()),
                size, null, null, pack.phase().isBusy() ? null : HeroModel.retryFailed(pack), ModRow.Look.ERROR, true);
            case RowStatus.Pending s -> row(mod, Glyph.PENDING, ModRow.IconTone.FAINT, version, oldVersion, author, size,
                tag("mods.status.pending", Tone.NEUTRAL), ModRow.Look.NORMAL);
            case RowStatus.Removed s -> throw new IllegalStateException("a pack mod is never removed: " + mod.file());
            case null -> switch (change) {
                case UPDATE -> row(mod, Glyph.UP, ModRow.IconTone.UPDATE, version, oldVersion, author, size,
                    tag("mods.status.update", Tone.UPDATE), ModRow.Look.NORMAL);
                case NEW -> row(mod, Glyph.PLUS, ModRow.IconTone.ADDED, version, null, author, size,
                    tag("mods.status.new", Tone.ADDED), ModRow.Look.NORMAL);
                case UP_TO_DATE -> row(mod, Glyph.CHECK, ModRow.IconTone.FAINT, version, null, author, size, null,
                    ModRow.Look.NORMAL);
                // Not checked yet: a faint check when the jar is there, a faint dot when it is missing
                case null -> row(mod, pack.installed().jars().contains(mod.file()) ? Glyph.CHECK : Glyph.PENDING,
                    ModRow.IconTone.FAINT, version, null, author, size, null, ModRow.Look.NORMAL);
            };
        };
    }

    private static ModRow.Mod row(ModEntry mod, Glyph icon, ModRow.IconTone tone, String version, String oldVersion,
                                  String author, String size, ModRow.Tag tag, ModRow.Look look) {
        return new ModRow.Mod(mod.file(), icon, tone, mod.name(), version, oldVersion, author, size, tag, null, null,
            look, true);
    }

    private static ModRow.Tag tag(String key, Tone tone) {
        return new ModRow.Tag(Messages.get(key), tone);
    }

    private static ModRow.Mod removalRow(PackUiState pack, String file) {
        boolean removed = pack.rowStatus().get(file) instanceof RowStatus.Removed;
        return new ModRow.Mod(file, removed ? Glyph.CHECK : Glyph.MINUS,
            removed ? ModRow.IconTone.ACCENT : ModRow.IconTone.ERROR, file,
            VersionLabel.clean(pack.installed().versions().get(file), pack.packInfo()), null, "", "",
            removed ? tag("mods.status.removed", Tone.ACCENT) : tag("mods.status.remove", Tone.ERROR), null, null,
            ModRow.Look.NORMAL, false);
    }

    /** The version HopSync recorded when it installed the file, or {@code null} when it did not record one. */
    private static String oldVersion(PackUiState pack, String file) {
        String recorded = pack.installed().versions().get(file);
        return recorded == null ? null : VersionLabel.clean(recorded, pack.packInfo());
    }

    private static List<ModRow> ownRows(List<String> own, InstalledMods installed, Predicate<String> matches) {
        List<ModRow> rows = new ArrayList<>();
        rows.add(new ModRow.Banner(Messages.get("mods.own.banner")));
        if (own.isEmpty()) {
            rows.add(new ModRow.Message(Messages.get("mods.own.empty")));
            return rows;
        }
        List<String> shown = own.stream().filter(matches).toList();
        if (shown.isEmpty()) {
            // Nothing matches the search: the caller says so
            return List.of();
        }
        for (String file : shown) {
            Long size = installed.sizes().get(file);
            rows.add(new ModRow.Mod(file, Glyph.USER, ModRow.IconTone.FAINT, file, "—", null,
                Messages.get("mods.own.author"), size == null ? "" : PlanSummary.formatBytes(size),
                tag("mods.own.tag", Tone.NEUTRAL), null, null, ModRow.Look.NORMAL, false));
        }
        return rows;
    }

    private static List<ModRow> configRows(Manifest manifest, SyncPlan plan, Predicate<String> matches) {
        if (manifest == null) return List.of(new ModRow.Message(Messages.get("mods.loading")));
        Map<String, ConfigAction> actions = new HashMap<>();
        if (plan != null) {
            plan.configs().forEach(action -> actions.put(action.config().path(), action));
        }
        List<ConfigEntry> shown = manifest.configs().stream().filter(config -> matches.test(config.path())).toList();
        if (shown.isEmpty()) return List.of();
        List<ModRow> rows = new ArrayList<>();
        rows.add(new ModRow.Group(Messages.get("mods.configs.group"), Messages.get("mods.configs.note")));
        for (ConfigEntry config : shown) {
            rows.add(new ModRow.Mod(config.path(), Glyph.FILE, ModRow.IconTone.FAINT, config.path(), "—", null,
                Messages.get("mods.configs.author"), PlanSummary.formatBytes(config.size()),
                configTag(actions.get(config.path())), null, null, ModRow.Look.NORMAL, false));
        }
        return rows;
    }

    private static ModRow.Tag configTag(ConfigAction action) {
        if (action == null) return null;
        return switch (action.change()) {
            case INSTALL -> tag("mods.config.new", Tone.ADDED);
            case REPLACE -> tag("mods.config.update", Tone.UPDATE);
            case KEEP_EDITED -> tag("mods.config.kept", Tone.NEUTRAL);
            case UP_TO_DATE -> tag("mods.config.upToDate", Tone.ACCENT);
        };
    }

    private static Predicate<String> matcher(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return text -> needle.isEmpty() || text.toLowerCase(Locale.ROOT).contains(needle);
    }
}