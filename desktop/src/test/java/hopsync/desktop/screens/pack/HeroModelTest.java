package hopsync.desktop.screens.pack;

import hopsync.core.event.SyncEvent;
import hopsync.core.failure.SyncFailure;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.ButtonStyle;
import hopsync.desktop.kit.StatusMark;
import hopsync.desktop.store.UiAction;
import hopsync.desktop.store.UiState;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static hopsync.desktop.store.StoreFixtures.DEMO;
import static hopsync.desktop.store.StoreFixtures.OTHER;
import static hopsync.desktop.store.StoreFixtures.loaded;
import static hopsync.desktop.store.StoreFixtures.manifest;
import static hopsync.desktop.store.StoreFixtures.plan;
import static hopsync.desktop.store.StoreFixtures.reduce;
import static hopsync.desktop.store.StoreFixtures.upToDatePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroModelTest {
    private static final String D = DEMO.id();
    private static final String O = OTHER.id();

    private static HeroModel hero(UiState state, String packId) {
        return HeroModel.of(state.packs().get(packId), state, ZoneOffset.UTC);
    }

    private static List<String> labels(HeroModel model) {
        return model.actions().stream().map(HeroModel.Action::label).toList();
    }

    @Test
    void uncheckedOffersToCheck() {
        HeroModel model = hero(loaded(), D);

        assertEquals(StatusMark.Kind.IDLE, model.mark());
        assertEquals(Messages.get("hero.state.unchecked"), model.title());
        assertEquals(List.of(Messages.get("hero.check")), labels(model));
        assertEquals(new UiAction.CheckPack(D), model.actions().getFirst().action());
        assertEquals(ButtonStyle.PRIMARY, model.actions().getFirst().style());
    }

    @Test
    void checkingCanBeCancelledAndTheMainButtonIsDisabled() {
        HeroModel model = hero(reduce(loaded(), new SyncEvent.UpdateStarted(D)), D);

        assertEquals(StatusMark.Kind.SPINNER, model.mark());
        assertEquals(new UiAction.CancelUpdate(D), model.actions().get(0).action());
        assertNull(model.actions().get(1).action(), "Buscando… is shown disabled");
    }

    @Test
    void reviewSummarizesThePlanAndOffersToApply() {
        HeroModel model = hero(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan())), D);

        assertEquals(StatusMark.Kind.WARN, model.mark());
        assertEquals(Messages.get("hero.state.review", 3), model.title());
        assertEquals(Messages.get("hero.detail.version", "1.2.0"), model.detail());
        assertEquals(List.of(Messages.get("hero.chip.update", 1), Messages.get("hero.chip.new", 1),
            Messages.get("hero.chip.remove", 1), "200 B"), model.tags().stream().map(HeroModel.Tag::text).toList());
        assertEquals(List.of(Messages.get("hero.notNow"), Messages.get("hero.apply")), labels(model));
        assertEquals(new UiAction.DismissPlan(D), model.actions().get(0).action());
        assertEquals(new UiAction.ConfirmPlan(D), model.actions().get(1).action());
    }

    @Test
    void reviewWhileAnotherPackAppliesOffersToQueue() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()),
            new SyncEvent.ApplyStarted(O), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()));

        assertEquals(Messages.get("hero.enqueue"), labels(hero(state, D)).get(1));
    }

    @Test
    void queuedSaysWhichPackItWaitsFor() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(O), new SyncEvent.PlanReady(O, plan()),
            new SyncEvent.ApplyStarted(O), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.Queued(D));
        HeroModel model = hero(state, D);

        assertEquals(StatusMark.Kind.QUEUED, model.mark());
        assertEquals(Messages.get("hero.detail.queuedBehind", state.packs().get(O).displayName()), model.detail());
        assertEquals(List.of(Messages.get("hero.unqueue")), labels(model));
        assertEquals(new UiAction.CancelUpdate(D), model.actions().getFirst().action());
    }

    @Test
    void applyingShowsFilesBytesSpeedAndTheModBeingDownloaded() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.ModDownloading(D, "new.jar"),
            new SyncEvent.Progress(D, 1, 3, 3 * 1024 * 1024, 8 * 1024 * 1024, 2 * 1024 * 1024));
        HeroModel.Applying applying = hero(state, D).applying();

        assertEquals(37, applying.percent());
        assertEquals(Messages.get("hero.progress.files", 1, 3, "3.0 / 8.0 MB"), applying.files());
        assertEquals(Messages.get("hero.progress.speed", "2.0 MB"), applying.speed());
        assertEquals(Messages.get("hero.downloadingVersion", "new.jar", "1.0"), applying.current(),
            "the mod's name and version, not its file");
        assertEquals(ButtonStyle.DANGER, hero(state, D).actions().getFirst().style());
    }

    @Test
    void speedIsHiddenUntilItIsKnown() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.Progress(D, 0, 3, 512, 8 * 1024 * 1024, 0));

        assertEquals("", hero(state, D).applying().speed());
        assertEquals(Messages.get("hero.progress.files", 0, 3, "0.0 / 8.0 MB"), hero(state, D).applying().files());
    }

    @Test
    void upToDateSaysWhenItWasChecked() {
        HeroModel model = hero(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpToDate(D, upToDatePlan())), D);

        assertEquals(StatusMark.Kind.OK, model.mark());
        assertEquals(Messages.get("hero.detail.checkedAt", "12:00"), model.detail());
        assertEquals(List.of(Messages.get("hero.checkAgain")), labels(model));
        assertNull(model.applying());
    }

    @Test
    void doneWithErrorsNamesTheFirstFailure() {
        SyncFailure locked = new SyncFailure.FileLocked("upd.jar");
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.ModCompleted(D, "upd.jar", false, locked),
            new SyncEvent.UpdateCompleted(D, 1, 1));
        HeroModel model = hero(state, D);

        assertEquals(StatusMark.Kind.WARN, model.mark());
        assertEquals(Messages.get("hero.state.doneWithErrors", 1), model.title());
        assertEquals(Messages.failure(locked), model.detail());
        assertEquals(List.of(Messages.get("hero.retryDownloads", 1)), labels(model));
        assertEquals(new UiAction.ContinueUpdate(D, Set.of("upd.jar")), model.actions().getFirst().action(),
            "retries only what failed");
    }

    @Test
    void cancelledWhileCheckingOffersToCheckAgain() {
        HeroModel model = hero(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpdateCancelled(D)), D);

        assertEquals(List.of(Messages.get("hero.continue")), labels(model));
        assertEquals(new UiAction.CheckPack(D), model.actions().getFirst().action());
        assertEquals(Messages.get("hero.detail.pending"), model.detail());
    }

    @Test
    void cancelledWhileApplyingSaysHowMuchWasDoneAndContinuesThePlan() {
        UiState state = reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.PlanReady(D, plan()),
            new SyncEvent.ApplyStarted(D), new SyncEvent.Progress(D, 1, 3, 100, 200, 0), new SyncEvent.UpdateCancelled(D));
        HeroModel model = hero(state, D);

        assertEquals(Messages.get("hero.detail.cancelled", 1, 3), model.detail());
        assertEquals(new UiAction.ContinueUpdate(D, plan().pendingFiles()), model.actions().getFirst().action());
    }

    @Test
    void errorExplainsAndOffersSettingsAndRetry() {
        SyncFailure offline = new SyncFailure.Network("github.com", "timeout");
        HeroModel model = hero(reduce(loaded(), new SyncEvent.UpdateStarted(D), new SyncEvent.UpdateFailed(D, offline)), D);

        assertEquals(StatusMark.Kind.ERROR, model.mark());
        assertEquals(Messages.failure(offline), model.detail());
        assertEquals(new UiAction.OpenSettings(), model.actions().get(0).action());
        assertEquals(new UiAction.CheckPack(D), model.actions().get(1).action());
    }

    @Test
    void metaLineNeedsTheManifest() {
        UiState state = loaded();
        assertNull(HeroModel.meta(state.packs().get(D)));

        state = reduce(state, new UiAction.ManifestLoaded(D, manifest()));
        String meta = HeroModel.meta(state.packs().get(D));
        assertNotNull(meta);
        assertTrue(meta.contains("1.21.1") && meta.contains("neoforge") && meta.contains("3 mods"));
    }
}
