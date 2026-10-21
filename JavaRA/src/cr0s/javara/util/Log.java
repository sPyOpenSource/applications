package cr0s.javara.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * same configuration. Must run before any {@link Logger} for a JavaRA
     * class is created - JUL fixes a logger's parent at creation time, so a
     * per-subtree override only reaches the class loggers underneath it if the
     * subtree logger already exists. That is why the static initialiser below
     * calls this method before any {@code Log.of(...)} call can run.</p>
     */
    public static void configure() {
        Properties properties = loadProperties();

        Logger root = Logger.getLogger("");
        root.setLevel(levelOf(properties.getProperty(LEVEL_PROPERTY, "INFO")));

        // Drop JUL's default handlers so nothing is printed twice.
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        root.addHandler(handler);

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(LEVEL_PROPERTY_PREFIX)) {
                continue;
            }
            String loggerName = key.substring(LEVEL_PROPERTY_PREFIX.length());
            if (loggerName.isEmpty()) {
                continue;
            }
            Logger.getLogger(loggerName).setLevel(levelOf(properties.getProperty(key)));
        }
    }

    /** Reads the optional properties file, then overlays the system properties. */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
        } catch (IOException e) {
            // No properties file in the working directory - the defaults apply.
        }
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.equals(LEVEL_PROPERTY) || key.startsWith(LEVEL_PROPERTY_PREFIX)) {
                properties.setProperty(key, System.getProperty(key));
            }
        }
        return properties;
    }

    /** Maps a JavaRA level name onto its JUL level. Unknown values fall back to INFO. */
    private static Level levelOf(String value) {
        if (value == null) {
            return Level.INFO;
        }
        switch (value.trim().toUpperCase()) {
            case "ERROR": return Level.SEVERE;
            case "WARN":  return Level.WARNING;
            case "INFO":  return Level.INFO;
            case "DEBUG": return Level.FINE;
            case "TRACE": return Level.FINER;
            default:      return Level.INFO;
        }
    }

    static {
        configure();
    }
}
