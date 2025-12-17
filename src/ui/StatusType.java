package ui;

import config.AssetPaths;

/**
 * An enumeration representing different status types, each associated
 * with a specific icon path. The icon path is sourced from a predefined
 * constants class, and it indicates the visual representation of the status.
 * <p>
 * This enumeration is useful for situations where status representation
 * along with an icon is required, such as in UI feedback or logging systems.
 * <p>
 * Enum constants:
 * - SUCCESS: Indicates a successful operation, paired with a checkmark icon.
 * - WARNING: Represents a warning condition, paired with a warning icon.
 * - ERROR: Refers to an error condition, paired with a danger icon.
 * - PROCESSING: Indicates an ongoing process, paired with a loading icon.
 * - IN_SYSTEM: Denotes an entity or condition present in the system, paired with a system icon.
 * <p>
 * Provides a method to retrieve the respective icon path for each status type.
 */
public enum StatusType {
    SUCCESS(AssetPaths.CHECKMARK_ICON),
    WARNING(AssetPaths.WARNINGMARK_ICON),
    ERROR(AssetPaths.DANGERMARK_ICON),
    PROCESSING(AssetPaths.LOAD_ICON),
    IN_SYSTEM(AssetPaths.SYSTEM_ICON);

    /**
     * The file path to the icon associated with a specific status type.
     * This path specifies the location of the visual representation
     * corresponding to the particular status, such as success, warning,
     * error, processing, or an in-system state.
     * The icon path is defined using a constant from a predefined
     * resources class.
     */
    private final String iconPath;

    /**
     * Constructs a new StatusType enumeration constant with the specified icon path.
     *
     * @param iconPath The file path to the icon associated with the status type.
     *                 This path defines the visual representation corresponding to
     *                 the specific status (e.g., success, warning, error, processing, or in-system).
     */
    StatusType(String iconPath) {
        this.iconPath = iconPath;
    }

    /**
     * Retrieves the file path to the icon associated with the specific status type.
     * The icon path represents the visual representation of the status, such as
     * success, warning, error, processing, or in-system states.
     *
     * @return The file path to the icon as a string.
     */
    public String getIconPath() {
        return iconPath;
    }
}
