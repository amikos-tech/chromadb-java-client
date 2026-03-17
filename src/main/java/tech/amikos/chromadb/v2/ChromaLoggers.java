package tech.amikos.chromadb.v2;

import java.util.Map;

final class ChromaLoggers {

    private static final ChromaLogger NOOP = new ChromaLogger() {
        @Override
        public boolean isNoop() {
            return true;
        }

        @Override
        public void debug(String event, Map<String, Object> fields) {}

        @Override
        public void info(String event, Map<String, Object> fields) {}

        @Override
        public void warn(String event, Map<String, Object> fields) {}

        @Override
        public void error(String event, Map<String, Object> fields, Throwable throwable) {}
    };

    private ChromaLoggers() {}

    static ChromaLogger noopInstance() {
        return NOOP;
    }
}
