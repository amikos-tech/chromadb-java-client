package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Structured logger abstraction for Chroma client internals.
 *
 * <p>Integrators can bridge this interface to their logging stack (for example SLF4J).
 * Fields are best-effort metadata and are safe to ignore.</p>
 */
public interface ChromaLogger {

    void debug(String event, Map<String, Object> fields);

    /**
     * Reserved for future informational events.
     *
     * <p>Current transport logging emits debug/warn/error events.</p>
     */
    void info(String event, Map<String, Object> fields);

    void warn(String event, Map<String, Object> fields);

    void error(String event, Map<String, Object> fields, Throwable throwable);

    /**
     * Returns {@code true} if this logger discards all events.
     *
     * <p>The default implementation returns {@code false}. The built-in
     * {@link #noop()} logger overrides this to return {@code true}, allowing
     * transport code to skip log-field construction when logging is disabled.</p>
     */
    default boolean isNoop() {
        return false;
    }

    static ChromaLogger noop() {
        return ChromaLoggers.noopInstance();
    }
}
