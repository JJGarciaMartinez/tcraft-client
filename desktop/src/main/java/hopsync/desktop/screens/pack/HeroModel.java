package hopsync.desktop.screens.pack;

import hopsync.core.manifest.PackInfo;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.Icons.Glyph;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.kit.StatusTag;
import hopsync.desktop.store.PackUiState;
import hopsync.desktop.store.RowStatus;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * What the pack header shows for the pack's phase: the status line, the summary tags and the contextual
 * actions. Pure, so every state of the design's table is tested without a window.
 *
 * @param detail      text after the title, or {@code null}
 * @param percent     progress bar value while applying, or {@code null} to hide the bar
 * @param currentFile file being downloaded, or {@code null}
 */
public record HeroModel(StatusMark.Kind mark, String title, String detail, List<Tag> tags, List<Action> actions,
                        Integer percent, String currentFile) {

    public record Tag(String text, StatusTag.Tone tone) {
    }

    /** @param action what the button dispatches, or {@code null} for a disabled button */
    public record Action(String label, Glyph icon, UiAction action, ButtonStyle style) {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static HeroModel of(PackUiState pack, UiState state, ZoneId zone) {
        String id = pack.packId();
        Action checkAgain = new Action(Messages.get("hero.checkAgain"), Glyph.REFRESH, new UiAction.CheckPack(id),
            ButtonStyle.SECONDARY);
        return switch (pack.phase()) {
            case UNCHECKED -> new HeroModel(StatusMark.Kind.IDLE, Messages.get("hero.state.unchecked"), null, List.of(),
                List.of(new Action(Messages.get("hero.check"), Glyph.REFRESH, new UiAction.CheckPack(id), ButtonStyle.PRIMARY)),
                null, null);
            case CHECKING -> new HeroModel(StatusMark.Kind.SPINNER, Messages.get("hero.state.checking"),
                pack.modCount() > 0 ? Messages.get("hero.detail.mods", pack.modCount()) : null, List.of(),
                List.of(new Action(Messages.get("hero.cancel"), null, new UiAction.CancelUpdate(id), ButtonStyle.QUIET),
                    new Action(Messages.get("hero.checking"), Glyph.SPINNER, null, ButtonStyle.PRIMARY)),
                null, null);
            case REVIEW -> {
                PlanSummary summary = PlanSummary.of(pack.plan());
                boolean waits = state.anotherPackApplying(id);
                yield new HeroModel(StatusMark.Kind.WARN, Messages.get("hero.state.review", summary.changes()),
                    version(pack), reviewTags(summary),
                    List.of(new Action(Messages.get("hero.notNow"), null, new UiAction.DismissPlan(id), ButtonStyle.QUIET),
                        new Action(Messages.get(waits ? "hero.enqueue" : "hero.apply"), waits ? Glyph.CLOCK : Glyph.CHECK,
                            new UiAction.ConfirmPlan(id), ButtonStyle.PRIMARY)),
                    null, null);
            }
            case QUEUED -> {
                PlanSummary summary = PlanSummary.of(pack.plan());
                String behind = state.applyingPack()
                    .map(other -> Messages.get("hero.detail.queuedBehind", other.displayName()))
                    .orElse(Messages.get("hero.detail.queuedCurrent"));
                yield new HeroModel(StatusMark.Kind.QUEUED, Messages.get("hero.state.queued"), behind,
                    List.of(new Tag(Messages.get("hero.chip.changes", summary.changes(),
                        PlanSummary.formatBytes(summary.bytes())), StatusTag.Tone.NEUTRAL)),
                    List.of(new Action(Messages.get("hero.unqueue"), null, new UiAction.CancelUpdate(id), ButtonStyle.QUIET)),
                    null, null);
            }
            case APPLYING -> new HeroModel(StatusMark.Kind.SPINNER, Messages.get("hero.state.applying"),
                Messages.get("hero.detail.applying"), List.of(),
                List.of(new Action(Messages.get("hero.cancel"), Glyph.X, new UiAction.CancelUpdate(id), ButtonStyle.DANGER)),
                pack.progress().percent(), pack.progress().currentFile());
            case UP_TO_DATE -> new HeroModel(StatusMark.Kind.OK, Messages.get("hero.state.upToDate"),
                pack.lastChecked() == null ? null
                    : Messages.get("hero.detail.checkedAt", TIME.withZone(zone).format(pack.lastChecked())),
                List.of(), List.of(checkAgain), null, null);
            case DONE -> new HeroModel(StatusMark.Kind.OK, Messages.get("hero.state.upToDate"),
                Messages.get("hero.detail.applied", pack.result().succeeded()), List.of(), List.of(checkAgain), null, null);
            case DONE_WITH_ERRORS -> new HeroModel(StatusMark.Kind.WARN,
                Messages.get("hero.state.doneWithErrors", pack.result().failed()), firstFailure(pack), List.of(),
                List.of(new Action(Messages.get("hero.checkAgain"), Glyph.REFRESH, new UiAction.CheckPack(id),
                    ButtonStyle.PRIMARY)),
                null, null);
            case CANCELLED -> new HeroModel(StatusMark.Kind.IDLE, Messages.get("hero.state.cancelled"),
                Messages.get("hero.detail.pending"), List.of(),
                List.of(new Action(Messages.get("hero.continue"), Glyph.REFRESH, new UiAction.CheckPack(id),
                    ButtonStyle.PRIMARY)),
                null, null);
            case ERROR -> new HeroModel(StatusMark.Kind.ERROR, Messages.get("hero.state.error"),
                pack.error() == null ? null : Messages.failure(pack.error()), List.of(),
                List.of(new Action(Messages.get("hero.packSettings"), null, new UiAction.OpenSettings(), ButtonStyle.QUIET),
                    new Action(Messages.get("hero.retry"), Glyph.REFRESH, new UiAction.CheckPack(id), ButtonStyle.PRIMARY)),
                null, null);
        };
    }

    /** "Minecraft 1.21.1 · neoforge · 77 mods", or {@code null} before the manifest is known. */
    public static String meta(PackUiState pack) {
        PackInfo info = pack.packInfo();
        return info == null ? null : Messages.get("hero.meta", info.minecraft(), info.loader(), pack.modCount());
    }

    private static String version(PackUiState pack) {
        return pack.packInfo() == null ? null : Messages.get("hero.detail.version", pack.packInfo().version());
    }

    private static List<Tag> reviewTags(PlanSummary summary) {
        List<Tag> tags = new ArrayList<>();
        if (summary.updatedMods() > 0) tags.add(new Tag(Messages.get("hero.chip.update", summary.updatedMods()), StatusTag.Tone.UPDATE));
        if (summary.newMods() > 0) tags.add(new Tag(Messages.get("hero.chip.new", summary.newMods()), StatusTag.Tone.ADDED));
        if (summary.removedMods() > 0) tags.add(new Tag(Messages.get("hero.chip.remove", summary.removedMods()), StatusTag.Tone.ERROR));
        if (summary.configs() > 0) tags.add(new Tag(Messages.get("hero.chip.configs", summary.configs()), StatusTag.Tone.NEUTRAL));
        tags.add(new Tag(PlanSummary.formatBytes(summary.bytes()), StatusTag.Tone.NEUTRAL));
        return tags;
    }

    private static String firstFailure(PackUiState pack) {
        return pack.rowStatus().values().stream()
            .filter(status -> status instanceof RowStatus.Failed failed && failed.failure() != null)
            .map(status -> Messages.failure(((RowStatus.Failed) status).failure()))
            .findFirst().orElse(null);
    }
}
