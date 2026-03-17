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

    void info(String event, Map<String, Object> fields);

    void warn(String event, Map<String, Object> fields);

    void error(String event, Map<String, Object> fields, Throwable throwable);

    ChromaLogger NOOP = new ChromaLogger() {
        @Override
        public void debug(String event, Map<String, Object> fields) {}

        @Override
        public void info(String event, Map<String, Object> fields) {}

        @Override
        public void warn(String event, Map<String, Object> fields) {}

        @Override
        public void error(String event, Map<String, Object> fields, Throwable throwable) {}
    };

    static ChromaLogger noop() {
        return NOOP;
    }
}
