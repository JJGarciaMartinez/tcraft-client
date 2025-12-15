package ui;

import config.AssetPaths;

public enum StatusType {
    SUCCESS(AssetPaths.CHECKMARK_ICON),
    WARNING(AssetPaths.WARNINGMARK_ICON),
    ERROR(AssetPaths.DANGERMARK_ICON),
    PROCESSING(AssetPaths.LOAD_ICON);
    
    private final String iconPath;
    StatusType(String iconPath) {
        this.iconPath = iconPath;
    }
    public String getIconPath() {
        return iconPath;
    }
}
