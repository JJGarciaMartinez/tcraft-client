package hopsync.desktop.i18n;

import hopsync.core.failure.SyncFailure;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    static final List<SyncFailure> ONE_OF_EACH = List.of(
        new SyncFailure.NotConfigured(),
        new SyncFailure.InvalidUrl("http://x.org/m.json"),
        new SyncFailure.Network("github.com", "timeout"),
        new SyncFailure.HttpStatus(404, "https://x.org/m.json"),
        new SyncFailure.ResponseTooLarge("https://x.org/m.json", 5),
        new SyncFailure.ManifestInvalid("mods[0].sha512 has an invalid format"),
        new SyncFailure.UnsupportedFormat(3, 2),
        new SyncFailure.HashMismatch("create.jar"),
        new SyncFailure.SizeMismatch("create.jar", 10, 9),
        new SyncFailure.UnsafePath("../evil.jar"),
        new SyncFailure.FolderUnavailable("/game/mods"),
        new SyncFailure.FolderInUse("/game"),
        new SyncFailure.FolderUsedByPack("/game", "Demo"),
        new SyncFailure.DiskFull("create.jar"),
        new SyncFailure.FileLocked("create.jar"),
        new SyncFailure.PermissionDenied("/game/mods"),
        new SyncFailure.Unexpected("bug"));

    @Test
    void everyFailureHasAMessage() {
        Set<Class<?>> covered = ONE_OF_EACH.stream().map(Object::getClass).collect(Collectors.toSet());
        assertEquals(Set.copyOf(Arrays.asList(SyncFailure.class.getPermittedSubclasses())), covered,
            "add new SyncFailure types to ONE_OF_EACH and messages.properties");

        for (SyncFailure failure : ONE_OF_EACH) {
            String message = Messages.failure(failure);
            assertFalse(message.isBlank(), failure.toString());
            assertFalse(message.contains("{"), "unfilled placeholder in " + message);
            assertNotEquals(failure.describe(), message, "English log text shown to the player");
        }
    }

    @Test
    void failuresNameWhatThePlayerNeeds() {
        assertTrue(Messages.failure(new SyncFailure.Network("github.com", "timeout")).contains("github.com"));
        assertTrue(Messages.failure(new SyncFailure.FileLocked("create.jar")).contains("Minecraft"));
        assertTrue(Messages.failure(new SyncFailure.HttpStatus(503, "u")).contains("503"));
    }

    @Test
    void httpStatusesGetTheRightAdvice() {
        String notFound = Messages.failure(new SyncFailure.HttpStatus(404, "u"));
        String server = Messages.failure(new SyncFailure.HttpStatus(502, "u"));
        assertTrue(notFound.contains("Ajustes"), notFound);
        assertTrue(server.contains("más tarde"), server);
    }

    @Test
    void pluralsAndAccents() {
        assertEquals("Completado con 1 error", Messages.get("status.completedWithErrors", 1));
        assertEquals("Completado con 3 errores", Messages.get("status.completedWithErrors", 3));
        assertEquals("0 mods instalados", Messages.get("mods.installedCount", 0));
        assertEquals("1 mod instalado", Messages.get("mods.installedCount", 1));
        assertEquals("¡Actualización completada!", Messages.get("status.completed"));
    }
}
