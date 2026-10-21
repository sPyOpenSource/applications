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
     * a threshold that is itself wrong must not be able to hide the fact. Its
     * name is deliberately not a class name, so that it cannot be confused
     * with — or reached through — {@link Log#of(Class)}.</p>
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
        Logger logger = Logger.getLogger("cr0s.javara.util.LogConfig");
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

        // The configuration logger must keep reporting, whatever else is set.
        CONFIG_LOG.setUseParentHandlers(false);
        CONFIG_LOG.setLevel(Level.ALL);

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(LEVEL_PROPERTY_PREFIX)) {
                continue;
            }
            String loggerName = key.substring(LEVEL_PROPERTY_PREFIX.length());
            if (loggerName.isEmpty()) {
                continue;
            }
            Logger target = Logger.getLogger(loggerName);
            if (target == CONFIG_LOG) {
                // An override must not be able to silence the reporting of
                // configuration problems.
                continue;
            }
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
