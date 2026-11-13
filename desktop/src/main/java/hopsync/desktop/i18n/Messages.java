package hopsync.desktop.i18n;

import hopsync.core.failure.SyncFailure;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Every user-facing text, read from {@code messages.properties} (Spanish). Core and platform never hold
 * user-facing text: they report {@link SyncFailure}s, which are turned into messages here.
 */
public final class Messages {
    private static final Locale LOCALE = Locale.forLanguageTag("es");
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("hopsync.desktop.i18n.messages", LOCALE);

    private Messages() {
    }

    public static String get(String key, Object... args) {
        String pattern = BUNDLE.getString(key);
        return args.length == 0 ? pattern : new MessageFormat(pattern, LOCALE).format(args);
    }

    /**
     * What went wrong and what the player can do about it.
     */
    public static String failure(SyncFailure failure) {
        return switch (failure) {
            case SyncFailure.NotConfigured f -> get("failure.notConfigured");
            case SyncFailure.InvalidUrl f -> get("failure.invalidUrl", f.url());
            case SyncFailure.Network f -> get("failure.network", f.host());
            case SyncFailure.HttpStatus f -> get(httpStatusKey(f.status()), String.valueOf(f.status()));
            case SyncFailure.ResponseTooLarge f -> get("failure.responseTooLarge");
            case SyncFailure.ManifestInvalid f -> get("failure.manifestInvalid", f.detail());
            case SyncFailure.UnsupportedFormat f -> get("failure.unsupportedFormat", String.valueOf(f.found()), String.valueOf(f.supported()));
            case SyncFailure.HashMismatch f -> get("failure.hashMismatch", f.file());
            case SyncFailure.SizeMismatch f -> get("failure.sizeMismatch", f.file());
            case SyncFailure.UnsafePath f -> get("failure.unsafePath", f.path());
            case SyncFailure.FolderUnavailable f -> get("failure.folderUnavailable", f.path());
            case SyncFailure.FolderInUse f -> get("failure.folderInUse");
            case SyncFailure.FolderUsedByPack f -> get("failure.folderUsedByPack", f.packName());
            case SyncFailure.DiskFull f -> get("failure.diskFull", f.path());
            case SyncFailure.FileLocked f -> get("failure.fileLocked", f.path());
            case SyncFailure.PermissionDenied f -> get("failure.permissionDenied", f.path());
            case SyncFailure.Unexpected f -> get("failure.unexpected");
        };
    }

    private static String httpStatusKey(int status) {
        if (status == 404 || status == 410) return "failure.httpStatus.notFound";
        if (status == 401 || status == 403) return "failure.httpStatus.forbidden";
        if (status >= 500 || status == 429) return "failure.httpStatus.server";
        return "failure.httpStatus.other";
    }
}
