---
phase: 04-embedding-ecosystem
plan: 02
subsystem: embeddings
tags: [reranking, cohere, jina, okhttp, wiremock]

requires:
  - phase: 03-embeddings-id-extensibility
    provides: WithParam configuration pattern, EFException hierarchy
provides:
  - RerankingFunction interface for document reranking
  - RerankResult immutable value type
  - CohereRerankingFunction (Cohere v2/rerank endpoint)
  - JinaRerankingFunction (Jina v1/rerank endpoint)
affects: [04-embedding-ecosystem, embedding-registry]

tech-stack:
  added: []
  patterns: [reranking-function-interface, wiremock-baseapi-injection-via-withparam]

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/reranking/RerankingFunction.java
    - src/main/java/tech/amikos/chromadb/reranking/RerankResult.java
    - src/main/java/tech/amikos/chromadb/reranking/cohere/CohereRerankingFunction.java
    - src/main/java/tech/amikos/chromadb/reranking/cohere/RerankRequest.java
    - src/main/java/tech/amikos/chromadb/reranking/cohere/RerankResponse.java
    - src/main/java/tech/amikos/chromadb/reranking/jina/JinaRerankingFunction.java
    - src/main/java/tech/amikos/chromadb/reranking/jina/RerankRequest.java
    - src/main/java/tech/amikos/chromadb/reranking/jina/RerankResponse.java
    - src/test/java/tech/amikos/chromadb/reranking/TestRerankResult.java
    - src/test/java/tech/amikos/chromadb/reranking/TestCohereRerankingFunction.java
    - src/test/java/tech/amikos/chromadb/reranking/TestJinaRerankingFunction.java
  modified: []

key-decisions:
  - "WireMock tests use WithParam.baseAPI() injection instead of static field mutation -- defaults list captures DEFAULT_BASE_API at class-load time so static field change has no effect on constructor"

patterns-established:
  - "RerankingFunction interface: rerank(query, documents) returns List<RerankResult> sorted by descending score"
  - "Reranking providers follow same WithParam constructor pattern as embedding functions"
  - "Package-private static DEFAULT_BASE_API for documentation; actual test injection via WithParam.baseAPI()"

requirements-completed: [RERANK-01]

duration: 3min
completed: 2026-04-01
---

# Phase 04 Plan 02: Reranking Function Interface and Providers Summary

**RerankingFunction interface with Cohere v2 and Jina v1 reranking providers using OkHttp and WithParam configuration**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-01T12:39:33Z
- **Completed:** 2026-04-01T12:42:58Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- RerankingFunction interface with rerank(query, documents) returning sorted List<RerankResult>
- RerankResult immutable value type with index, score, equals/hashCode/toString
- CohereRerankingFunction targeting Cohere v2/rerank with Bearer auth and configurable model
- JinaRerankingFunction targeting Jina v1/rerank with Bearer auth and configurable model
- WireMock tests verifying HTTP calls, auth headers, error handling, and request body content

## Task Commits

Each task was committed atomically:

1. **Task 1: Create RerankingFunction interface, RerankResult, and Cohere/Jina providers** - `303a717` (feat)
2. **Task 2: Unit tests for RerankResult and WireMock tests for Cohere/Jina providers** - `b94c9ee` (test)

## Files Created/Modified
- `src/main/java/tech/amikos/chromadb/reranking/RerankingFunction.java` - Reranking interface with rerank(query, documents)
- `src/main/java/tech/amikos/chromadb/reranking/RerankResult.java` - Immutable value type with index and score
- `src/main/java/tech/amikos/chromadb/reranking/cohere/CohereRerankingFunction.java` - Cohere v2/rerank provider
- `src/main/java/tech/amikos/chromadb/reranking/cohere/RerankRequest.java` - Cohere request DTO
- `src/main/java/tech/amikos/chromadb/reranking/cohere/RerankResponse.java` - Cohere response DTO
- `src/main/java/tech/amikos/chromadb/reranking/jina/JinaRerankingFunction.java` - Jina v1/rerank provider
- `src/main/java/tech/amikos/chromadb/reranking/jina/RerankRequest.java` - Jina request DTO
- `src/main/java/tech/amikos/chromadb/reranking/jina/RerankResponse.java` - Jina response DTO
- `src/test/java/tech/amikos/chromadb/reranking/TestRerankResult.java` - Value type unit tests (5 tests)
- `src/test/java/tech/amikos/chromadb/reranking/TestCohereRerankingFunction.java` - Cohere WireMock tests (3 tests)
- `src/test/java/tech/amikos/chromadb/reranking/TestJinaRerankingFunction.java` - Jina WireMock tests (3 tests)

## Decisions Made
- WireMock tests use WithParam.baseAPI() constructor injection rather than static field mutation because the defaults list captures DEFAULT_BASE_API value at class-load time, making static field changes ineffective for constructor-based URL resolution

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Changed WireMock URL injection from static field to WithParam.baseAPI()**
- **Found during:** Task 2 (WireMock test creation)
- **Issue:** Plan specified setting `DEFAULT_BASE_API` static field in `@Before`, but the `defaults` list captures the field value at class-load time via `WithParam.baseAPI(DEFAULT_BASE_API)`, so mutating the static field after class loading has no effect on constructor behavior
- **Fix:** Tests pass `WithParam.baseAPI(wireMockUrl)` as constructor parameter instead of mutating static field
- **Files modified:** TestCohereRerankingFunction.java, TestJinaRerankingFunction.java
- **Verification:** All 11 tests pass
- **Committed in:** b94c9ee (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary fix for test correctness. No scope creep.

## Issues Encountered
None

## Known Stubs
None - all interfaces are fully wired to HTTP endpoints.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- RerankingFunction interface ready for integration into Collection query workflows
- Pattern established for adding additional reranking providers
- WireMock test pattern ready for reuse with future providers

## Self-Check: PASSED

All 11 created files verified on disk. Both task commits (303a717, b94c9ee) verified in git log.

---
*Phase: 04-embedding-ecosystem*
*Completed: 2026-04-01*
