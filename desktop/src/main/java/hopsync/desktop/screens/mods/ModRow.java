package hopsync.desktop.screens.mods;

import hopsync.desktop.kit.Icons.Glyph;
import hopsync.desktop.kit.StatusTag;
import hopsync.desktop.store.UiAction;

/**
 * One line of the mod list. Mods are 34 px rows; the other kinds structure the list around them.
 */
public sealed interface ModRow {

    /** Color of a row's leading icon. */
    enum IconTone { FAINT, ACCENT, ADDED, UPDATE, ERROR }

    /** Background treatment of a mod row. */
    enum Look { NORMAL, CURRENT, ERROR }

    /** @param tone color of the tag */
    record Tag(String text, StatusTag.Tone tone) {
    }

    /** Explanation above a tab's rows, e.g. that HopSync never touches the player's own mods. */
    record Banner(String html) implements ModRow {
    }

    record Group(String title, String note) implements ModRow {
    }

    /**
     * @param key        file name (or config path), unique within the list
     * @param oldVersion installed version shown before an arrow, or {@code null}
     * @param tag          status tag, or {@code null}
     * @param progress     download percent shown as a small bar instead of the tag, or {@code null}
     * @param statusAction a small button in the status column ("Reintentar"), or {@code null}
     * @param expandable   clicking it shows a {@link Detail} below
     */
    record Mod(String key, Glyph icon, IconTone iconTone, String name, String version, String oldVersion,
               String author, String size, Tag tag, Integer progress, UiAction statusAction, Look look,
               boolean expandable) implements ModRow {
    }

    /** @param url page opened by the link, or {@code null} for no link */
    record Detail(String key, String description, String file, String version, String size, String linkLabel,
                  String url) implements ModRow {
    }

    /** A clickable line such as "70 mods ya están al día · Ver todos". */
    record More(String text, UiAction action) implements ModRow {
    }

    record Message(String text) implements ModRow {
    }
}
