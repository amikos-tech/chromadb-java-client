---
phase: 03-embeddings-id-extensibility
plan: 01
subsystem: embeddings
tags: [java, wiremock, conformance-tests, embedding-functions, chromaexception, logging]

# Dependency graph
requires:
  - phase: 02-api-coverage-completion
    provides: ChromaException hierarchy (public constructors), ChromaHttpCollection with embeddingFunctionSpec handling
provides:
  - Normalized input validation and error wrapping across all four remote EF providers
  - WireMock conformance test suite (AbstractEmbeddingFunctionConformanceTest + 4 concrete subclasses)
  - EF precedence Javadoc on Collection interface and requireEmbeddingFunction()
  - WARNING log when explicit EF overrides persisted spec
  - FINE log when auto-wiring EF from spec
  - EmbeddingPrecedenceTest contract test suite (5 tests)
affects:
  - 03-02 (ID extensibility plan may also touch embeddings tests)
  - Any future provider additions (must follow AbstractEmbeddingFunctionConformanceTest pattern)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - AbstractEmbeddingFunctionConformanceTest abstract base class for provider tests (WireMock + JUnit4)
    - ChromaException thrown directly from embeddings.* packages (public constructors)
    - LOG.warning for explicit EF override, LOG.fine for auto-wire

key-files:
  created:
    - src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java
    - src/test/java/tech/amikos/chromadb/embeddings/OpenAIConformanceTest.java
    - src/test/java/tech/amikos/chromadb/embeddings/CohereConformanceTest.java
    - src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceConformanceTest.java
    - src/test/java/tech/amikos/chromadb/embeddings/OllamaConformanceTest.java
    - src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java
  modified:
    - src/main/java/tech/amikos/chromadb/v2/ChromaException.java
    - src/main/java/tech/amikos/chromadb/embeddings/openai/OpenAIEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/cohere/CohereEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/hf/HuggingFaceEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/ollama/OllamaEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/v2/Collection.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java

key-decisions:
  - "ChromaException convenience constructors widened from protected to public so embeddings.* packages can throw directly without EFException wrapping"
  - "Count-mismatch detection added after response deserialization in each provider's embedDocuments() method"
  - "Warning log fires on every requireEmbeddingFunction() call when explicit EF is set and spec exists (not just first call)"
  - "FINE log fires before EmbeddingFunctionResolver.resolve() so it fires even when resolution fails"
  - "HuggingFace embedQueries() delegates to embedDocuments(); null/empty guard placed in embedDocuments(List) only (no duplication)"

patterns-established:
  - "AbstractEmbeddingFunctionConformanceTest pattern: extend for each new remote provider with stubSuccess/stubFailure/stubCountMismatch"
  - "Provider error messages format: '{ProviderName} embedding failed (model: {modelName}): {details}'"
  - "EF precedence: explicit runtime > configuration.embedding_function > schema.default_embedding_function"

requirements-completed:
  - EMB-01
  - EMB-03

# Metrics
duration: 20min
completed: 2026-03-19
---

# Phase 03 Plan 01: Embedding Provider Error Normalization and Precedence Contract Summary

**WireMock conformance suite (20 tests) for all four remote EF providers with null/empty guards, ChromaException error wrapping, and count-mismatch detection; EF precedence chain documented in Javadoc and locked with 5 EmbeddingPrecedenceTest contract tests plus WARNING/FINE logging**

## Performance

- **Duration:** 20 min
- **Started:** 2026-03-19T09:49:00Z
- **Completed:** 2026-03-19T09:58:00Z
- **Tasks:** 2
- **Files modified:** 13 (6 created, 7 modified)

## Accomplishments
- Added null/empty guard and count-mismatch detection to all four providers (OpenAI, Cohere, HuggingFace, Ollama), plus normalized API error messages with provider name and model in ChromaException
- Created AbstractEmbeddingFunctionConformanceTest WireMock base class with 5 behavioral tests, plus concrete subclasses for all four providers (20 passing tests)
- Documented EF precedence chain in Collection interface Javadoc and requireEmbeddingFunction() Javadoc, added WARNING log for explicit-overrides-spec and FINE log for auto-wire, locked with EmbeddingPrecedenceTest (5 passing tests)

## Task Commits

Each task was committed atomically:

1. **Task 1: Provider input validation, error normalization, conformance tests** - `8732809` (feat)
2. **Task 2: EF precedence Javadoc, warning/debug logging, contract tests** - `1734894` (feat)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/ChromaException.java` - Widened convenience constructors to public
- `src/main/java/tech/amikos/chromadb/embeddings/openai/OpenAIEmbeddingFunction.java` - Null/empty guards, error normalization, count-mismatch check
- `src/main/java/tech/amikos/chromadb/embeddings/cohere/CohereEmbeddingFunction.java` - Same as above, plus embedQueries guards
- `src/main/java/tech/amikos/chromadb/embeddings/hf/HuggingFaceEmbeddingFunction.java` - Same as above
- `src/main/java/tech/amikos/chromadb/embeddings/ollama/OllamaEmbeddingFunction.java` - Same as above
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` - Added EF precedence Javadoc section
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - Added requireEmbeddingFunction() Javadoc, WARNING log, FINE log
- `src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java` - Base conformance class (5 tests)
- `src/test/java/tech/amikos/chromadb/embeddings/OpenAIConformanceTest.java` - OpenAI concrete conformance test
- `src/test/java/tech/amikos/chromadb/embeddings/CohereConformanceTest.java` - Cohere concrete conformance test
- `src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceConformanceTest.java` - HuggingFace concrete conformance test
- `src/test/java/tech/amikos/chromadb/embeddings/OllamaConformanceTest.java` - Ollama concrete conformance test
- `src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java` - EF precedence contract tests (5 tests)

## Decisions Made

- ChromaException convenience constructors widened to public so embeddings.* packages can throw directly without EFException wrapping (avoids double-wrapping, cleaner error propagation)
- Count-mismatch detection added after response deserialization; uses provider+model message format consistent with other errors
- WARNING log fires on every requireEmbeddingFunction() call where explicit EF + spec coexist (not cached/suppressed) — consistent signal for debugging
- FINE log fires before EmbeddingFunctionResolver.resolve() so it captures intent even when resolution fails with unsupported provider
- HuggingFace embedQueries() delegates to embedDocuments(); guards placed only in embedDocuments(List) to avoid duplication

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required. Tests use WireMock stubs (no real API keys).

## Next Phase Readiness

- EMB-01 and EMB-03 requirements complete
- All conformance tests serve as regression suite for future provider changes
- EF precedence chain is documented and contract-tested; Phase 03 Plan 02 (ID extensibility) can proceed

## Self-Check: PASSED

- AbstractEmbeddingFunctionConformanceTest.java: FOUND
- OpenAIConformanceTest.java: FOUND
- CohereConformanceTest.java: FOUND
- HuggingFaceConformanceTest.java: FOUND
- OllamaConformanceTest.java: FOUND
- EmbeddingPrecedenceTest.java: FOUND
- 03-01-SUMMARY.md: FOUND
- Commit 8732809: FOUND
- Commit 1734894: FOUND

---
*Phase: 03-embeddings-id-extensibility*
*Completed: 2026-03-19*
