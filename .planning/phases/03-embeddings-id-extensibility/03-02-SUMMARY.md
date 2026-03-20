---
phase: 03-embeddings-id-extensibility
plan: 02
subsystem: embeddings
tags: [okhttp, wiremock, onnx, retry, thread-safety, download-timeout]

# Dependency graph
requires:
  - phase: 03-01
    provides: "ChromaException public constructors widened so embeddings package can throw directly"
provides:
  - "DefaultEmbeddingFunction uses OkHttp with configurable timeout (default 300s) instead of URL.openStream()"
  - "Retry-once on retryable failures (SocketTimeout, ConnectException, HTTP 5xx)"
  - "Fail-fast on non-retryable failures (HTTP 404, 403, checksum mismatch)"
  - "Thread-safe lazy init via double-checked locking with stateless validateModel() gate"
  - "modelDownloadUrl package-private non-final field for WireMock test overrides"
  - "DefaultEmbeddingFunctionTest with 7 unit tests covering download reliability scenarios"
affects:
  - "03-03-embeddings-id-extensibility"
  - "testing"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Retry classification via private inner exception hierarchy (RetryableDownloadException, NonRetryableDownloadException)"
    - "Double-checked locking with stateless file-existence gate for thread-safe lazy init"
    - "Package-private non-final static field for test URL injection (no reflection required)"
    - "WireMock fixed-delay stub for timeout simulation in unit tests"

key-files:
  created:
    - src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java
  modified:
    - src/main/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunction.java

key-decisions:
  - "Download failures throw ChromaException (unchecked, v2 package) not EFException, per user decision in CONTEXT.md"
  - "validateModel() remains a stateless file-existence check (no static boolean flag) so TestDefaultEmbeddings.testDownloadModel() continues to work after file deletion in same JVM"
  - "modelDownloadUrl is package-private non-final static field (not private final) to enable test URL injection without reflection hacks"
  - "Retry exactly once on retryable failures; fail fast on non-retryable"

patterns-established:
  - "RetryableDownloadException / NonRetryableDownloadException: inner exception hierarchy for retry classification"
  - "ensureModelDownloaded() double-checked locking pattern for thread-safe lazy model download"

requirements-completed:
  - EMB-02

# Metrics
duration: 5min
completed: 2026-03-19
---

# Phase 03 Plan 02: DefaultEmbeddingFunction Download Reliability Summary

**OkHttp-based model download with configurable 300s timeout, retry-once for transient failures, fail-fast for 404/403/checksum, double-checked locking thread safety, and 7 WireMock unit tests**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-19T10:03:59Z
- **Completed:** 2026-03-19T10:08:41Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Replaced `URL.openStream()` (no timeout, no retry) with OkHttp client with configurable `readTimeout` (default 300s)
- Added overloaded constructor `DefaultEmbeddingFunction(int downloadTimeoutSeconds)` for custom timeout
- Implemented retry-once for transient failures (SocketTimeoutException, ConnectException, HTTP 5xx) and fail-fast for non-retryable (HTTP 404, 403, checksum mismatch)
- All download failures throw `ChromaException` (unchecked) with actionable message including download URL
- Thread-safe lazy init via `synchronized(MODEL_INIT_LOCK)` double-checked locking using stateless `validateModel()` file-existence gate
- `modelDownloadUrl` changed from `private static final String` to `static String` (package-private, non-final) for WireMock test injection
- Created `DefaultEmbeddingFunctionTest` with 7 tests: timeout constant, cache dir stability, custom timeout constructor, concurrent thread safety, non-retryable 404 (1 request), retryable 503 (2 requests), and timeout ChromaException

## Task Commits

Each task was committed atomically:

1. **Task 1: Refactor DefaultEmbeddingFunction with OkHttp download, timeout, retry, and thread safety** - `236572e` (feat)
2. **Task 2: Create DefaultEmbeddingFunctionTest with WireMock-based download reliability tests** - `7d474fc` (feat)

**Plan metadata:** committed as part of docs update

## Files Created/Modified
- `src/main/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunction.java` - OkHttp download with configurable timeout, retry classification, double-checked locking, ChromaException on failure
- `src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java` - 7 WireMock-based unit tests covering timeout, retry, non-retryable, and thread safety

## Decisions Made
- `ChromaException` (unchecked, `tech.amikos.chromadb.v2`) is thrown for all download failures, not `EFException` — per user decision in CONTEXT.md. This doesn't violate the `throws EFException` constructor declaration since ChromaException is unchecked.
- `validateModel()` remains a stateless file-existence check so `testDownloadModel()` (which deletes the model directory) continues to work correctly within the same JVM without needing a static boolean reset.
- `modelDownloadUrl` is package-private non-final so test classes in the same package can override it directly — no reflection required.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- EMB-02 complete: DefaultEmbeddingFunction is now reliable for CI use without indefinite blocking
- The blocker noted in STATE.md ("full mvn test run interrupted after stalling on first-time ONNX model download") is resolved by the 300s timeout and retry
- Plan 03-03 (ID extensibility) can proceed; it depends on ChromaHttpCollection changes which are already pending

---
*Phase: 03-embeddings-id-extensibility*
*Completed: 2026-03-19*
