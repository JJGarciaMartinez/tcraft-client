package hopsync.desktop.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The app version shown to players, read from the version.properties packaged in the JAR.
 */
public final class AppVersion {
    private static final System.Logger LOG = System.getLogger(AppVersion.class.getName());

    // Test builds are tagged a26.x.y, b26.x.y or rc26.x.y (see VERSION_GUIDE.md)
    private static final Pattern TEST_BUILD = Pattern.compile("^(a|b|rc)\\d");

    public static final String VERSION_APP = displayVersion(loadVersion());

    private AppVersion() {
    }

    static boolean isTestBuild(String version) {
        return TEST_BUILD.matcher(version).find();
    }

    static String displayVersion(String version) {
        return isTestBuild(version) ? version + " (test build)" : version;
    }

    private static String loadVersion() {
        try (InputStream input = AppVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input == null) {
                LOG.log(System.Logger.Level.WARNING, "version.properties not found, using default version");
                return "unknown";
            }
            Properties props = new Properties();
            props.load(input);
            return props.getProperty("app.version", "unknown");
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Could not load version.properties", e);
            return "unknown";
        }
    }
}
