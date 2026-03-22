# Phase 3: Search API - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the Chroma Search endpoint (v1.5+) with full ranking expression DSL, field projection, groupBy, and read levels — matching Go client capabilities. Requirements: SEARCH-01, SEARCH-02, SEARCH-03, SEARCH-04.

This phase delivers the `collection.search()` API surface, builder types (Search, Knn, Rrf, GroupBy), result types (SearchResult, SearchResultRow), the Select projection mechanism, SparseVector value type, and integration tests against Chroma >= 1.5.

</domain>

<decisions>
## Implementation Decisions

### Search Builder API Shape
- **D-01:** Hybrid approach — convenience shortcuts on SearchBuilder for simple KNN (`queryText()`, `queryEmbedding()` directly on the builder), explicit `Search` objects via `searches(Search...)` for batch and complex cases.
- **D-02:** Simple KNN case must be as frictionless as possible: `collection.search().queryText("headphones").limit(3).execute()` — consistent with `query()` builder shape.
- **D-03:** Batch search is first-class: `collection.search().searches(search1, search2).limit(5).execute()` — multiple `Search` objects in a single request.
- **D-04:** Both per-search filters (`Search.builder().knn(...).where(Where.eq(...))`) and global filters (`searchBuilder.where(Where.eq(...))`) supported. They combine (AND) when both present — this is how the Chroma API works.
- **D-05:** Naming follows Chroma search API terminology: `limit()` and `offset()` (not `nResults()`). This diverges from `query()` naming but matches the upstream API accurately.

### Result Type Design
- **D-06:** Single `SearchResult` interface — no compile-time split between grouped and ungrouped results.
- **D-07:** Flat access via `rows(searchIndex)` returns `ResultGroup<SearchResultRow>` — same pattern as `QueryResult.rows(queryIndex)`.
- **D-08:** Grouped access via `groups(searchIndex)` returns `List<SearchResultGroup>` where each group has `getKey()` (the group metadata value) and `rows()` returning `ResultGroup<SearchResultRow>`.
- **D-09:** `isGrouped()` method makes the response self-describing — no magic, no auto-flattening, no runtime surprises.
- **D-10:** Column-oriented accessors preserved for QueryResult consistency: `getIds()`, `getDocuments()`, `getMetadatas()`, `getEmbeddings()`, `getScores()` (not `getDistances()`).
- **D-11:** `SearchResultRow` extends `ResultRow`, adds `getScore()` returning `Float` (null if not included). Scores are relevance scores from the search endpoint (not distances).
- **D-12:** Dual access (column-oriented + row-oriented) matches existing QueryResult/GetResult pattern for familiarity.

### Field Projection (Select)
- **D-13:** Search uses `Select` class exclusively — no `Include` enum on search builders. Clean separation matching Chroma core and chroma-go.
- **D-14:** Standard field constants: `Select.DOCUMENT` (`#document`), `Select.SCORE` (`#score`), `Select.EMBEDDING` (`#embedding`), `Select.METADATA` (`#metadata`), `Select.ID` (`#id`).
- **D-15:** Custom metadata key projection via `Select.key("fieldName")` — returns just that metadata field, not the whole blob. Equivalent to Go's `K("fieldName")`.
- **D-16:** `select()` is per-search (on the `Search` builder), not global on SearchBuilder. Each search in a batch can project different fields.
- **D-17:** `selectAll()` convenience method sets all 5 standard fields.
- **D-18:** Wire format: `{"select": {"keys": ["#document", "#score", "title"]}}` — matches Chroma API spec exactly.

### Sparse Vector Support
- **D-19:** `SparseVector` value type (indices + values) created in Phase 3 as an immutable value object.
- **D-20:** `Knn.querySparseVector(SparseVector)` available in Phase 3 — search API ships with full KNN input type support.
- **D-21:** Actual `SparseEmbeddingFunction` implementations (BM25, Splade, etc.) deferred to Phase 4 or later. Phase 3 only creates the type and wires it into KNN.

### Claude's Discretion
- DTO structure and serialization details (ChromaDtos inner classes, Gson annotations)
- HTTP path construction in ChromaApiPaths
- Builder inner class implementation details in ChromaHttpCollection
- Test scaffolding structure and helpers
- Exact GroupBy builder API shape (following Go patterns)
- ReadLevel enum values and wire format
- RRF builder details (ranks, weights, k parameter)

</decisions>

<specifics>
## Specific Ideas

- Simple KNN search should look nearly identical to query(): `collection.search().queryText("foo").limit(3).execute()` vs `collection.query().queryTexts("foo").nResults(3).execute()`
- Go client's `K("fieldName")` pattern maps to Java's `Select.key("fieldName")` — readable, type-safe, extensible
- The response shape from Chroma is always `[][]` nested arrays regardless of groupBy — the Java client provides typed access over this uniform wire format
- Per the Chroma wire format, search uses `filter` (not `where`) as the JSON key, `rank` for ranking expressions, `select` for projection, `limit` for pagination
- RRF supports `weights` array and `k` parameter (default 60) per Chroma docs

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Chroma Search API (upstream spec)
- https://docs.trychroma.com/cloud/search-api/overview — Search API overview, request structure, ranking expressions
- https://docs.trychroma.com/cloud/search-api/pagination-selection — Field selection with select, pagination with limit/offset
- https://docs.trychroma.com/cloud/search-api/hybrid-search — RRF hybrid search, rank composition, weights
- https://docs.trychroma.com/cloud/search-api/ranking — KNN ranking, query types (text, dense, sparse)

### Go client reference implementation
- https://github.com/amikos-tech/chroma-go — Reference implementation for API parity
- https://go-client.chromadb.dev/search/ — Go client search API docs
- Key files: `pkg/api/v2/search.go` (Search, Knn, Rrf, Key, Select), `pkg/api/v2/results.go` (ResultRow, SearchResult)

### Existing Java client patterns
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — QueryBuilder pattern to follow
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — Builder impl pattern (inner classes, execute())
- `src/main/java/tech/amikos/chromadb/v2/QueryResult.java` — Result type pattern (column + row access)
- `src/main/java/tech/amikos/chromadb/v2/ResultRow.java` — Base row interface (SearchResultRow extends this)
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — Filter DSL (reused in search)
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` — DTO patterns (Gson, SerializedName)
- `src/main/java/tech/amikos/chromadb/v2/Include.java` — NOT used in search, but reference for how query handles field selection

### Requirements
- `.planning/REQUIREMENTS.md` §Search API — SEARCH-01 through SEARCH-04

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ResultRow` interface: SearchResultRow extends this, adding `getScore()`
- `ResultGroup<R>` interface: Used for both flat and grouped result access
- `QueryResultImpl.from(dto)` pattern: SearchResultImpl will follow same DTO-to-immutable conversion
- `Where` / `WhereDocument` DSL: Reused directly in search filters (per-search and global)
- `ChromaApiClient.post()`: HTTP layer ready — search is just a new POST path
- `ChromaApiPaths`: Add `collectionSearch()` path builder

### Established Patterns
- Fluent builder with `execute()` terminal: SearchBuilder follows this exactly
- Inner class builders in ChromaHttpCollection: SearchBuilderImpl, SearchImpl, KnnImpl, etc.
- DTO inner classes in ChromaDtos: SearchRequest, SearchResponse
- Immutable value objects with builder: SparseVector, Select, GroupBy, ReadLevel
- Column + row dual access on results: SearchResult mirrors QueryResult

### Integration Points
- `Collection.java` interface: Add `SearchBuilder search()` method
- `ChromaHttpCollection.java`: Add SearchBuilderImpl inner class and Search/Knn/Rrf builders
- `ChromaDtos.java`: Add SearchRequest/SearchResponse DTOs
- `ChromaApiPaths.java`: Add search endpoint path
- Phase 5 plan 05-02: Consumes Search API types for cloud integration tests (currently blocked on this phase)

</code_context>

<deferred>
## Deferred Ideas

- `SparseEmbeddingFunction` interface and implementations (BM25, Splade) — Phase 4 (EMB-05)
- Arithmetic combinators on rank expressions (multiply, add for score transformation) — evaluate if needed post-MVP
- `queryImage()` / multimodal KNN input — Phase 4 (EMB-06)
- Search result caching / client-side pagination helpers — post-milestone

</deferred>

---

*Phase: 03-search-api*
*Context gathered: 2026-03-22*
