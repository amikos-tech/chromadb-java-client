---
phase: 03-search-api
plan: 02
subsystem: api
tags: [search, knn, rrf, dto, http, java]

# Dependency graph
requires:
  - phase: 03-search-api-01
    provides: "Knn, Rrf, Search, Select, GroupBy, ReadLevel, SparseVector, SearchResult/Row/Group interfaces, Collection.SearchBuilder"

provides:
  - "ChromaDtos.SearchRequest: search envelope DTO with polymorphic rank serialization"
  - "ChromaDtos.SearchResponse: search response DTO with ids/documents/metadatas/embeddings/scores"
  - "ChromaDtos.buildKnnRankMap: serializes Knn to {knn:{...}} wire format"
  - "ChromaDtos.buildRrfRankMap: serializes Rrf to {rrf:{ranks:[...],k:60}} wire format"
  - "ChromaDtos.buildSearchItemMap: builds per-search map with rank, filter, select, limit, group_by"
  - "ChromaApiPaths.collectionSearch: path builder for /search endpoint"
  - "SearchResultRowImpl: immutable SearchResultRow with composition over ResultRowImpl, Float score"
  - "SearchResultGroupImpl: immutable SearchResultGroup with key and row group"
  - "SearchResultImpl: lazy-cached row groups, Double scores, grouped flag, from(DTO) factory"
  - "SearchBuilderImpl (ChromaHttpCollection): full HTTP POST wiring for search() operation"

affects:
  - 03-search-api-03 (unit and integration tests for all search types)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Map<String, Object> polymorphic serialization for rank field (knn vs rrf discriminated by wrapper key)"
    - "composition pattern for SearchResultRowImpl: wraps ResultRowImpl, adds Float score"
    - "AtomicReferenceArray lazy-cached row groups in SearchResultImpl (same as QueryResultImpl)"
    - "Double scores stored internally, downcast to Float on getScore() per SearchResultRow contract"

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/v2/SearchResultRowImpl.java
    - src/main/java/tech/amikos/chromadb/v2/SearchResultGroupImpl.java
    - src/main/java/tech/amikos/chromadb/v2/SearchResultImpl.java
  modified:
    - src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java

key-decisions:
  - "SearchRequest.searches uses List<Map<String,Object>> (not typed DTOs) for polymorphic rank serialization — knn and rrf require different keys in the rank wrapper"
  - "filter key is used (not where) in buildSearchItemMap per Search API spec pitfall documented in research"
  - "scores stored as List<List<Double>> in SearchResultImpl to match wire precision; downcast to Float on row access per SearchResultRow.getScore() contract"
  - "SearchResultImpl.groups() returns each row as a single-element group with key=null for initial implementation — group key extraction from server response will be refined in integration tests"
  - "Global limit/offset is propagated to per-search items only when the search lacks its own limit — per-search limit wins"

# Metrics
duration: 3min
completed: 2026-03-22
---

# Phase 03 Plan 02: Search API Implementation Summary

**Search DTOs, HTTP wiring, and result converters: ChromaDtos SearchRequest/SearchResponse, buildKnnRankMap/buildRrfRankMap/buildSearchItemMap helpers, collectionSearch path, SearchResultRowImpl/GroupImpl/Impl, and SearchBuilderImpl replacing the stub — 6 files completing the full search request/response pipeline**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-22T18:11:36Z
- **Completed:** 2026-03-22T18:14:21Z
- **Tasks:** 2
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments

- Task 1: Added `collectionSearch()` path builder to ChromaApiPaths. Added `SearchRequest` and `SearchResponse` DTOs to ChromaDtos. Added `buildKnnRankMap`, `buildRrfRankMap`, and `buildSearchItemMap` helper methods with correct `"filter"` key (not `"where"`), polymorphic rank wrapper (`{knn:{...}}` / `{rrf:{...}}`), select/group_by/limit serialization.
- Task 2: Created `SearchResultRowImpl` (composition over `ResultRowImpl`, `Float score`), `SearchResultGroupImpl` (key + row group), `SearchResultImpl` (lazy-cached rows via `AtomicReferenceArray`, `Double` scores, grouped flag, `from(SearchResponse, boolean)` factory). Replaced stub `SearchBuilderImpl` in `ChromaHttpCollection` with full implementation: convenience `queryText`/`queryEmbedding`, batch `searches(Search...)`, global filter/limit/offset/readLevel, HTTP POST to `/search` via `ChromaApiPaths.collectionSearch`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Search DTOs and API path** - `516e4e2` (feat)
2. **Task 2: Implement SearchResult impls and wire SearchBuilderImpl** - `56ba74e` (feat)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java` - Added `collectionSearch(tenant, db, id)` returning `collectionById(...) + "/search"`
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` - Added `SearchRequest`, `SearchResponse` DTOs; `buildKnnRankMap`, `buildRrfRankMap`, `buildSearchItemMap` helpers
- `src/main/java/tech/amikos/chromadb/v2/SearchResultRowImpl.java` - Immutable `SearchResultRow` using composition; `Float score`, equals/hashCode/toString
- `src/main/java/tech/amikos/chromadb/v2/SearchResultGroupImpl.java` - Immutable `SearchResultGroup`; `Object key`, `ResultGroup<SearchResultRow> rows`
- `src/main/java/tech/amikos/chromadb/v2/SearchResultImpl.java` - Immutable `SearchResult`; lazy-cached row groups, `Double` scores, `boolean grouped`, `from()` factory; column and row accessors; `stream()`
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - Replaced stub `SearchBuilderImpl` with full implementation: fields for `searches`, `globalFilter`, `globalLimit`, `globalOffset`, `readLevel`; all builder methods; `execute()` wires HTTP POST

## Decisions Made

- `SearchRequest.searches` is `List<Map<String,Object>>` instead of typed inner DTOs — the `rank` field is polymorphic (either `{knn:{...}}` or `{rrf:{...}}`) and cannot be represented as a single typed class without custom Gson adapter.
- `"filter"` key (not `"where"`) used in `buildSearchItemMap` per Search API wire format (per plan Pitfall 1).
- `SearchResultImpl` stores scores as `List<List<Double>>` (wire precision) and downcasts to `Float` on row access (per `SearchResultRow.getScore()` contract, per plan Pitfall 2 / D-11).
- `SearchResultImpl.groups()` returns single-element groups with `key=null` for initial implementation — group key extraction from server response is a future refinement tied to integration test results.
- Global limit/offset propagation is per-search: only applied when a search has no explicit limit of its own.

## Deviations from Plan

None - plan executed exactly as written. The `toFloatList(float[])` helper already existed in `ChromaDtos.java` (noted in plan as a check step — confirmed present, reused).

## Known Stubs

| File | Description | Reason |
|------|-------------|--------|
| `SearchResultImpl.groups()` | Returns each row as single-element group with `key=null` | Server response format for groupBy results not yet verified in integration tests; group key extraction will be refined in Plan 03 integration tests |

## Issues Encountered

None — plan executed cleanly with two atomic commits.

## Next Phase Readiness

- Complete search pipeline is operational: `collection.search().queryText("foo").limit(5).execute()` routes to HTTP POST `/search`
- Plan 03 can now implement unit tests against all new types and update `EXPECTED_COLLECTION_METHOD_COUNT`
- `SearchResultImpl.groups()` stub is documented and ready for refinement when integration test data shows the actual grouped response format

---
*Phase: 03-search-api*
*Completed: 2026-03-22*

## Self-Check: PASSED

- SearchResultRowImpl.java: FOUND
- SearchResultGroupImpl.java: FOUND
- SearchResultImpl.java: FOUND
- 03-02-SUMMARY.md: FOUND
- Commit 516e4e2: FOUND
- Commit 56ba74e: FOUND
- mvn compile: PASSED
