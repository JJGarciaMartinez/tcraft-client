package hopsync.desktop.store;

import hopsync.core.failure.SyncFailure;

import java.util.List;

/**
 * A sheet open on top of the window. Everything it shows lives here, so it renders like any other view.
 */
public sealed interface Sheet {

    /**
     * The add-pack wizard: step 1 checks the link, step 2 picks the folder.
     *
     * @param firstRun shown in the whole window while there are no packs; cannot be closed
     * @param folders  folders found on disk, {@code null} while they are being looked for
     * @param chosen   the folder picked in step 2, or {@code null}
     * @param error    why saving failed, or {@code null}
     */
    record AddPack(boolean firstRun, int step, String url, LinkCheck link, List<GameFolder> folders,
                   GameFolder chosen, SyncFailure error) implements Sheet {

        public static AddPack start(boolean firstRun) {
            return new AddPack(firstRun, 1, "", new LinkCheck.Unchecked(), null, null, null);
        }

        AddPack withUrl(String newUrl, LinkCheck newLink) {
            return new AddPack(firstRun, step, newUrl, newLink, folders, chosen, error);
        }

        AddPack withLink(LinkCheck newLink) {
            return new AddPack(firstRun, step, url, newLink, folders, chosen, error);
        }

        AddPack withStep(int newStep) {
            return new AddPack(firstRun, newStep, url, link, newStep == 2 ? null : folders, newStep == 2 ? null : chosen,
                null);
        }

        AddPack withFolders(List<GameFolder> found) {
            // Preselect the first free folder, as the design does
            GameFolder first = found.stream().filter(GameFolder::isFree).findFirst().orElse(null);
            return new AddPack(firstRun, step, url, link, List.copyOf(found), chosen != null ? chosen : first, error);
        }

        AddPack withChosen(GameFolder folder) {
            return new AddPack(firstRun, step, url, link, folders, folder, null);
        }

        AddPack withError(SyncFailure failure) {
            return new AddPack(firstRun, step, url, link, folders, chosen, failure);
        }
    }

    /**
     * Settings of the selected pack.
     *
     * @param folder the folder picked, or the pack's own
     * @param error  why saving failed, or {@code null}
     */
    record PackSettings(String packId, Section section, String url, LinkCheck link, GameFolder folder,
                        SyncFailure error) implements Sheet {

        public enum Section { LINK, FOLDER, REMOVE }

        PackSettings withSection(Section newSection) {
            return new PackSettings(packId, newSection, url, link, folder, null);
        }

        PackSettings withUrl(String newUrl, LinkCheck newLink) {
            return new PackSettings(packId, section, newUrl, newLink, folder, null);
        }

        PackSettings withLink(LinkCheck newLink) {
            return new PackSettings(packId, section, url, newLink, folder, error);
        }

        PackSettings withFolder(GameFolder newFolder) {
            return new PackSettings(packId, section, url, link, newFolder, null);
        }

        PackSettings withError(SyncFailure failure) {
            return new PackSettings(packId, section, url, link, folder, failure);
        }
    }

    /** Preferences for every pack, and information about the app. */
    record AppSettings(Section section, Preferences draft) implements Sheet {

        public enum Section { GENERAL, ABOUT }
    }
}
