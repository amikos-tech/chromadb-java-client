import tech.amikos.chromadb.v2.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingExample {

    public static void main(String[] args) {
        // --8<-- [start:default-logger]
        // By default, no logger is set — the client discards all internal events.
        // Use ChromaLogger.noop() to make the no-op behavior explicit:
        Client silentClient = ChromaClient.builder()
                .baseUrl(System.getenv("CHROMA_URL"))
                .logger(ChromaLogger.noop())
                .build();
        // --8<-- [end:default-logger]

        // --8<-- [start:custom-logger]
        // Bridge ChromaLogger to java.util.logging:
        final Logger julLogger = Logger.getLogger("tech.amikos.chromadb");
        julLogger.setLevel(Level.FINE);

        ChromaLogger customLogger = new ChromaLogger() {
            @Override
            public void debug(String event, Map<String, Object> fields) {
                julLogger.fine("[chroma] " + event + " " + fields);
            }

            @Override
            public void info(String event, Map<String, Object> fields) {
                julLogger.info("[chroma] " + event + " " + fields);
            }

            @Override
            public void warn(String event, Map<String, Object> fields) {
                julLogger.warning("[chroma] " + event + " " + fields);
            }

            @Override
            public void error(String event, Map<String, Object> fields, Throwable throwable) {
                julLogger.log(Level.SEVERE, "[chroma] " + event + " " + fields, throwable);
            }
        };

        Client client = ChromaClient.builder()
                .baseUrl(System.getenv("CHROMA_URL"))
                .logger(customLogger)
                .build();
        // --8<-- [end:custom-logger]
    }
}
