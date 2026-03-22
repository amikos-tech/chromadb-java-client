---
phase: 03-search-api
plan: 01
subsystem: api
tags: [search, knn, rrf, sparse-vector, java]

# Dependency graph
requires:
  - phase: 02-collection-api-extensions
    provides: "Collection interface, ResultRow/ResultGroup/QueryResult patterns, ChromaHttpCollection"
  - phase: 01-result-ergonomics-wheredocument
    provides: "ResultRow, ResultGroup, GetResult/QueryResult row-oriented access patterns"
provides:
  - "SparseVector: immutable value type for sparse KNN queries"
  - "Select: field projection constants and custom key factory"
  - "ReadLevel: enum (INDEX_AND_WAL, INDEX_ONLY) for search read control"
  - "GroupBy: builder for partitioning search results by metadata key"
  - "Knn: factory+fluent-chain ranking expression (text, embedding, sparse vector)"
  - "Rrf: builder combining multiple Knn sub-rankings with weights"
  - "Search: per-search builder composing rank, filter, select, groupBy, limit, offset"
  - "SearchResultRow: extends ResultRow with Float getScore()"
  - "SearchResultGroup: interface for grouped result access"
  - "SearchResult: dual column-oriented and row-oriented result access interface"
  - "Collection.SearchBuilder: fluent builder interface for search() operation"
affects:
  - 03-search-api-02 (DTO mapping, SearchResult implementation, SearchBuilderImpl wiring)
  - 03-search-api-03 (unit and integration tests for all new types)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Factory+fluent-chain pattern for Knn: static factories + immutable chainable setters"
    - "Inner Builder pattern for Rrf, Search, GroupBy following CollectionConfiguration"
    - "Immutable value type pattern for SparseVector (defensive copy on construction and getters)"
    - "Enum with wire-format value + fromValue() following Include.fromValue()"
    - "Stub UnsupportedOperationException in ChromaHttpCollection for planned Plan 02 wiring"

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/v2/SparseVector.java
    - src/main/java/tech/amikos/chromadb/v2/Select.java
    - src/main/java/tech/amikos/chromadb/v2/ReadLevel.java
    - src/main/java/tech/amikos/chromadb/v2/GroupBy.java
    - src/main/java/tech/amikos/chromadb/v2/Knn.java
    - src/main/java/tech/amikos/chromadb/v2/Rrf.java
    - src/main/java/tech/amikos/chromadb/v2/Search.java
    - src/main/java/tech/amikos/chromadb/v2/SearchResult.java
    - src/main/java/tech/amikos/chromadb/v2/SearchResultRow.java
    - src/main/java/tech/amikos/chromadb/v2/SearchResultGroup.java
  modified:
    - src/main/java/tech/amikos/chromadb/v2/Collection.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java

key-decisions:
  - "Knn uses factory+fluent-chain (not inner Builder) because it has only one required field and simple mutability"
  - "Rrf.Builder auto-sets returnRank=true on Knn sub-rankings via withReturnRank() to prevent Pitfall 3 from research"
  - "SearchResult.getScores() uses List<List<Double>> (not Float) to match wire format precision as documented in Pitfall 2"
  - "SearchBuilderImpl in ChromaHttpCollection is a stub throwing UnsupportedOperationException — wired in Plan 02"
  - "PublicInterfaceCompatibilityTest failure (expected method count 21 vs 22) is intentional — EXPECTED_COLLECTION_METHOD_COUNT updated in Plan 03"

patterns-established:
  - "Search API factory+fluent-chain (Knn): static factory for required type discriminator, then immutable chained setters"
  - "Auto-enable sub-ranking flags: Rrf.Builder.rank() auto-calls knn.withReturnRank() so callers cannot forget"

requirements-completed: [SEARCH-01, SEARCH-02, SEARCH-03, SEARCH-04]

# Metrics
duration: 4min
completed: 2026-03-22
---

# Phase 03 Plan 01: Search API Type System Summary

**Complete Search API type surface: SparseVector, Select, ReadLevel, GroupBy, Knn, Rrf, Search, SearchResult/Row/Group interfaces, and Collection.SearchBuilder — 11 files establishing the contract for Phase 3 Plan 02 implementation**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-22T18:04:17Z
- **Completed:** 2026-03-22T18:08:18Z
- **Tasks:** 2
- **Files modified:** 12 (10 created, 2 modified)

## Accomplishments

- Task 1: Four value types created — SparseVector (immutable with defensive copies), Select (field projection constants + key factory), ReadLevel (enum with fromValue), GroupBy (builder with key/minK/maxK)
- Task 2: Five ranking/builder/result types created — Knn (factory+fluent chain), Rrf (builder with auto-returnRank), Search (per-search builder), SearchResult/Row/Group interfaces
- Collection interface extended with `SearchBuilder search()` and `SearchBuilder` inner interface
- ChromaHttpCollection updated with stub SearchBuilderImpl (throws UnsupportedOperationException; full wiring in Plan 02)
- All 11 new/modified files compile cleanly

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Search API value types (SparseVector, Select, ReadLevel, GroupBy)** - `da4a7ad` (feat)
2. **Task 2: Create ranking builders (Knn, Rrf), Search builder, result interfaces, and SearchBuilder on Collection** - `94b982a` (feat)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/SparseVector.java` - Immutable sparse vector; int[]/float[] with validation, defensive copies, equals/hashCode
- `src/main/java/tech/amikos/chromadb/v2/Select.java` - Field projection: DOCUMENT/SCORE/EMBEDDING/METADATA/ID constants, key() factory, all()
- `src/main/java/tech/amikos/chromadb/v2/ReadLevel.java` - Enum INDEX_AND_WAL/INDEX_ONLY with wire-format value and fromValue()
- `src/main/java/tech/amikos/chromadb/v2/GroupBy.java` - Builder: required key, optional minK/maxK, validation on build()
- `src/main/java/tech/amikos/chromadb/v2/Knn.java` - Factory methods queryText/queryEmbedding/querySparseVector; immutable fluent chain; package-private withReturnRank()
- `src/main/java/tech/amikos/chromadb/v2/Rrf.java` - Builder: rank(Knn, weight) auto-sets returnRank; k/normalize params; RankWithWeight inner class
- `src/main/java/tech/amikos/chromadb/v2/Search.java` - Builder composing knn/rrf (mutually exclusive), filter, select, groupBy, limit, offset
- `src/main/java/tech/amikos/chromadb/v2/SearchResult.java` - Interface: column-oriented (ids/docs/metadatas/embeddings/scores) and row-oriented (rows/groups/stream)
- `src/main/java/tech/amikos/chromadb/v2/SearchResultRow.java` - Extends ResultRow with Float getScore()
- `src/main/java/tech/amikos/chromadb/v2/SearchResultGroup.java` - Interface: getKey() + rows() for grouped results
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` - Added SearchBuilder search() method + SearchBuilder inner interface
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - Added search() override + stub SearchBuilderImpl

## Decisions Made

- Knn uses factory+fluent-chain (not inner Builder): single required parameter (query type is factory-discriminated), so the builder pattern would be overkill. Fluent chain keeps it concise.
- Rrf.Builder auto-calls `knn.withReturnRank()` on rank() to prevent callers from forgetting the flag (per Pitfall 3 in research notes).
- SearchResult.getScores() typed as `List<List<Double>>` (not Float) to match wire format precision per Pitfall 2.
- SearchBuilderImpl is a stub throwing UnsupportedOperationException — full wiring deferred to Plan 02 per plan scope.
- PublicInterfaceCompatibilityTest failure (expected 21, got 22) is expected/anticipated by the plan — EXPECTED_COLLECTION_METHOD_COUNT will be updated in Plan 03.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added stub SearchBuilderImpl to ChromaHttpCollection**
- **Found during:** Task 2 (Extending Collection interface with SearchBuilder)
- **Issue:** Adding `SearchBuilder search()` as abstract method on Collection caused ChromaHttpCollection (the sole implementation) to fail to compile — `is not abstract and does not override abstract method search()`
- **Fix:** Added `search()` override returning a `SearchBuilderImpl` stub; all SearchBuilderImpl methods throw `UnsupportedOperationException` with "coming in Phase 03 Plan 02" message
- **Files modified:** `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java`
- **Verification:** `mvn compile` passes cleanly
- **Committed in:** `94b982a` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Essential to unblock compilation. Stub pattern is correct approach — Plan 02 will replace with real implementation.

## Known Stubs

| File | Description | Reason |
|------|-------------|--------|
| `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — `SearchBuilderImpl` | All methods throw `UnsupportedOperationException` | HTTP DTO wiring and SearchResult implementation are in Plan 02. This stub satisfies the compiler contract so Plan 01's type system can compile independently. |

## Issues Encountered

None — plan executed cleanly with one expected compilation fix (stub for new interface method).

## Next Phase Readiness

- All public types defined; Plan 02 can implement ChromaDtos search request/response, wire SearchBuilderImpl, implement SearchResultImpl/SearchResultRowImpl
- Plan 03 can implement unit tests against all new types and update EXPECTED_COLLECTION_METHOD_COUNT
- PublicInterfaceCompatibilityTest expected to remain failing until Plan 03 updates the count constant

---
*Phase: 03-search-api*
*Completed: 2026-03-22*
