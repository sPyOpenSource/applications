package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    public enum Level { OFF, ERROR, WARN, INFO, TRACE }

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static volatile Level level =
            parse(System.getProperty("coc.debug", "TRACE"));

    private DebugLog() {}

    public static void setLevel(Level newLevel) {
        level = newLevel;
    }

    public static Level getLevel() {
        return level;
    }

    private static Level parse(String value) {
        try {
            return Level.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return Level.TRACE;
        }
    }

    private static boolean enabled(Level threshold) {
        return level != Level.OFF && threshold.ordinal() <= level.ordinal();
    }

    private static String prefix(String category) {
        return "[" + LocalDateTime.now().format(TS) + "] ["
                + Thread.currentThread().getName() + "] [" + category + "]";
    }

    public static void error(String category, String message, Throwable throwable) {
        if (!enabled(Level.ERROR)) return;
        System.err.println(prefix(category) + " ERROR " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    public static void warn(String category, String message) {
        if (!enabled(Level.WARN)) return;
        System.out.println(prefix(category) + " WARN " + message);
    }

    public static void info(String category, String message) {
        if (!enabled(Level.INFO)) return;
        System.out.println(prefix(category) + " INFO " + message);
    }

    public static void trace(String category, String message) {
        if (!enabled(Level.TRACE)) return;
        System.out.println(prefix(category) + " TRACE " + message);
    }
}
