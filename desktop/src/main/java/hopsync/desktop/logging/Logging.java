package hopsync.desktop.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Sends every {@link System.Logger} message to rotating files in the user's HopSync folder, so players
 * can attach them when reporting a problem. Warnings and errors also go to the console.
 */
public final class Logging {
    static final String FILE_PATTERN = "hopsync%g.log";
    private static final int FILE_LIMIT_BYTES = 1_000_000;
    private static final int FILE_COUNT = 3;

    private Logging() {
    }

    /**
     * Must run before the first log message. Falls back to console-only logging if the folder cannot
     * be written.
     */
    public static void configure(Path logDir) {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        Formatter formatter = new LineFormatter();

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.WARNING);
        console.setFormatter(formatter);
        root.addHandler(console);

        try {
            Files.createDirectories(logDir);
            Handler file = new FileHandler(logDir.resolve(FILE_PATTERN).toString(), FILE_LIMIT_BYTES, FILE_COUNT, true);
            file.setEncoding("UTF-8");
            file.setFormatter(formatter);
            root.addHandler(file);
        } catch (IOException | SecurityException e) {
            root.log(Level.WARNING, "Cannot write logs to " + logDir + ", logging to the console only", e);
        }
    }

    /** One line per message: time, level, logger, message, then the stack trace if any. */
    static final class LineFormatter extends Formatter {
        private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord record) {
            StringBuilder line = new StringBuilder()
                .append(TIME.format(Instant.ofEpochMilli(record.getMillis()))).append(' ')
                .append(record.getLevel().getName()).append(' ')
                .append(shortName(record.getLoggerName())).append(" - ")
                .append(formatMessage(record)).append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter trace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(trace));
                line.append(trace);
            }
            return line.toString();
        }

        private static String shortName(String loggerName) {
            if (loggerName == null) return "-";
            int dot = loggerName.lastIndexOf('.');
            return dot < 0 ? loggerName : loggerName.substring(dot + 1);
        }
    }
}
