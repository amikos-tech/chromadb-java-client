# Logging

The client uses a structured `ChromaLogger` interface for transport-level events. By default, no logger is set and all internal events are silently discarded.

## ChromaLogger Interface

`ChromaLogger` (`tech.amikos.chromadb.v2.ChromaLogger`) is a structured logging interface. Implement it to bridge client events to your application's logging stack.

```java
public interface ChromaLogger {
    void debug(String event, Map<String, Object> fields);
    void info(String event, Map<String, Object> fields);
    void warn(String event, Map<String, Object> fields);
    void error(String event, Map<String, Object> fields, Throwable throwable);
    default boolean isNoop() { return false; }
}
```

`fields` is a best-effort metadata map. The `isNoop()` method allows transport code to skip log-field construction when logging is disabled (the default no-op implementation returns `true`).

## No-op Logger (Default)

The built-in no-op logger explicitly discards all events. Use it to make silent behavior explicit in code:

```java
--8<-- "LoggingExample.java:default-logger"
```

`ChromaLogger.noop()` returns a singleton no-op instance.

## Custom Logger

Implement `ChromaLogger` to bridge to any logging backend. The following example bridges to `java.util.logging`:

```java
--8<-- "LoggingExample.java:custom-logger"
```

The same pattern applies to SLF4J, Log4j 2, or any other logging framework — implement the four methods and delegate to your framework's equivalent levels.

## Setting a Logger

Pass the logger to `ChromaClient.builder()` or `ChromaClient.cloud()`:

```java
Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .logger(customLogger)
        .build();
```

The logger applies to transport-level events: HTTP request/response details, connection errors, and retry attempts.

!!! note
    The client emits `debug` events for internal request/response tracing. Implement `debug(...)` with a `FINE`-level or equivalent handler to see detailed logging.
