---
phase: 01-result-ergonomics-wheredocument
plan: 01
subsystem: api
tags: [java, result-types, interfaces, immutability, tdd, unit-tests]

# Dependency graph
requires: []
provides:
  - "ResultRow public interface with getId/getDocument/getMetadata/getEmbedding/getUri"
  - "QueryResultRow public interface extending ResultRow with getDistance() returning boxed Float"
  - "ResultGroup<R extends ResultRow> public interface extending Iterable with get/size/isEmpty/stream/toList"
  - "ResultRowImpl package-private: defensive copy embedding, unmodifiable metadata, null for non-included fields"
  - "QueryResultRowImpl package-private: composes ResultRowImpl, adds Float distance"
  - "ResultGroupImpl package-private: unmodifiable-list backed, IndexOutOfBoundsException on bad index"
  - "ResultRowTest: 22 unit tests covering all type contracts"
affects: [02-result-ergonomics-wheredocument, QueryResult, GetResult, row-iteration]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Interface-first result type hierarchy: public interface + package-private impl"
    - "Defensive copy on every getEmbedding() call (not just construction)"
    - "Composition over inheritance in QueryResultRowImpl (wraps ResultRowImpl)"
    - "Unmodifiable map from LinkedHashMap copy (preserves insertion order)"
    - "ResultGroupImpl delegates IndexOutOfBoundsException to underlying List.get()"

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/v2/ResultRow.java
    - src/main/java/tech/amikos/chromadb/v2/QueryResultRow.java
    - src/main/java/tech/amikos/chromadb/v2/ResultGroup.java
    - src/main/java/tech/amikos/chromadb/v2/ResultRowImpl.java
    - src/main/java/tech/amikos/chromadb/v2/QueryResultRowImpl.java
    - src/main/java/tech/amikos/chromadb/v2/ResultGroupImpl.java
    - src/test/java/tech/amikos/chromadb/v2/ResultRowTest.java
  modified: []

key-decisions:
  - "ResultRow fields return null (not Optional, not exception) when Include not requested — consistent with existing GetResult/QueryResult approach"
  - "QueryResultRowImpl uses composition (wraps ResultRowImpl) to avoid code duplication without inheritance"
  - "Defensive copy applied on every getEmbedding() call, not just construction, to prevent aliasing across callers"
  - "ResultGroupImpl.toList() returns the stored unmodifiable list directly (no extra copy needed — already unmodifiable)"

patterns-established:
  - "Public interface in tech.amikos.chromadb.v2; package-private final impl in same package"
  - "Embedding immutability: Arrays.copyOf on construction AND on each getter call"
  - "Metadata immutability: Collections.unmodifiableMap(new LinkedHashMap<>(source))"

requirements-completed: [ERGO-01]

# Metrics
duration: 2min
completed: 2026-03-20
---

# Phase 01 Plan 01: Result Ergonomics WhereDocument Summary

**Public ResultRow/QueryResultRow/ResultGroup interface hierarchy with package-private immutable implementations, defensive embedding copies, and 22 green unit tests**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-20T19:12:04Z
- **Completed:** 2026-03-20T19:13:44Z
- **Tasks:** 1
- **Files modified:** 7

## Accomplishments
- Created 3 public interfaces (ResultRow, QueryResultRow, ResultGroup) forming the foundation for row-based result iteration (ERGO-01)
- Created 3 package-private final implementations (ResultRowImpl, QueryResultRowImpl, ResultGroupImpl) with full immutability guarantees
- Created ResultRowTest with 22 unit tests covering null fields, defensive copy, unmodifiable metadata, distance, iteration, and stream operations

## Task Commits

TDD task with two commits:

1. **RED: Failing tests** - `68e2928` (test)
2. **GREEN: Implementations** - `31aea48` (feat)

## Files Created/Modified
- `src/main/java/tech/amikos/chromadb/v2/ResultRow.java` - Public interface: getId/getDocument/getMetadata/getEmbedding/getUri
- `src/main/java/tech/amikos/chromadb/v2/QueryResultRow.java` - Public interface extending ResultRow with getDistance() -> boxed Float
- `src/main/java/tech/amikos/chromadb/v2/ResultGroup.java` - Public generic interface extending Iterable<R>: get/size/isEmpty/stream/toList
- `src/main/java/tech/amikos/chromadb/v2/ResultRowImpl.java` - Package-private final impl with defensive embedding copy and unmodifiable metadata map
- `src/main/java/tech/amikos/chromadb/v2/QueryResultRowImpl.java` - Package-private final impl composing ResultRowImpl, adds Float distance
- `src/main/java/tech/amikos/chromadb/v2/ResultGroupImpl.java` - Package-private final generic impl backed by unmodifiable list
- `src/test/java/tech/amikos/chromadb/v2/ResultRowTest.java` - 22 unit tests covering all contracts

## Decisions Made
- ResultRow fields return null (not Optional, not exception) when Include not requested — consistent with existing GetResult/QueryResult approach
- QueryResultRowImpl uses composition (wraps ResultRowImpl) to avoid code duplication without inheritance
- Defensive copy applied on every getEmbedding() call, not just construction, to prevent aliasing across callers
- ResultGroupImpl.toList() returns the stored unmodifiable list directly (no extra copy needed — already unmodifiable)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- ResultRow/QueryResultRow/ResultGroup interfaces are ready for Plan 02 to wire QueryResult and GetResult to use them
- All implementations pass tests; package-private access is correct for encapsulation
- No blockers

---
*Phase: 01-result-ergonomics-wheredocument*
*Completed: 2026-03-20*

## Self-Check: PASSED

All created files found on disk. All task commits verified in git log.
