package cr0s.javara.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

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
        out.append(TIME_FORMAT.format(LocalTime.ofInstant(record.getInstant(), ZoneId.systemDefault())));
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
