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
     * Backward-compatible alias for the noop logger instance.
     *
     * <p>Prefer {@link #noop()}.</p>
     */
    @Deprecated
    ChromaLogger NOOP = ChromaLoggers.noopInstance();

    static ChromaLogger noop() {
        return ChromaLoggers.noopInstance();
    }
}
