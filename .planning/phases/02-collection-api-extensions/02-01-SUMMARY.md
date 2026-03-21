---
phase: 02-collection-api-extensions
plan: 01
subsystem: api
tags: [java, wiremock, collection, fork, indexing-status, value-object, dto]

# Dependency graph
requires:
  - phase: 01-result-ergonomics-wheredocument
    provides: "Stable v2 Collection interface baseline with 18 declared methods"
provides:
  - "IndexingStatus immutable value object with factory, getters, equals/hashCode/toString"
  - "Collection.fork(String), Collection.forkCount(), Collection.indexingStatus() interface methods"
  - "ChromaHttpCollection HTTP implementations for fork, forkCount, indexingStatus"
  - "ChromaApiPaths path builders: collectionFork, collectionForkCount, collectionIndexingStatus"
  - "ChromaDtos: ForkCollectionRequest, ForkCountResponse, IndexingStatusResponse DTOs"
  - "CollectionApiExtensionsValidationTest: 13 WireMock-backed tests"
affects:
  - 02-collection-api-extensions
  - cloud-integration-tests

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Cloud-only operations documented with Availability Javadoc tags on interface methods"
    - "ForkCountResponse DTO (not bare Integer) used for JSON object response parsing"
    - "explicitEmbeddingFunction passed through fork() to preserve EF inheritance in forked collections"

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/v2/IndexingStatus.java
    - src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsValidationTest.java
  modified:
    - src/main/java/tech/amikos/chromadb/v2/Collection.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java
    - src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java

key-decisions:
  - "forkCount() uses ForkCountResponse DTO (not Integer.class) because server returns a JSON object {count: N} not a bare integer"
  - "fork() passes explicitEmbeddingFunction (not null) to ChromaHttpCollection.from() for EF inheritance in forked collections"
  - "Cloud-only methods (fork, forkCount, indexingStatus) propagate ChromaNotFoundException naturally on 404 — no special handling"
  - "IndexingStatus fields use long (not int) for numIndexedOps/numUnindexedOps/totalOps per API spec"
  - "No isComplete() or other computed convenience methods on IndexingStatus (per D-11): callers derive from primitives"

patterns-established:
  - "Availability Javadoc tag: mark cloud-only methods with <strong>Availability:</strong> block explaining self-hosted vs cloud behavior"
  - "DTO pattern: use @SerializedName for snake_case JSON field mapping in package-private DTO inner classes"

requirements-completed:
  - COLL-01
  - COLL-02

# Metrics
duration: 3min
completed: 2026-03-21
---

# Phase 02 Plan 01: Collection API Extensions Summary

**fork/forkCount/indexingStatus on Collection interface with HTTP implementations and 13 WireMock unit tests covering request contracts, EF inheritance, and 404 error propagation**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T13:12:06Z
- **Completed:** 2026-03-21T13:15:10Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- IndexingStatus immutable value object with `of()` factory, four typed getters (3x long, 1x double), `equals`/`hashCode` (using `Double.compare` and `doubleToLongBits`), and `toString`
- Collection interface extended with `fork(String)`, `forkCount()`, `indexingStatus()` — each with full Javadoc including Availability notes for Cloud-only semantics
- ChromaHttpCollection implements all three methods using existing transport patterns; `fork()` passes `explicitEmbeddingFunction` to `from()` preserving EF inheritance; `forkCount()` parses JSON object via DTO not bare Integer
- 3 path builders added to ChromaApiPaths; 3 DTO classes added to ChromaDtos
- PublicInterfaceCompatibilityTest updated to 21 methods (from 18)
- CollectionApiExtensionsValidationTest: 13 tests covering IndexingStatus unit tests, fork POST contract, EF inheritance (reflection), null/blank validation, forkCount, indexingStatus field mapping, and 404 propagation

## Task Commits

1. **Task 1: Create IndexingStatus value object, interface methods, DTOs, and path builders** - `abb94b5` (feat)
2. **Task 2: Implement fork/forkCount/indexingStatus in ChromaHttpCollection and add WireMock validation tests** - `26ba337` (feat)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/IndexingStatus.java` — New immutable value object for indexing progress snapshots
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — Added fork, forkCount, indexingStatus interface declarations with Availability Javadoc
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — HTTP implementations of fork, forkCount, indexingStatus
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java` — collectionFork, collectionForkCount, collectionIndexingStatus path builders
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` — ForkCollectionRequest, ForkCountResponse, IndexingStatusResponse DTOs
- `src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsValidationTest.java` — 13 WireMock-backed tests for all three operations
- `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` — Updated EXPECTED_COLLECTION_METHOD_COUNT from 18 to 21

## Decisions Made

- **forkCount() DTO vs bare Integer:** `ForkCountResponse` DTO used because the server returns `{"count": N}` (a JSON object), not a bare integer. Using `Integer.class` would fail deserialization.
- **EF inheritance in fork():** `explicitEmbeddingFunction` passed to `ChromaHttpCollection.from()` so forked collections inherit the source EF for text embedding operations.
- **No special 404 handling:** Cloud-only methods let `ChromaNotFoundException` propagate naturally — self-hosted users get a clear typed exception with no special wrapping.
- **IndexingStatus uses `long` fields:** `numIndexedOps`, `numUnindexedOps`, `totalOps` are `long` (not `int`) matching the Chroma API spec for large operation counts.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- fork/forkCount/indexingStatus are fully wired and tested at the unit level
- Cloud integration tests can now exercise these endpoints against Chroma Cloud
- COLL-01 and COLL-02 requirements are complete

---
*Phase: 02-collection-api-extensions*
*Completed: 2026-03-21*
