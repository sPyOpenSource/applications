# Implementation Plan: Unified Logging for JavaRA

**Date:** 2026-10-21
**Spec:** `JavaRA/docs/superpowers/specs/2026-10-21-javara-unified-logging-design.md` (committed as `4c9a588e`)
**Baseline:** the existing suite passes — `OK (76 tests)`.

---

## Goal

Replace every ad-hoc `System.out` / `System.err` / `printStackTrace()` call in
`cr0s.javara` with a single leveled logging facility, so that:

- every line has one format (`HH:mm:ss.SSS LEVEL [category] message`),
- every line has one of five levels (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`),
- the console names the subsystem that produced the line,
- verbosity is a **runtime** decision (`-Djavara.log.level=…`), not a
  recompile.

Success means: the live-call-site check (see *Build recipe*) reports **no live
call sites**, `GUI.DEBUG_MODE` no longer exists, and the test suite still
passes with the new logging tests added.

---

## Architecture

A thin facade over `java.util.logging` (JDK built-in), in two new classes in
the already-exported package `cr0s.javara.util`:

| Class | Responsibility |
|---|---|
| `Log` | Facade. Wraps one `java.util.logging.Logger`. Static `configure()` installs the console handler and applies levels. |
| `LogFormatter` | Renders a `LogRecord` as `HH:mm:ss.SSS LEVEL [category] message`. Owns the level-name mapping and the category derivation. |

Key mechanics:

- **Logger name = full class name.** `Log.of(Combat.class)` → logger
  `cr0s.javara.combat.Combat`. JUL's logger hierarchy then mirrors the package
  tree, which is what makes per-subsystem level control work.
- **Category = first segment after `cr0s.javara.`** (`ai`, `render`, `entity`,
  `resources`, `combat`, `ui`); names outside that hierarchy fall back to their
  last segment.
- **Level mapping:** `ERROR`→`SEVERE`, `WARN`→`WARNING`, `INFO`→`INFO`,
  `DEBUG`→`FINE`, `TRACE`→`FINER`. JUL orders `FINER (400) < FINE (500)`, so a
  `DEBUG` threshold correctly rejects `TRACE` with no extra logic.
- **One `ConsoleHandler` on the root logger** carrying `LogFormatter`. JUL's
  default root handlers are removed first, so nothing is printed twice.
- **Ordering.** JUL creates a logger's ancestor chain on demand and resolves
  the effective level dynamically, so an override reaches the class loggers
  beneath it whether they are created before or after `configure()`. `Log`'s
  static initialiser still runs `configure()` *before* any `Log.of(...)` call
  can create a class logger, so the console handler is in place before any
  JavaRA class can log. Nothing outside `Log` may create a
  `java.util.logging.Logger` directly; always go through `Log`.
- **Retention.** JUL holds loggers weakly, so `Log` keeps a strong reference
  to every logger whose level it sets — otherwise a subtree override would be
  silently reverted by the next garbage collection.

---

## Tech Stack

| | |
|---|---|
| Language | Java 25 (JDK at `/opt/homebrew/opt/openjdk@25/bin`) |
| Logging | `java.util.logging` — no new dependency |
| Module system | JPMS. Module `cr0s.javara.main` (`src/module-info.java`) gains `requires transitive java.logging;` |
| Tests | JUnit 4.12, module `test` (`test/module-info.java`), package `test` |
| Assets | JavaFX SDK 25, snakeyaml 2.4, `assets` module |

---

## Global Constraints

1. **Scope is `cr0s.javara` only.** `src/net/pso/jswarm/**` (vendored
   third-party) and `src/mazesolver/**` (a separate mini-app) stay
   byte-for-byte unchanged. Do not touch them even to "fix" their prints.
2. **Commented-out code is not migrated.** The 19 commented `System.out`/
   `System.err` sites, the **2** commented `printStackTrace()` sites (both in
   `main/GUI.java`), and the 2 commented `DEBUG_MODE` blocks are the only
   remaining hits after this work — they are expected to still be there.
   The one exception is Task 4, which removes the two commented `DEBUG_MODE`
   blocks because they name a field this change deletes.
3. **Do not change any log message beyond what the spec's tables prescribe.**
   The ad-hoc prefixes (`[AI]`, `[SBP]`, `[MAP]`, `ERR:`, `HIRES:`,
   `[Warning]`, `[BUG]`) are dropped: the category column now carries that
   information.
4. **`cr0s.javara.util` is an exported package** and `LogFormatter` extends
   `java.util.logging.Formatter`, which is why the module change uses
   `requires transitive java.logging;` and not plain `requires`.
5. **Every migrated class gets exactly one `private static final Log LOG`**
   field, declared immediately after the class's opening brace, initialised
   with `Log.of(<ThisClass>.class)`.
6. **One commit per task.** Each task ends with a `git add` + `git commit`.

---

## Build recipe

The NetBeans `build.xml` is unusable on this machine (it references
`/home/spy/...` paths and a missing `javafx-sdk-17.0.0.1` — see the spec's
*Aside*). Use direct `javac`/`java` instead.

Set these once per shell:

```bash
cd /Users/xuyi/.local/share/opencode/worktree/25f7f9/silent-river/JavaRA
export JDK=/opt/homebrew/opt/openjdk@25/bin
export MP="/Users/xuyi/opt/javafx-sdk-25.0.2/lib:/Users/xuyi/Source/Java/lib/snakeyaml-2.4.jar:../assets/dist/assets.jar"
export JUNIT=/Users/xuyi/Source/Java/JML/lib/junit-4.12.jar
export HAMCREST=/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar
export JSON=/Users/xuyi/Source/Java/lib/json-20200518.jar
```

Compile both modules:

```bash
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
```

Run the suite:

```bash
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

> **List the test classes explicitly.** zsh does not word-split an unquoted
> variable, so `... JUnitCore $TESTS` passes the whole list as a single
> argument and the runner reports `Could not find class: test.LogTest test.PosTest …`.

Expected on a clean checkout: `OK (76 tests)`.

**Live-call-site check** (distinguishes live code from comments). Run from
`JavaRA/`:

```bash
for f in $(grep -rl "System\.\(out\|err\)\.\|printStackTrace()" --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /System\.(out|err)\./ {printf "PRINT %s:%d\n", F, NR} /printStackTrace\(\)/ {printf "STACK %s:%d\n", F, NR}' "$f"
done
```

Note the **empty parentheses** in `printStackTrace()`. `LogFormatter` calls
`thrown.printStackTrace(writer)` — that is the facility rendering a stack
trace into the log, not a call site to migrate, and the pattern must not
match it.

---

## Validation

This plan was **executed end-to-end in a scratch copy** before it was
committed, and the results are known to be good:

- `Log.java`, `LogFormatter.java` and `LogTest.java` compile and the suite
  reports `OK (94 tests)` (76 pre-existing + 18 new).
- All 50 call sites (31 prints + 19 stack traces) were migrated; the live
  call-site check reports nothing.
- All 17 live `DEBUG_MODE` blocks were deleted, and each block body was
  verified to be **entirely dead** — commented-out Slick render code — so the
  removal is a runtime no-op.
- `src/net/pso/jswarm/**` and `src/mazesolver/**` are byte-for-byte
  identical to their originals.

The validation also produced real output, confirming the format end to end:

```
13:44:13.789 ERROR [resources] Sound file not found: /…/sounds.mix/nonexistent.wav
```

---

## Task 1: The logging facility

**Files:**
- Create `JavaRA/src/cr0s/javara/util/LogFormatter.java`
- Create `JavaRA/src/cr0s/javara/util/Log.java`
- Create `JavaRA/test/test/LogTest.java`
- Modify `JavaRA/src/module-info.java`

**Goal:** a working `Log` / `LogFormatter` pair with a green test suite, wired
into the module graph. Everything after this task is mechanical migration.

### Step 1: Write the failing test

Create `JavaRA/test/test/LogTest.java`:

```java
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
        final Level previousLevel;

        Attachment(Logger logger, RecordingHandler recorder, Level previousLevel) {
            this.logger = logger;
            this.recorder = recorder;
            this.previousLevel = previousLevel;
        }
    }

    /** The recorders attached by the current test, detached again in tearDown. */
    private final List<Attachment> attachments = new ArrayList<>();

    @After
    public void tearDown() {
        for (Attachment attachment : this.attachments) {
            attachment.logger.removeHandler(attachment.recorder);
            attachment.logger.setLevel(attachment.previousLevel);
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
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
        RecordingHandler recorder = new RecordingHandler();
        logger.addHandler(recorder);
        this.attachments.add(new Attachment(logger, recorder, previousLevel));
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
        Level previousLevel = logger.getLevel();
        logger.setLevel(threshold);
        RecordingHandler recorder = new RecordingHandler();
        logger.addHandler(recorder);
        this.attachments.add(new Attachment(logger, recorder, previousLevel));

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
        System.setProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.testreset", "TRACE");
        Log.configure();
        assertEquals(Level.FINER, Logger.getLogger("cr0s.javara.testreset").getLevel());

        System.clearProperty(Log.LEVEL_PROPERTY_PREFIX + "cr0s.javara.testreset");
        Log.configure();

        assertNull(Logger.getLogger("cr0s.javara.testreset").getLevel());
    }

    @Test
    public void testConcurrentConfigureLeavesExactlyOneHandler() throws InterruptedException {
        Log.configure();

        Thread[] workers = new Thread[8];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                for (int round = 0; round < 20; round++) {
                    Log.configure();
                }
            });
        }
        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        assertEquals(1, Logger.getLogger("").getHandlers().length);
    }

    @Test
    public void testUnknownLevelNameFallsBackToInfo() {
        System.setProperty(Log.LEVEL_PROPERTY, "DBG");
        Log.configure();

        assertEquals(Level.INFO, Logger.getLogger("").getLevel());
    }

    @Test
    public void testUnknownLevelNameIsReported() {
        RecordingHandler recorder = record("cr0s.javara.util.Log");

        System.setProperty(Log.LEVEL_PROPERTY, "DBG");
        Log.configure();

        assertEquals(1, recorder.records.size());
        assertEquals(Level.WARNING, recorder.records.get(0).getLevel());
        assertTrue(recorder.records.get(0).getMessage(),
                recorder.records.get(0).getMessage().contains("Unknown log level 'DBG'"));
    }

    @Test
    public void testMalformedPropertiesFileIsReportedAndSurvived() throws IOException {
        File file = propertiesFile();
        Assume.assumeFalse("a real " + Log.PROPERTIES_FILE + " is present in the working directory",
                file.exists());
        RecordingHandler recorder = record("cr0s.javara.util.Log");
        try {
            writeProperties("javara.log.level=bad\\uZZZZ\n");
            Log.configure();

            assertEquals(Level.INFO, Logger.getLogger("").getLevel());
            assertEquals(1, recorder.records.size());
            assertTrue(recorder.records.get(0).getMessage(),
                    recorder.records.get(0).getMessage().contains("Malformed " + Log.PROPERTIES_FILE));
        } finally {
            file.delete();
        }
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
        // The file lives in the working directory; make sure it cannot outlive
        // the run even if the JVM is killed between writing and deleting it.
        propertiesFile().deleteOnExit();
    }
}
```

### Step 2: Run the test to verify it fails

```bash
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
```

Expected: compilation errors — `cannot find symbol: class Log`, `cannot find
symbol: class LogFormatter`.

### Step 3: Write `LogFormatter`

Create `JavaRA/src/cr0s/javara/util/LogFormatter.java`:

```java
package cr0s.javara.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Renders a {@link LogRecord} in the JavaRA console format:
 *
 * <pre>14:32:05.123 ERROR [resources] Mix file tileset.mix is not found</pre>
 *
 * The level name is padded to exactly 5 characters so the category column
 * never shifts between lines.
 */
public class LogFormatter extends Formatter {

    /** Prefix shared by every JavaRA logger name. */
    public static final String LOGGER_NAME_ROOT = "cr0s.javara.";

    /** Category used when a logger name yields nothing usable. */
    public static final String UNKNOWN_CATEGORY = "unknown";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT);

    /** The zone the timestamps are rendered in, captured once at class load. */
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /**
     * Maps a JUL level onto the 5-character JavaRA level name.
     * SEVERE to ERROR, WARNING to WARN, INFO to INFO, CONFIG and FINE to
     * DEBUG, FINER and FINEST to TRACE.
     */
    public static String levelName(Level level) {
        if (level == null) {
            return "INFO ";
        }
        int value = level.intValue();
        if (value >= Level.SEVERE.intValue())  return "ERROR";
        if (value >= Level.WARNING.intValue()) return "WARN ";
        if (value >= Level.INFO.intValue())    return "INFO ";
        if (value >= Level.FINE.intValue())    return "DEBUG";
        if (value >= Level.FINER.intValue())   return "TRACE";
        return "TRACE";
    }

    /**
     * Derives the console category from a logger name: the first segment after
     * {@code cr0s.javara.}, or the last segment for names outside that
     * hierarchy.
     */
    public static String categoryOf(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return UNKNOWN_CATEGORY;
        }

        String name = loggerName;
        if (name.startsWith(LOGGER_NAME_ROOT)) {
            name = name.substring(LOGGER_NAME_ROOT.length());
        } else {
            int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                name = name.substring(lastDot + 1);
            }
        }

        int dot = name.indexOf('.');
        if (dot >= 0) {
            name = name.substring(0, dot);
        }
        return name.isEmpty() ? UNKNOWN_CATEGORY : name;
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder out = new StringBuilder(128);
        out.append(TIME_FORMAT.format(LocalTime.ofInstant(record.getInstant(), ZONE)));
        out.append(' ');
        out.append(levelName(record.getLevel()));
        out.append(" [");
        out.append(categoryOf(record.getLoggerName()));
        out.append("] ");
        out.append(formatMessage(record));
        out.append(System.lineSeparator());

        Throwable thrown = record.getThrown();
        if (thrown != null) {
            StringWriter trace = new StringWriter();
            PrintWriter writer = new PrintWriter(trace);
            thrown.printStackTrace(writer);
            writer.flush();
            out.append(trace.toString());
        }
        return out.toString();
    }
}
```

### Step 4: Write `Log`

Create `JavaRA/src/cr0s/javara/util/Log.java`:

```java
package cr0s.javara.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified logging facade for JavaRA.
 *
 * <p>Use one instance per class:</p>
 *
 * <pre>private static final Log LOG = Log.of(EntityInfantry.class);</pre>
 *
 * <p>The logger name is the full class name, so JUL's logger hierarchy mirrors
 * the package tree and any subtree can be turned up on its own. The console
 * category shown for a line is the first segment after {@code cr0s.javara.}.</p>
 *
 * <p>Verbosity is controlled at runtime:</p>
 * <ul>
 *   <li>{@code -Djavara.log.level=DEBUG} sets the global threshold
 *       (default {@code INFO}).</li>
 *   <li>{@code -Djavara.log.level.cr0s.javara.ai=TRACE} overrides one subtree.</li>
 *   <li>A {@code javara-log.properties} file in the working directory may
 *       supply the same keys. System properties win over the file.</li>
 * </ul>
 */
public final class Log {

    /** Global threshold property, e.g. {@code -Djavara.log.level=DEBUG}. */
    public static final String LEVEL_PROPERTY = "javara.log.level";

    /** Prefix of per-logger overrides, e.g. {@code -Djavara.log.level.cr0s.javara.ai=TRACE}. */
    public static final String LEVEL_PROPERTY_PREFIX = "javara.log.level.";

    /** Optional properties file, read from the working directory. */
    public static final String PROPERTIES_FILE = "javara-log.properties";

    /**
     * Reports problems with the logging configuration itself, such as a level
     * name that is not recognised.
     *
     * <p>It has its own handler and does not use its parent's, so that a
     * configuration problem is reported whatever the configured threshold is:
     * a threshold that is itself wrong must not be able to hide the fact.</p>
     */
    private static final Logger CONFIG_LOG = createConfigLogger();

    /**
     * The loggers whose level this class has set.
     *
     * <p>{@code java.util.logging.LogManager} holds loggers weakly, so a
     * subtree logger that nothing else references can be garbage collected -
     * taking its level with it and silently reverting the override. Holding a
     * strong reference here keeps every applied override alive.</p>
     */
    private static final List<Logger> CONFIGURED_LOGGERS = new ArrayList<>();

    /**
     * Creates the logger used to report configuration problems.
     */
    private static Logger createConfigLogger() {
        Logger logger = Logger.getLogger("cr0s.javara.util.Log");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    private final Logger logger;

    private Log(Logger logger) {
        this.logger = logger;
    }

    /**
     * Creates a logger named after the given class.
     */
    public static Log of(Class<?> type) {
        return of(type.getName());
    }

    /**
     * Creates a logger with an explicit name. Prefer {@link #of(Class)}.
     */
    public static Log of(String name) {
        return new Log(Logger.getLogger(name));
    }

    public void error(String message) {
        this.logger.severe(message);
    }

    public void error(String message, Throwable cause) {
        this.logger.log(Level.SEVERE, message, cause);
    }

    public void warn(String message) {
        this.logger.warning(message);
    }

    public void warn(String message, Throwable cause) {
        this.logger.log(Level.WARNING, message, cause);
    }

    public void info(String message) {
        this.logger.info(message);
    }

    public void debug(String message) {
        this.logger.fine(message);
    }

    public void trace(String message) {
        this.logger.finer(message);
    }

    /**
     * Installs the console handler and applies the configured levels.
     *
     * <p>Idempotent: calling it again re-reads the properties and rebuilds the
     * configuration, including clearing subtree overrides that are no longer
     * configured.</p>
     *
     * <p>JUL creates a logger's ancestor chain on demand and resolves the
     * effective level dynamically, so a per-subtree override reaches the class
     * loggers underneath it whether they are created before or after this
     * call. The static initialiser below still runs this method first, so that
     * the console handler is in place before any JavaRA class can log.</p>
     */
    public static synchronized void configure() {
        Properties properties = loadProperties();

        Logger root = Logger.getLogger("");

        // Replace the root logger's handlers so that nothing is printed twice.
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        root.addHandler(handler);

        // The global threshold.
        root.setLevel(levelOf(properties.getProperty(LEVEL_PROPERTY, "INFO"), LEVEL_PROPERTY));

        // Clear the subtree overrides applied by an earlier configuration.
        for (Logger configured : CONFIGURED_LOGGERS) {
            configured.setLevel(null);
        }
        CONFIGURED_LOGGERS.clear();

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(LEVEL_PROPERTY_PREFIX)) {
                continue;
            }
            String loggerName = key.substring(LEVEL_PROPERTY_PREFIX.length());
            if (loggerName.isEmpty()) {
                continue;
            }
            Logger target = Logger.getLogger(loggerName);
            target.setLevel(levelOf(properties.getProperty(key), key));
            // Keep it reachable: JUL would otherwise let it be collected.
            CONFIGURED_LOGGERS.add(target);
        }
    }

    /** Reads the optional properties file, then overlays the system properties. */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
        } catch (FileNotFoundException e) {
            // No properties file in the working directory - the defaults apply.
        } catch (IOException e) {
            CONFIG_LOG.warning("Cannot read " + PROPERTIES_FILE + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // A malformed escape sequence; the defaults apply.
            CONFIG_LOG.warning("Malformed " + PROPERTIES_FILE + ": " + e.getMessage());
        }
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.equals(LEVEL_PROPERTY) || key.startsWith(LEVEL_PROPERTY_PREFIX)) {
                properties.setProperty(key, System.getProperty(key));
            }
        }
        return properties;
    }

    /**
     * Maps a JavaRA level name onto its JUL level. An unrecognised name is
     * reported and falls back to INFO, so that a typo in a level property is
     * visible instead of silently changing the verbosity.
     */
    private static Level levelOf(String value, String source) {
        if (value == null) {
            return Level.INFO;
        }
        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ERROR": return Level.SEVERE;
            case "WARN":  return Level.WARNING;
            case "INFO":  return Level.INFO;
            case "DEBUG": return Level.FINE;
            case "TRACE": return Level.FINER;
            default:
                CONFIG_LOG.warning("Unknown log level '" + value + "' in " + source + "; using INFO");
                return Level.INFO;
        }
    }

    static {
        configure();
    }
}
```

### Step 5: Wire the module

In `JavaRA/src/module-info.java`, add the `java.logging` requirement next to the
other `requires` lines:

```java
module cr0s.javara.main {
    requires javafx.controls;
    ...
    requires java.sql;
    requires transitive java.logging;
    ...
}
```

`transitive` (not plain `requires`) because `cr0s.javara.util` is exported and
`LogFormatter` extends `java.util.logging.Formatter`: consumers of the module
must be able to read that supertype without declaring `requires java.logging`
themselves. The `test` module already does `requires cr0s.javara.main;` and
needs no change.

### Step 6: Run the test to verify it passes

```bash
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

Expected: `OK (94 tests)` — the original 76 plus the 18 new `LogTest` methods.

### Step 7: Commit

```bash
git add JavaRA/src/cr0s/javara/util/Log.java JavaRA/src/cr0s/javara/util/LogFormatter.java \
        JavaRA/test/test/LogTest.java JavaRA/src/module-info.java
git commit -m "feat: add unified Log facade and LogFormatter for JavaRA"
```

### Step 8: Apply the review fixes

The code review of this task found five real problems. They are fixed in
commit `fd82a993`; the code blocks above already show the corrected code.

1. **JUL holds loggers weakly.** A subtree logger whose level `Log` set could
   be garbage collected, taking its level with it — so
   `-Djavara.log.level.cr0s.javara.ai=TRACE` would silently revert after a GC.
   `Log` now keeps a strong reference to every logger whose level it sets.
2. **`configure()` was not thread-safe.** Two concurrent calls could leave two
   `ConsoleHandler`s on the root logger and print every line twice. It is now
   `synchronized`.
3. **`configure()` could not undo a previous override.** It now resets the
   subtree loggers it configured before applying the current properties, so it
   really does rebuild the configuration.
4. **The Javadoc's ordering rationale was wrong.** JUL creates a logger's
   ancestor chain on demand and resolves the effective level dynamically, so an
   override reaches class loggers created before *or* after `configure()`.
5. **An unrecognised level name was swallowed.** It is now reported on the log
   and falls back to `INFO`, so a typo in `-Djavara.log.level` is visible.

Two smaller changes rode along: the timestamp zone is cached instead of being
recomputed per line, and the timestamp format is pinned to `Locale.ROOT` so
that the digits do not follow the platform locale.

### Step 9: Apply the second review round

The re-review confirmed all five fixes and found four more. They are fixed in
commit `d4e01f82`; the code blocks above already show the corrected code.

1. **A configuration problem could be hidden by the threshold it reported
   on.** The warning about an unrecognised level name went through the root
   logger, so `-Djavara.log.level=ERROR` filtered it out — exactly the
   "silently swallowed" case the fix was meant to remove. The configuration
   logger now has its own `ConsoleHandler`, sets
   `setUseParentHandlers(false)` and pins its level to `ALL`, so a
   configuration problem is reported whatever the threshold is.
2. **A malformed properties file broke `Log` entirely.** `Properties.load`
   throws `IllegalArgumentException` for a bad `\u` escape, which escaped
   `Log`'s static initialiser and turned every later use into an
   `ExceptionInInitializerError`. The exception is now caught and reported,
   and the defaults apply.
3. **Two regressions were unguarded.** `LogTest` now asserts that concurrent
   `configure()` calls leave exactly one handler on the root logger, and that
   the unknown-level warning is actually published.
4. **A stray `javara-log.properties` could survive a killed test run** and
   silently change the logging of every later run. The test now registers the
   file for deletion on exit, the file summary lists it in `.gitignore`, and
   the tests that would be affected by a leftover file use a logger name that
   no realistic configuration would mention.

`LogTest` grew from 8 to 18 methods: the recording handlers are now detached
again in `tearDown`, every level property is cleared there, and the missing
behaviour is covered — `of(Class)`, the properties file, file-vs-system-property
precedence, an unknown level name, the trailing line separator, reconfiguring,
a subtree override surviving a GC, concurrent configuration, and a malformed
properties file.

---

## Task 2: Migrate the 31 `System.out` / `System.err` call sites

**Files:** 22 files under `JavaRA/src/cr0s/javara/`.

**Goal:** every live `System.out` / `System.err` call in `cr0s.javara` becomes
a `Log` call at the level the spec prescribes.

### Step 1: Confirm the starting inventory

```bash
for f in $(grep -rl "System\.\(out\|err\)\." --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /System\.(out|err)\./ {printf "%s:%d\n", F, NR}' "$f"
done
```

Expected: exactly 31 lines.

### Step 2: Add the `Log` field and import to each file

For each of the 22 files below:

1. Add `import cr0s.javara.util.Log;` to the import block. Place it in the
   `cr0s.javara.*` group, in alphabetical order among the `cr0s.javara.util.*`
   imports when those exist, otherwise immediately after the last
   `cr0s.javara.*` import.
2. Add the logger field immediately after the class's opening brace:

   ```java
   private static final Log LOG = Log.of(<ClassName>.class);
   ```

| File | Class | `Log.of(...)` argument |
|---|---|---|
| `ui/GameSideBar.java` | `GameSideBar` | `GameSideBar.class` |
| `ui/sbpages/SideBarPage.java` | `SideBarPage` | `SideBarPage.class` |
| `ui/cursor/CursorManager.java` | `CursorManager` | `CursorManager.class` |
| `combat/Combat.java` | `Combat` | `Combat.class` |
| `entity/infantry/EntityInfantry.java` | `EntityInfantry` | `EntityInfantry.class` |
| `entity/effect/TeslaZapRenderer.java` | `TeslaZapRenderer` | `TeslaZapRenderer.class` |
| `entity/actor/activity/activities/Drag.java` | `Drag` | `Drag.class` |
| `entity/building/common/EntityWall.java` | `EntityWall` | `EntityWall.class` |
| `entity/vehicle/EntityVehicle.java` | `EntityVehicle` | `EntityVehicle.class` |
| `resources/RemappedTextureCache.java` | `RemappedTextureCache` | `RemappedTextureCache.class` |
| `resources/ResourceManager.java` | `ResourceManager` | `ResourceManager.class` |
| `resources/SoundManager.java` | `SoundManager` | `SoundManager.class` |
| `render/World.java` | `World` | `World.class` |
| `render/EntityBlockingMap.java` | `EntityBlockingMap` | `EntityBlockingMap.class` |
| `render/map/TileMap.java` | `TileMap` | `TileMap.class` |
| `render/map/TileSet.java` | `TileSet` | `TileSet.class` |
| `render/map/TheaterCache.java` | `TheaterCache` | `TheaterCache.class` |
| `render/map/MinimapRenderer.java` | `MinimapRenderer` | `MinimapRenderer.class` |
| `ai/BaseBuilder.java` | `BaseBuilder` | `BaseBuilder.class` |
| `ai/AIPlayer.java` | `AIPlayer` | `AIPlayer.class` |
| `main/StateMainMenu.java` | `StateMainMenu` | `StateMainMenu.class` |
| `main/StatePauseMenu.java` | `StatePauseMenu` | `StatePauseMenu.class` |

### Step 3: Replace the 31 call sites

Each row is one edit. **The `Before` text is the anchor** — it is unique
within its file. The line number is only a hint taken from the file as it
stands before this task; Step 2 has already inserted an import and a `LOG`
field into each of these files, which shifts every line below the insertion
point down by two. Locate each site by its `Before` text, never by line
number.

| File:line | Before | After | Level |
|---|---|---|---|
| `ui/GameSideBar.java:221` | `System.out.println(event);` | `LOG.trace("Mouse event: " + event);` | TRACE |
| `ui/GameSideBar.java:540` | `System.out.println("[" + barX + "; " + barY + "] Button-" + button + " clicked: " + buttonX + " " + buttonY);` | `LOG.debug("Button " + button + " clicked at " + buttonX + "," + buttonY);` | DEBUG |
| `ui/sbpages/SideBarPage.java:67` | `System.out.println("[SBP] Button pos: " + x + "; " + y);` | `LOG.trace("Button click at " + x + "," + y);` | TRACE |
| `ui/sbpages/SideBarPage.java:72` | `System.out.println("[SBP] Found button: " + btn.getDescription());` | `LOG.debug("Found button: " + btn.getDescription());` | DEBUG |
| `combat/Combat.java:103` | `System.err.println("[BUG] Weapon " + weapon.getClass().getName() + " trying to impact without warheads");` | `LOG.error("Weapon " + weapon.getClass().getName() + " trying to impact without warheads");` | ERROR |
| `entity/infantry/EntityInfantry.java:184` | `System.out.println("Attacking");` | `LOG.trace("Attacking");` | TRACE |
| `entity/effect/TeslaZapRenderer.java:147` | `System.err.println("Sprite limit!");` | `LOG.error("Sprite limit reached");` | ERROR |
| `entity/actor/activity/activities/Drag.java:25` | `System.out.println("Drag is cancelled.");` | `LOG.debug("Drag is cancelled");` | DEBUG |
| `entity/building/common/EntityWall.java:41` | `System.err.println("Wall texture not found: " + this.textureName);` | `LOG.error("Wall texture not found: " + this.textureName);` | ERROR |
| `resources/RemappedTextureCache.java:65` | `System.err.println("[Warning] Remapped textures cache warning: trying to put in cache image that already cached.");` | `LOG.warn("Remapped texture cache: image already cached");` | WARN |
| `resources/ResourceManager.java:222` | `System.err.println("Record SHP (" + name +") in " + tileSetName + ".mix is not found");` | `LOG.error("Record SHP (" + name + ") in " + tileSetName + ".mix is not found");` | ERROR |
| `resources/ResourceManager.java:226` | `System.err.println("Mix file " + tileSetName + ".mix is not found");` | `LOG.error("Mix file " + tileSetName + ".mix is not found");` | ERROR |
| `resources/ResourceManager.java:258` | `System.out.println(type + ".mix is not found");` | `LOG.error("Template mix file " + type + ".mix is not found");` | ERROR |
| `resources/ResourceManager.java:322` | `System.out.println("HIRES: " + name + " not found");` | `LOG.warn("Hi-res texture " + name + " not found");` | WARN |
| `resources/SoundManager.java:73` | `System.err.println("Sound file not found: " + filePath);` | `LOG.error("Sound file not found: " + filePath);` | ERROR |
| `render/World.java:306` | `System.out.println(e.getName());` | `LOG.debug("Spawned entity " + e.getName());` | DEBUG |
| `render/map/TileMap.java:119` | `System.err.println("Invalid map.");` | `LOG.error("Invalid map");` | ERROR |
| `render/map/TileMap.java:135` | `System.out.println("Map size: " + this.width + " x " + this.height);` | `LOG.info("Map size: " + this.width + " x " + this.height);` | INFO |
| `render/map/TileMap.java:275` | `System.out.println(t.getTextureName());` | `LOG.trace("Map entity texture: " + t.getTextureName());` | TRACE |
| `render/map/TileSet.java:58` | `System.out.println("Loaded tileSet: " + ResourceManager.TILESETS_FOLDER + (aSetName + ".yaml").toLowerCase());` | `LOG.info("Loaded tileSet: " + ResourceManager.TILESETS_FOLDER + (aSetName + ".yaml").toLowerCase());` | INFO |
| `render/map/TheaterCache.java:195` | `System.out.println("Unable to add: " + texture.getTextureName());` | `LOG.error("Unable to add texture: " + texture.getTextureName());` | ERROR |
| `render/EntityBlockingMap.java:135` | `System.out.println("ERR: null subcell!");` | `LOG.error("Null subcell");` | ERROR |
| `ai/BaseBuilder.java:74` | `System.err.println("[AI] Cannot find common names for: " + commonName);` | `LOG.warn("Cannot find common names for: " + commonName);` | WARN |
| `ai/BaseBuilder.java:110` | `System.out.println("[AI] Checking building limit for " + building + ": " + this.ai.countBuildings(building, this.ai) + " / " + limit);` | `LOG.debug("Checking building limit for " + building + ": " + this.ai.countBuildings(building, this.ai) + " / " + limit);` | DEBUG |
| `ai/BaseBuilder.java:124` | `System.out.println("[AI] Building power plant: " + power.getName());` | `LOG.info("Building power plant: " + power.getName());` | INFO |
| `ai/BaseBuilder.java:133` | `System.out.println("[AI] Building ore refinery");` | `LOG.info("Building ore refinery");` | INFO |
| `ai/BaseBuilder.java:143` | `System.out.println("[AI] Building production: " + production.getName());` | `LOG.info("Building production: " + production.getName());` | INFO |
| `ai/BaseBuilder.java:153` | `System.out.println("[AI] Building ore silo");` | `LOG.info("Building ore silo");` | INFO |
| `ai/BaseBuilder.java:182` | `System.out.println("[AI] Building power plant: " + power.getName());` | `LOG.info("Building power plant: " + power.getName());` | INFO |
| `ai/BaseBuilder.java:188` | `System.out.println("[AI] Building: " + a.getName());` | `LOG.info("Building: " + a.getName());` | INFO |
| `ai/AIPlayer.java:809` | `System.out.println(base.getCash());` | `LOG.debug("Cash: " + base.getCash());` | DEBUG |

### Step 4: Verify

```bash
# no live prints left
for f in $(grep -rl "System\.\(out\|err\)\." --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /System\.(out|err)\./ {printf "%s:%d\n", F, NR}' "$f"
done
```

Expected: no output.

```bash
# still compiles, still green
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

Expected: `OK (94 tests)`.

### Step 5: Commit

```bash
git add -A JavaRA/src/cr0s/javara
git commit -m "refactor: migrate System.out/System.err call sites to the Log facade"
```

---

## Task 3: Migrate the 19 `printStackTrace()` call sites

**Files:** the 22 files from Task 2, minus those with no `printStackTrace`
(`ui/sbpages/SideBarPage.java`, `entity/effect/TeslaZapRenderer.java`,
`entity/actor/activity/activities/Drag.java`, `entity/building/common/EntityWall.java`,
`resources/RemappedTextureCache.java`, `render/EntityBlockingMap.java`,
`render/map/TileSet.java`, `ai/BaseBuilder.java`).

**Goal:** every live `e.printStackTrace()` becomes a labelled `ERROR` with the
exception attached, so the stack trace is preserved but no longer anonymous.

### Step 1: Confirm the starting inventory

```bash
for f in $(grep -rl "printStackTrace()" --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /printStackTrace\(\)/ {printf "%s:%d\n", F, NR}' "$f"
done
```

Expected: exactly 19 lines. (The two commented sites in `main/GUI.java` are
filtered out by the comment handling; `LogFormatter`'s
`printStackTrace(writer)` does not match `printStackTrace()`.)

### Step 2: Replace the 19 call sites

The `Log` field and import are already in place from Task 2. Each row is one
edit. **The `Before` text is the anchor** — locate each site by its `Before`
text, not by the line number, which is a hint taken from the file before
Tasks 2 and 3 shifted it.

| File:line | Before | After |
|---|---|---|
| `ui/cursor/CursorManager.java:111` | `e.printStackTrace();` | `LOG.error("Failed to update cursor", e);` |
| `ui/GameSideBar.java:99` | `e.printStackTrace();` | `LOG.error("Failed to load sidebar button sheet", e);` |
| `entity/vehicle/EntityVehicle.java:86` | `e.printStackTrace();` | `LOG.error("Failed to instantiate vehicle " + b.getClass().getSimpleName(), e);` |
| `resources/ResourceManager.java:103` | `e.printStackTrace();` | `LOG.error("Failed to load mix file " + path, e);` |
| `resources/ResourceManager.java:184` | `e.printStackTrace();` | `LOG.error("Failed to decode sound " + name, e);` |
| `resources/ResourceManager.java:274` | `e.printStackTrace();` | `LOG.error("Failed to load palette " + name, e);` |
| `resources/ResourceManager.java:350` | `e.printStackTrace();` | `LOG.error("Failed to load texture " + name, e);` |
| `resources/ResourceManager.java:357` | `e.printStackTrace();` | `LOG.error("Failed to close file for " + name, e);` |
| `render/map/TileMap.java:170` | `e.printStackTrace();` | `LOG.error("Failed to load map resources", e);` |
| `render/map/TheaterCache.java:74` | `e1.printStackTrace();` | `LOG.error("Failed to create sprite sheet", e1);` |
| `render/map/TheaterCache.java:176` | `e.printStackTrace();` | `LOG.error("Failed to copy texture into sprite sheet", e);` |
| `render/map/TheaterCache.java:226` | `e.printStackTrace();` | `LOG.error("Failed to copy texture into sprite sheet", e);` |
| `render/map/TheaterCache.java:263` | `e.printStackTrace();` | `LOG.error("Failed to copy texture into sprite sheet", e);` |
| `render/map/MinimapRenderer.java:36` | `e.printStackTrace();` | `LOG.error("Failed to initialise minimap", e);` |
| `render/map/MinimapRenderer.java:105` | `e1.printStackTrace();` | `LOG.error("Failed to render minimap", e1);` |
| `ai/AIPlayer.java:224` | `e.printStackTrace();` | `LOG.error("Failed to load AI configuration", e);` |
| `ai/AIPlayer.java:230` | `e.printStackTrace();` | `LOG.error("Failed to load AI configuration", e);` |
| `main/StateMainMenu.java:120` | `e.printStackTrace();` | `LOG.error("Failed to load main menu textures", e);` |
| `main/StatePauseMenu.java:127` | `e.printStackTrace();` | `LOG.error("Failed to load pause menu textures", e);` |

Notes on the variable names used above (all verified against the source):

- `EntityVehicle.java:86` — the caught variable is `e`; `b` is the template
  vehicle being copied. The spec's `{name}` placeholder is filled with
  `b.getClass().getSimpleName()` because `Entity.getName()` is a stub that
  returns the literal `"don't know"`. The class name is the useful identity
  here (`EntityMcv`, `EntityHarvester`, …).
- `ResourceManager.java:103` — the caught variable is `e`; `path` is the
  `String` parameter of `loadMixes(String path)`.
- `ResourceManager.java:184` — `name` is the `String` parameter.
- `ResourceManager.java:274` and `:350` — `name` is the `String` parameter.
- `ResourceManager.java:357` — `name` is the `String` parameter of the
  enclosing method.
- `TheaterCache.java:74` and `MinimapRenderer.java:105` catch `Exception e1`,
  not `e` — keep the original variable name.
- `AIPlayer.java:230` sits in a `finally` block closing the config file. The
  spec assigns it the same message as `:224` ("Failed to load AI configuration");
  it is kept verbatim here so the implementation matches the approved spec.

### Step 3: Verify

```bash
for f in $(grep -rl "printStackTrace()" --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /printStackTrace\(\)/ {printf "%s:%d\n", F, NR}' "$f"
done
```

Expected: no output.

```bash
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

Expected: `OK (94 tests)`.

### Step 4: Commit

```bash
git add -A JavaRA/src/cr0s/javara
git commit -m "refactor: replace printStackTrace with labelled Log.error calls"
```

---

## Task 4: Retire `GUI.DEBUG_MODE`

**Files:**
- Modify `JavaRA/src/cr0s/javara/main/GUI.java` — delete the field
- Modify 17 files — delete the live `if (GUI.DEBUG_MODE) { … }` blocks
- Modify `JavaRA/src/cr0s/javara/render/World.java` and
  `JavaRA/src/cr0s/javara/entity/building/common/EntityConstructionYard.java` —
  delete the commented-out blocks that name the removed field
- Modify `JavaRA/src/cr0s/javara/entity/MobileEntity.java` — delete
  `drawPath(Scene g)` and the imports it was the only user of
- Modify 6 files — delete the stale `//drawPath(g);` comments
- Modify 16 files — remove the now-unused `import cr0s.javara.main.GUI;`

**Goal:** no compile-time verbosity switch survives; verbosity is a runtime
decision only.

### Step 1: Delete the field

In `JavaRA/src/cr0s/javara/main/GUI.java`, delete the line:

```java
    public static boolean DEBUG_MODE = false;
```

### Step 2: Delete the 17 live `if (GUI.DEBUG_MODE) { … }` blocks

Every one of these blocks has a body that is entirely commented-out Slick
render code, so removing it is a runtime no-op.

> **Locate the blocks by content, not by line number.** Tasks 2 and 3 inserted
> an import and a `LOG` field into several of these files, which shifts every
> line below the insertion point down by two. Each of the 17 files contains
> exactly **one** live `if (GUI.DEBUG_MODE) {` — find it, then delete from
> that line through its matching `}`. (`entity/MobileEntity.java` is handled
> by Step 3 instead.)

The 17 files:

| File | Block shape |
|---|---|
| `entity/vehicle/common/EntityMcv.java` | line comments |
| `entity/vehicle/common/EntityHarvester.java` | line comments |
| `entity/vehicle/soviet/EntityMammothTank.java` | line comments |
| `entity/vehicle/soviet/EntityHeavyTank.java` | line comments |
| `entity/vehicle/soviet/EntityV2Launcher.java` | block comment |
| `entity/MobileEntity.java` | deleted wholesale in Step 3 |
| `entity/building/common/EntityProc.java` | line comments |
| `entity/building/common/EntityWarFactory.java` | line comments |
| `entity/building/common/EntityOreSilo.java` | line comments |
| `entity/building/common/EntityAdvPowerPlant.java` | block comment |
| `entity/building/common/EntityHelipad.java` | line comments |
| `entity/building/common/EntityRadarDome.java` | line comments |
| `entity/building/common/EntityPowerPlant.java` | block comment |
| `entity/building/soviet/EntityBarracks.java` | line comments |
| `entity/building/soviet/EntityFireTurret.java` | line comments |
| `entity/building/soviet/EntityAirField.java` | line comments |
| `entity/building/soviet/EntityTeslaCoil.java` | line comments |

Two shapes occur. The common one (e.g. `EntityMcv.java`):

```java
	if (GUI.DEBUG_MODE) {
	    //g.setLineWidth(1);
	    //g.setColor(owner.playerColor);
	    //g.draw(boundingBox);
	    //g.drawOval(posX - 1, posY - 1, this.boundingBox.getWidth() + 1, this.boundingBox.getHeight() + 1);
	}
```

The other (e.g. `EntityPowerPlant.java`), where the body is a single block
comment:

```java
	// Draw bounding box if debug mode is on
	if (GUI.DEBUG_MODE) {
	    /*g.setLineWidth(2);
	    g.setColor(owner.playerColor);
	    g.draw(boundingBox);
	    g.setLineWidth(1);*/
	}
```

Delete the whole `if (…) { … }` block in both shapes. When the block is
preceded by the comment line `// Draw bounding box if debug mode is on`,
delete that comment line too — it refers to a switch that no longer exists.

**Matching the closing brace.** Brace matching must ignore braces inside
comments: `MobileEntity.drawPath` contains a commented-out `/*final int
GRID_SIZE = 3; … }*/` whose `}` would otherwise close the block early. For
the 16 blocks in this step, the body is comments only, so the `}` on the line
immediately after the body is the match — but read it, do not count blindly.

### Step 3: Delete `MobileEntity.drawPath(Scene g)`

In `JavaRA/src/cr0s/javara/entity/MobileEntity.java`, delete the whole method
`protected void drawPath(Scene g)` — from its signature through its closing
brace, including the commented-out `// Draw grid` block at the end of it. It
has zero live callers.

The method's closing brace is the `}` that follows the commented-out
`//g.setColor(Color.orange);` / `//g.fillOval(...)` lines. The commented
`/*final int GRID_SIZE = 3; … }*/` block inside the method contains a `}`
that does **not** close the method — do not stop there.

Three imports in that file become unused as a result and must be removed:

```java
import cr0s.javara.main.GUI;
import cr0s.javara.entity.actor.activity.activities.MoveInfantry;
import javafx.scene.Scene;
```

**Keep** `import javafx.scene.shape.MoveTo;` — it is still used by
`startMovingByPath` (the `((MoveTo)p.getElements().get(...)).getX()` lines),
which lives outside `drawPath`.

**Keep** `import javafx.scene.shape.Path;` — still used by `findPathFromTo`
and `startMovingByPath`. **Keep**
`import cr0s.javara.entity.actor.activity.activities.Move;` — still used by
`moveTo`.

### Step 4: Delete the stale `//drawPath(g);` comments

Remove the single commented call line from each of:

- `entity/vehicle/common/EntityMcv.java`
- `entity/vehicle/common/EntityHarvester.java`
- `entity/vehicle/soviet/EntityMammothTank.java`
- `entity/vehicle/soviet/EntityHeavyTank.java`
- `entity/vehicle/soviet/EntityV2Launcher.java`
- `entity/infantry/EntityInfantry.java`

Each contains exactly one line whose trimmed content is `//drawPath(g);`.
(`entity/infantry/EntityInfantry.java` does not import `GUI`, so no import
change is needed there.)

### Step 5: Delete the two commented-out `DEBUG_MODE` blocks

These two references live inside `/* … */` block comments and name a field
that no longer exists. Remove the `DEBUG_MODE` part only, and **keep the
enclosing comment structurally valid** — the comment opener and closer must
still balance.

**`render/World.java`** — the `if (GUI.DEBUG_MODE) {` sits inside a block
comment that starts earlier in the file and ends with a `}*/` line. Edit it as
follows:

1. Delete the `// Debug: render blocked cells` comment line.
2. Delete the `if (GUI.DEBUG_MODE) {` line and its body through the `}` that
   closes it.
3. Change the closing `}*/` line to just `*/`.

The result is a comment that closes cleanly and no longer mentions
`DEBUG_MODE`.

**`entity/building/common/EntityConstructionYard.java`** — the
`if (GUI.DEBUG_MODE) {` is *inside* a block comment whose opener is on the
same line. Edit it as follows:

1. Delete the `// Draw bounding box if debug mode is on` comment line above
   it.
2. Change the `/*if (GUI.DEBUG_MODE) {` line to just `/*`.
3. Delete the block body through the `}` that closes it.

The rest of that comment — a dead `if (this.isSelected) { … }` block and a
dead "Render repairing wrench" block — stays commented out, exactly as it is
today.

Neither file imports `cr0s.javara.main.GUI`, so no import changes here.

### Step 6: Remove the 16 now-unused `import cr0s.javara.main.GUI;` lines

These files referenced `GUI` only through `DEBUG_MODE`:

- `entity/vehicle/common/EntityMcv.java`
- `entity/vehicle/common/EntityHarvester.java`
- `entity/vehicle/soviet/EntityMammothTank.java`
- `entity/vehicle/soviet/EntityHeavyTank.java`
- `entity/vehicle/soviet/EntityV2Launcher.java`
- `entity/MobileEntity.java`
- `entity/building/common/EntityProc.java`
- `entity/building/common/EntityWarFactory.java`
- `entity/building/common/EntityOreSilo.java`
- `entity/building/common/EntityAdvPowerPlant.java`
- `entity/building/common/EntityHelipad.java`
- `entity/building/common/EntityPowerPlant.java`
- `entity/building/soviet/EntityBarracks.java`
- `entity/building/soviet/EntityFireTurret.java`
- `entity/building/soviet/EntityAirField.java`
- `entity/building/soviet/EntityTeslaCoil.java`

**Keep** the import in `entity/building/common/EntityRadarDome.java` — it uses
`GUI` for other members.

### Step 7: Verify

```bash
grep -rn "DEBUG_MODE" --include="*.java" src/cr0s/
```

Expected: no output.

```bash
grep -rn "drawPath" --include="*.java" src/cr0s/
```

Expected: no output.

```bash
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

Expected: `OK (94 tests)`.

### Step 8: Commit

```bash
git add -A JavaRA/src/cr0s/javara
git commit -m "refactor: retire the GUI.DEBUG_MODE compile-time switch"
```

---

## Task 5: Document the runtime knob

**Files:** modify `JavaRA/README.md`.

**Goal:** a developer can discover `-Djavara.log.level` without reading the
source.

### Step 1: Add the section

Append to `JavaRA/README.md`:

```markdown
## Debug output

All diagnostic output goes through one leveled logger. Every line has the
same shape:

```
14:32:05.123 ERROR [resources] Mix file tileset.mix is not found
14:32:05.130 INFO  [render] Map size: 64 x 64
14:32:05.131 DEBUG [ai] Building power plant: PowerPlant
```

Levels are `ERROR`, `WARN`, `INFO`, `DEBUG` and `TRACE`. The default
threshold is `INFO`. Raise or lower it at runtime, without recompiling:

```
java -Djavara.log.level=DEBUG -cp <classpath> cr0s.javara.main.GUI
```

Any subtree can be turned up on its own - the suffix is a logger name (the
full class name), and the setting applies to everything below it:

```
java -Djavara.log.level=DEBUG -Djavara.log.level.cr0s.javara.ai=TRACE -cp <classpath> cr0s.javara.main.GUI
```

The same keys can be put in a `javara-log.properties` file next to the
application. System properties win over the file.

Log output is written to `stderr`.
```

### Step 2: Verify

```bash
git diff --stat JavaRA/README.md
```

Expected: one file changed, with the new section appended.

### Step 3: Commit

```bash
git add JavaRA/README.md
git commit -m "docs: document the javara.log.level runtime knob"
```

---

## Manual smoke test

Run after Task 5 (the game needs a display; do this on a machine with a
screen, or via the project's own launcher).

| Invocation | Expected |
|---|---|
| default | Errors, warnings and lifecycle info only. No per-tick noise. |
| `-Djavara.log.level=DEBUG` | AI decisions, entity spawns, button interactions appear. |
| `-Djavara.log.level=TRACE` | Per-frame state traces appear (`Attacking`, `Mouse event: …`). |
| `-Djavara.log.level=DEBUG -Djavara.log.level.cr0s.javara.ai=TRACE` | The AI subtree is at TRACE; `render`, `ui` and the rest stay at DEBUG. |
| default | Every line matches `HH:mm:ss.SSS LEVEL [category] message`, the level column is 5 characters wide, and **no line appears twice**. |

The "no line appears twice" check is the one that catches a duplicated
handler; if a line is printed twice, `Log.configure()` did not remove JUL's
default root handlers.

---

## Final verification

After all five tasks:

```bash
cd JavaRA
# no live output statements left
for f in $(grep -rl "System\.\(out\|err\)\.\|printStackTrace()" --include="*.java" src/cr0s); do
  awk -v F="$f" '/^[[:space:]]*\/\// {next} /\/\*/ {inblock=1} inblock && /\*\// {inblock=0; next} inblock {next} /System\.(out|err)\./ {printf "PRINT %s:%d\n", F, NR} /printStackTrace\(\)/ {printf "STACK %s:%d\n", F, NR}' "$f"
done
```

Expected: no output.

```bash
# the migration is complete: 50 LOG.* call sites, one LOG field per migrated file
grep -rn "LOG\.\(error\|warn\|info\|debug\|trace\)" --include="*.java" src/cr0s | wc -l
grep -rn "private static final Log LOG" --include="*.java" src/cr0s | wc -l
```

Expected: `50` and `23`. The second count is 23 rather than 22 because
`Log.java`'s own Javadoc contains the example line
`private static final Log LOG = Log.of(EntityInfantry.class);`.

```bash
grep -rn "DEBUG_MODE\|drawPath" --include="*.java" src/cr0s/
```

Expected: no output.

```bash
git status --short JavaRA/src/net/pso/jswarm JavaRA/src/mazesolver
```

Expected: no output — the vendored library and the separate mini-app are
untouched.

```bash
rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

Expected: `OK (94 tests)`.

---

## File summary

**Added**

- `JavaRA/src/cr0s/javara/util/Log.java`
- `JavaRA/src/cr0s/javara/util/LogFormatter.java`
- `JavaRA/test/test/LogTest.java`

**Modified**

- `JavaRA/src/module-info.java` — `requires transitive java.logging;`
- `.gitignore` — ignore `javara-log.properties`, the optional runtime
  configuration the facade reads from the working directory
- `JavaRA/README.md` — "Debug output" section
- 22 files — added `import cr0s.javara.util.Log;` and a `LOG` field
- 22 files — migrated 31 `System.out`/`System.err` and 19 `printStackTrace()` sites
- `JavaRA/src/cr0s/javara/main/GUI.java` — deleted `DEBUG_MODE`
- 17 files — deleted live `if (GUI.DEBUG_MODE)` blocks
- 2 files — deleted commented `DEBUG_MODE` blocks
- `JavaRA/src/cr0s/javara/entity/MobileEntity.java` — deleted `drawPath` + 3 unused imports (`GUI`, `MoveInfantry`, `Scene`; `MoveTo` stays)
- 6 files — deleted stale `//drawPath(g);` comments
- 16 files — removed the unused `import cr0s.javara.main.GUI;`

**Untouched**

- `JavaRA/src/net/pso/jswarm/**`
- `JavaRA/src/mazesolver/**`
- `JavaRA/nbproject/project.properties` (broken machine-specific paths — recorded as an aside in the spec, deliberately out of scope)

---

## Notes on the spec

Four points where the implementation needs a judgement call. Each is
recorded here so the decision is visible rather than silent.

1. **Category derivation (§1.1 vs §1.3).** §1.1 says the category is
   "derived once at construction"; §1.3 says the derivation "lives in one
   place (`LogFormatter`)". Implemented as a single public static method
   `LogFormatter.categoryOf(String)`, used by the formatter. A JUL logger
   name never changes, so the derived category is constant per logger — the
   two statements describe the same result.
2. **`GameSideBar.java:540`.** The spec's message template
   (`Button {button} clicked at {x},{y}`) does not carry the old
   `barX`/`barY` values, so those are dropped. They were the button's
   position in the sidebar rather than the click coordinates.
3. **`AIPlayer.java:230`.** The spec gives `Failed to load AI configuration`
   for both AIPlayer stack-trace sites. Line 230 is in a `finally` block that
   closes the config file, so the wording is slightly off; it is kept
   verbatim to match the approved spec.
4. **`EntityVehicle.java:86`.** The spec's `{name}` placeholder is filled
   with `b.getClass().getSimpleName()` rather than `b.getName()`, because
   `Entity.getName()` is a stub returning the literal `"don't know"`.
