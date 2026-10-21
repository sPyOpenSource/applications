package test;

import cr0s.javara.util.Log;
import cr0s.javara.util.LogFormatter;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

public class LogTest {

    /** Captures every record published to the logger it is attached to. */
    private static class RecordingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /** A recorder together with the logger it is attached to, so both can be undone. */
    private static class Attachment {
        final Logger logger;
        final RecordingHandler recorder;

        Attachment(Logger logger, RecordingHandler recorder) {
            this.logger = logger;
            this.recorder = recorder;
        }
    }

    /** The recorders attached by the current test, detached again in tearDown. */
    private final List<Attachment> attachments = new ArrayList<>();

    @After
    public void tearDown() {
        for (Attachment attachment : this.attachments) {
            attachment.logger.removeHandler(attachment.recorder);
            attachment.logger.setLevel(null);
        }
        this.attachments.clear();

        System.clearProperty(Log.LEVEL_PROPERTY);
        for (String key : new ArrayList<>(System.getProperties().stringPropertyNames())) {
            if (key.startsWith(Log.LEVEL_PROPERTY_PREFIX)) {
                System.clearProperty(key);
            }
        }
        // Rebuilds the configuration from the (now cleared) properties, which also
        // drops any subtree override applied while the test was running.
        Log.configure();
    }

    /** Attaches a recorder to a fresh logger and returns it. */
    private RecordingHandler record(String loggerName) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(Level.ALL);
        RecordingHandler recorder = new RecordingHandler();
        logger.addHandler(recorder);
        this.attachments.add(new Attachment(logger, recorder));
        return recorder;
    }

    @Test
    public void testLevelMapping() {
        RecordingHandler recorder = record("test.LogTest.levels");
        Log log = Log.of("test.LogTest.levels");

        log.error("e");
        log.warn("w");
        log.info("i");
        log.debug("d");
        log.trace("t");

        assertEquals(5, recorder.records.size());
        assertEquals(Level.SEVERE,  recorder.records.get(0).getLevel());
        assertEquals(Level.WARNING, recorder.records.get(1).getLevel());
        assertEquals(Level.INFO,    recorder.records.get(2).getLevel());
        assertEquals(Level.FINE,    recorder.records.get(3).getLevel());
        assertEquals(Level.FINER,   recorder.records.get(4).getLevel());
    }

    @Test
    public void testOfClassUsesTheFullyQualifiedClassName() {
        RecordingHandler recorder = record(LogTest.class.getName());
        Log log = Log.of(LogTest.class);

        log.info("hello");

        assertEquals(1, recorder.records.size());
        assertEquals("hello", recorder.records.get(0).getMessage());
    }

    @Test
    public void testFormatShape() {
        LogRecord record = new LogRecord(Level.INFO, "Building ore refinery");
        record.setLoggerName("cr0s.javara.ai.BaseBuilder");

        String line = new LogFormatter().format(record);

        assertTrue(line, line.trim().matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} INFO  \\[ai\\] Building ore refinery"));
    }

    @Test
    public void testFormatEndsWithTheLineSeparator() {
        LogRecord record = new LogRecord(Level.INFO, "done");
        record.setLoggerName("cr0s.javara.ai.BaseBuilder");

        String line = new LogFormatter().format(record);

        assertTrue(line, line.endsWith(System.lineSeparator()));
    }

    @Test
    public void testLevelNameIsPaddedToFiveCharacters() {
        assertEquals("ERROR", LogFormatter.levelName(Level.SEVERE));
        assertEquals("WARN ", LogFormatter.levelName(Level.WARNING));
        assertEquals("INFO ", LogFormatter.levelName(Level.INFO));
        assertEquals("DEBUG", LogFormatter.levelName(Level.FINE));
        assertEquals("DEBUG", LogFormatter.levelName(Level.CONFIG));
        assertEquals("TRACE", LogFormatter.levelName(Level.FINER));
        assertEquals("TRACE", LogFormatter.levelName(Level.FINEST));

        for (Level level : Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO,
                                         Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST)) {
            assertEquals(level.getName(), 5, LogFormatter.levelName(level).length());
        }
    }

    @Test
    public void testCategoryDerivation() {
        assertEquals("ai",        LogFormatter.categoryOf("cr0s.javara.ai.BaseBuilder"));
        assertEquals("render",    LogFormatter.categoryOf("cr0s.javara.render.map.TileMap"));
        assertEquals("entity",    LogFormatter.categoryOf("cr0s.javara.entity.actor.activity.activities.Drag"));
        assertEquals("resources", LogFormatter.categoryOf("cr0s.javara.resources.ResourceManager"));
        assertEquals("combat",    LogFormatter.categoryOf("cr0s.javara.combat.Combat"));
        assertEquals("ui",        LogFormatter.categoryOf("cr0s.javara.ui.GameSideBar"));
        assertEquals("Something", LogFormatter.categoryOf("com.example.Something"));
        assertEquals("Something", LogFormatter.categoryOf("Something"));
        assertEquals("unknown",   LogFormatter.categoryOf(null));
        assertEquals("unknown",   LogFormatter.categoryOf(""));
        assertEquals("unknown",   LogFormatter.categoryOf("cr0s.javara."));
    }

    @Test
    public void testThrowableIsRenderedBelowTheMessage() {
        LogRecord record = new LogRecord(Level.SEVERE, "Failed to load mix file");
        record.setLoggerName("cr0s.javara.resources.ResourceManager");
        record.setThrown(new java.io.IOException("boom"));

        String out = new LogFormatter().format(record);

        assertTrue(out, out.contains("ERROR [resources] Failed to load mix file"));
        assertTrue(out, out.contains("java.io.IOException: boom"));
        assertTrue(out, out.contains("LogTest.testThrowableIsRenderedBelowTheMessage"));
    }

    @Test
    public void testLevelFiltering() {
        assertEquals(Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO),
                publishedLevels("test.LogTest.filter.info", Level.INFO));
        assertEquals(Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE),
                publishedLevels("test.LogTest.filter.debug", Level.FINE));
        assertEquals(Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE, Level.FINER),
                publishedLevels("test.LogTest.filter.trace", Level.FINER));
    }

    /** Logs one message per level at the given threshold; returns the levels that got through. */
    private List<Level> publishedLevels(String loggerName, Level threshold) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(threshold);
        RecordingHandler recorder = new RecordingHandler();
        logger.addHandler(recorder);
        this.attachments.add(new Attachment(logger, recorder));

        Log log = Log.of(loggerName);
        log.error("e");
        log.warn("w");
        log.info("i");
        log.debug("d");
        log.trace("t");

        List<Level> published = new ArrayList<>();
        for (LogRecord record : recorder.records) {
            published.add(record.getLevel());
        }
        return published;
    }

    @Test
    public void testPropertyWiringSetsGlobalThreshold() {
        System.setProperty(Log.LEVEL_PROPERTY, "DEBUG");
        Log.configure();

        assertEquals(Level.FINE, Logger.getLogger("").getLevel());
    }

    @Test
    public void testPropertyWiringOverridesSingleSubtree() {
        System.setProperty(Log.LEVEL_PROPERTY, "INFO");
        System.setProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.ai", "TRACE");
        Log.configure();

        assertEquals(Level.INFO,  Logger.getLogger("").getLevel());
        assertEquals(Level.FINER, Logger.getLogger("cr0s.javara.ai").getLevel());
        assertTrue(Logger.getLogger("cr0s.javara.ai.BaseBuilder").isLoggable(Level.FINER));
        assertFalse(Logger.getLogger("cr0s.javara.render").isLoggable(Level.FINE));
    }

    @Test
    public void testSubtreeOverrideSurvivesGarbageCollection() {
        System.setProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.gc", "TRACE");
        Log.configure();

        System.gc();

        assertTrue(Logger.getLogger("cr0s.javara.gc").isLoggable(Level.FINER));
    }

    @Test
    public void testReconfiguringClearsAnOverrideThatIsNoLongerSet() {
        System.setProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.ai", "TRACE");
        Log.configure();
        assertEquals(Level.FINER, Logger.getLogger("cr0s.javara.ai").getLevel());

        System.clearProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.ai");
        Log.configure();

        assertNull(Logger.getLogger("cr0s.javara.ai").getLevel());
    }

    @Test
    public void testUnknownLevelNameFallsBackToInfo() {
        System.setProperty(Log.LEVEL_PROPERTY, "DBG");
        Log.configure();

        assertEquals(Level.INFO, Logger.getLogger("").getLevel());
    }

    @Test
    public void testPropertiesFileIsRead() throws IOException {
        File file = propertiesFile();
        Assume.assumeFalse("a real " + Log.PROPERTIES_FILE + " is present in the working directory",
                file.exists());
        try {
            writeProperties("javara.log.level=DEBUG\n");
            Log.configure();

            assertEquals(Level.FINE, Logger.getLogger("").getLevel());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSystemPropertyWinsOverThePropertiesFile() throws IOException {
        File file = propertiesFile();
        Assume.assumeFalse("a real " + Log.PROPERTIES_FILE + " is present in the working directory",
                file.exists());
        try {
            writeProperties("javara.log.level=DEBUG\n");
            System.setProperty(Log.LEVEL_PROPERTY, "INFO");
            Log.configure();

            assertEquals(Level.INFO, Logger.getLogger("").getLevel());
        } finally {
            file.delete();
        }
    }

    /** The properties file the facade reads: it lives in the working directory. */
    private File propertiesFile() {
        return new File(Log.PROPERTIES_FILE);
    }

    private void writeProperties(String content) throws IOException {
        try (Writer writer = new FileWriter(Log.PROPERTIES_FILE)) {
            writer.write(content);
        }
    }
}
