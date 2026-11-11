package hopsync.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes the UI scale correctly on Linux HiDPI screens.
 * <p>
 * On Linux, Java only honours {@code GDK_SCALE}; it ignores the desktop's scale (published as
 * {@code Xft.dpi}), so on a 200% screen the window renders at half size. This reads the desktop scale
 * and sets {@code sun.java2d.uiScale} before any AWT class is initialised. Windows and macOS scale
 * natively and are left alone, as is any scale the user set explicitly.
 * <p>
 * Java on Linux only supports integer scales, so fractional desktop scales are rounded.
 */
public final class DisplayScaling {
    private static final String UI_SCALE = "sun.java2d.uiScale";
    private static final Pattern XFT_DPI = Pattern.compile("^Xft\\.dpi:\\s*(\\d+(?:\\.\\d+)?)\\s*$", Pattern.MULTILINE);
    private static final Pattern GSETTINGS_UINT = Pattern.compile("^(?:uint32\\s+)?(\\d+)\\s*$");

    private DisplayScaling() {
    }

    /**
     * Must be called before any Swing or AWT class is used.
     */
    public static void configure() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux") || System.getProperty(UI_SCALE) != null || hasValue(System.getenv("GDK_SCALE"))) {
            return;
        }
        detectLinuxScale().ifPresent(scale -> System.setProperty(UI_SCALE, Integer.toString(scale)));
    }

    static Optional<Integer> detectLinuxScale() {
        Optional<Integer> fromXft = run(List.of("xrdb", "-query")).flatMap(DisplayScaling::scaleFromXrdb);
        if (fromXft.isPresent()) {
            return fromXft;
        }
        return run(List.of("gsettings", "get", "org.gnome.desktop.interface", "scaling-factor"))
            .flatMap(DisplayScaling::scaleFromGsettings);
    }

    /**
     * Scale from {@code xrdb -query} output, where 96 dpi is 100%.
     */
    static Optional<Integer> scaleFromXrdb(String output) {
        Matcher m = XFT_DPI.matcher(output);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(scaleFromDpi(Double.parseDouble(m.group(1))));
    }

    /**
     * Scale from {@code gsettings get org.gnome.desktop.interface scaling-factor}; 0 means "automatic".
     */
    static Optional<Integer> scaleFromGsettings(String output) {
        Matcher m = GSETTINGS_UINT.matcher(output.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        int factor = Integer.parseInt(m.group(1));
        return factor > 0 ? Optional.of(Math.min(factor, 4)) : Optional.empty();
    }

    static int scaleFromDpi(double dpi) {
        long scale = Math.round(dpi / 96.0);
        return (int) Math.max(1, Math.min(4, scale));
    }

    private static Optional<String> run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (InputStream in = process.getInputStream()) {
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    return Optional.empty();
                }
                return process.exitValue() == 0
                    ? Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    : Optional.empty();
            }
        } catch (IOException e) {
            // Tool not installed: fall through to the next source
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
