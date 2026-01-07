package config;

import java.awt.Color;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration loader that reads from app.properties.
 * Falls back to sensible defaults if properties file is missing.
 */
public class AppProperties {
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    // Network
    public static int getConnectTimeout() { return getInt("network.connect.timeout", 10000); }
    public static int getReadTimeout() { return getInt("network.read.timeout", 30000); }
    public static String getUserAgent() { return get("network.user.agent", "Mozilla/5.0"); }
    public static String getManifestUrl() {
        return get("manifest.url", "https://raw.githubusercontent.com/JJGarciaMartinez/tcraft-mods-list/main/modList/mod-list.json");
    }

    // Theme - Backgrounds
    public static Color getBackgroundDarker() { return getColor("theme.bg.darker", 0x1E1E1E); }
    public static Color getBackgroundDark() { return getColor("theme.bg.dark", 0x232323); }
    public static Color getBackgroundMedium() { return getColor("theme.bg.medium", 0x282828); }
    public static Color getBorderColor() { return getColor("theme.border", 0x505050); }

    // Theme - Text
    public static Color getTextPrimary() { return getColor("theme.text.primary", 0xFFFFFF); }
    public static Color getTextSecondary() { return getColor("theme.text.secondary", 0xB4B4B4); }
    public static Color getTextTertiary() { return getColor("theme.text.tertiary", 0x969696); }

    // Theme - Accents
    public static Color getAccentSuccess() { return getColor("theme.accent.success", 0x64C864); }
    public static Color getAccentSuccessLight() { return getColor("theme.accent.success.light", 0x96C896); }
    public static Color getAccentError() { return getColor("theme.accent.error", 0xC86464); }
    public static Color getAccentWarning() { return getColor("theme.accent.warning", 0xFFAA00); }
    public static Color getAccentWarningLight() { return getColor("theme.accent.warning.light", 0xFFC864); }
    public static Color getAccentInfo() { return getColor("theme.accent.info", 0x6496FF); }
    public static Color getAccentVersion() { return getColor("theme.accent.version", 0x9696FA); }

    // Font
    public static String getFallbackFont() { return get("font.fallback", "Arial"); }

    private static void ensureLoaded() {
        if (!loaded) {
            try (InputStream input = AppProperties.class.getClassLoader()
                    .getResourceAsStream("app.properties")) {
                if (input != null) props.load(input);
            } catch (Exception e) {
                System.err.println("Warning: Could not load app.properties");
            }
            loaded = true;
        }
    }

    private static String get(String key, String defaultValue) {
        ensureLoaded();
        return props.getProperty(key, defaultValue);
    }

    private static int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static Color getColor(String key, int defaultValue) {
        try { return new Color(Integer.parseInt(get(key, Integer.toHexString(defaultValue)), 16)); }
        catch (NumberFormatException e) { return new Color(defaultValue); }
    }
}
