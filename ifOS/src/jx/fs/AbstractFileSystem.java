package jx.fs;

/**
 * Base class for filesystem implementations.
 *
 * <p>Subclasses supply {@code mount()} / {@code unmount()} and the root
 * directory accessors. {@link #close()} delegates to {@link #unmount()}.
 *
 * <p>Implements the legacy {@code FileSystemInterface} so instances still
 * satisfy the old API for consumers.
 */
// implements deprecated FileSystemInterface for backward compatibility
@SuppressWarnings("deprecation")
public abstract class AbstractFileSystem implements FileSystemInterface, AutoCloseable {
    protected final String name;

    /**
     * Creates a filesystem with the given name.
     */
    protected AbstractFileSystem(String name) {
        this.name = name;
    }

    /**
     * Returns the filesystem name.
     */
    public String getName() {
        return name;
    }

    /**
     * Closes the filesystem by unmounting it.
     *
     * <p>Subclasses must make {@code unmount()} safe to call even if
     * {@link #mount()}
     * was never invoked and idempotent for repeated calls, since callers
     * using try-with-resources may close more than once.
     *
     * <p>Declares {@code Exception} (stricter than the usual
     * {@code close()}), matching the legacy {@code FileSystemInterface}.
     */
    public void close() throws Exception {
        unmount();
    }
}
