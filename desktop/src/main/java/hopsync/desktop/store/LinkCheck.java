package hopsync.desktop.store;

import hopsync.core.failure.SyncFailure;
import hopsync.core.manifest.PackInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * What HopSync knows about the pack link typed in a sheet.
 */
public sealed interface LinkCheck {

    /** Not checked yet, or changed since. */
    record Unchecked() implements LinkCheck {}

    record Checking(String url) implements LinkCheck {}

    /**
     * @param bytes  size of the whole pack, what a fresh folder downloads
     * @param copies folders of other packs with the same link: another copy needs another folder
     */
    record Valid(String url, PackInfo info, int mods, long bytes, List<Path> copies) implements LinkCheck {

        public Valid {
            copies = List.copyOf(copies);
        }
    }

    /** @param failure why the manifest could not be used, for {@link Problem#UNREADABLE} */
    record Invalid(Problem problem, SyncFailure failure) implements LinkCheck {}

    enum Problem { EMPTY, NOT_HTTPS, UNREADABLE }
}
