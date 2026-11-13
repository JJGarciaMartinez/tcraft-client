package hopsync.desktop.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingTest {

    @TempDir
    Path dir;

    @AfterEach
    void restoreDefaultLogging() throws IOException {
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    void systemLoggerWritesToTheLogFolder() throws IOException {
        Path logs = dir.resolve("logs");
        Logging.configure(logs);

        System.getLogger("hopsync.test").log(System.Logger.Level.INFO, "Sync of pack ñ finished");
        LogManager.getLogManager().reset(); // flushes and closes the file handler

        String content = Files.readString(logs.resolve("hopsync0.log"));
        assertTrue(content.contains("INFO test - Sync of pack ñ finished"), content);
    }
}
