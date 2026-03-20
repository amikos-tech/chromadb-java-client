# Phase 1: Result Ergonomics & WhereDocument - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Add row-based iteration on query/get results (`ResultRow`, `ResultGroup`, `rows()`, `get(index)`) and complete the WhereDocument typed filter helpers (`contains`, `notContains`, `regex`, `notRegex`, `and`, `or`). Improving daily-use ergonomics for the v2 API. No new API endpoints or server interactions — this is a client-side convenience layer over existing column-oriented results and existing Chroma filter parameters.

</domain>

<decisions>
## Implementation Decisions

### ResultRow type hierarchy
- **D-01:** `ResultRow` is an **interface** (not concrete class) with package-private implementation, matching the `QueryResult`/`GetResult` pattern.
- **D-02:** `QueryResultRow extends ResultRow` adds `Float getDistance()` — type-safe separation, distance is never present on get results.
- **D-03:** Fields: `getId()`, `getDocument()`, `getMetadata()`, `getEmbedding()`, `getUri()` on `ResultRow`; `getDistance()` on `QueryResultRow`.
- **D-04:** Fields not requested via `Include` return **`null`** — consistent with existing column-oriented getters. No `Optional`, no exceptions.
- **D-05:** URI is included as a field, mirroring Python and Go Chroma clients.

### ResultGroup intermediate type
- **D-06:** `rows()` returns `ResultGroup<R extends ResultRow>`, not a raw `List`. This intermediate type enables both indexed access and streaming.
- **D-07:** `ResultGroup<R>` implements `Iterable<R>` and provides: `get(int index)`, `size()`, `isEmpty()`, `stream()`, `toList()`.
- **D-08:** `get(int index)` follows Java `List` convention — throws `IndexOutOfBoundsException` on invalid index.
- **D-09:** `ResultGroup` is an interface with package-private implementation.

### Result-level row access
- **D-10:** `GetResult.rows()` takes **no argument** — always one group (simple API for the common case).
- **D-11:** `QueryResult.rows(int queryIndex)` **always requires an index** — explicit about which query input's results.
- **D-12:** `QueryResult.groupCount()` returns number of query inputs.
- **D-13:** `QueryResult.stream()` returns `Stream<ResultGroup<QueryResultRow>>` — enables streaming over all query groups with `flatMap` patterns.
- **D-14:** No flat `rows()` without index on `QueryResult` — avoids ambiguity about which query's results are returned.

### WhereDocument operator scope
- **D-15:** Implement **all six** operators: `contains`, `notContains`, `regex`, `notRegex`, `and`, `or` — not just the two in the requirement. Leaving stubs that throw `UnsupportedOperationException` is poor DX.
- **D-16:** Both **static factory and instance chaining** for `and`/`or`, mirroring the `Where` class exactly.
- **D-17:** **No client-side regex validation** for `regex`/`notRegex` — pass through to Chroma. Different regex flavors between Java and the server make client validation unreliable.
- **D-18:** Javadoc on `WhereDocument.contains()` must clarify the distinction from `Where.documentContains()` — the former is the `where_document` API parameter (self-hosted filter), the latter is the inline `#document` metadata filter (Chroma Cloud).

### Claude's Discretion
- Immutability implementation details for `ResultRow` and `ResultGroup` (defensive copies, unmodifiable collections)
- Package-private impl class naming (`ResultRowImpl`, `QueryResultRowImpl`, `ResultGroupImpl` or similar)
- Whether `ResultGroup` also implements `RandomAccess` marker interface
- Test structure and assertion style for new types
- `WhereDocument` inner class naming and structure (follow `Where.MapWhere` pattern)

</decisions>

<specifics>
## Specific Ideas

- Go client's `SearchResult.Rows()` is the closest precedent for row access — but Java version goes further by adding it to get/query (not just search), making it a differentiator over both Python (no row access) and Go (search-only row access).
- `ResultGroup` should feel like a lightweight read-only collection — Java developers should immediately recognize `get()`, `size()`, `stream()`, `Iterable` without learning a new abstraction.
- The `Stream` at both levels enables idiomatic patterns like `result.stream().flatMap(ResultGroup::stream)` for cross-query flattening.

</specifics>

<canonical_refs>
## Canonical References

### Result types
- `src/main/java/tech/amikos/chromadb/v2/QueryResult.java` — Current column-oriented query result interface
- `src/main/java/tech/amikos/chromadb/v2/GetResult.java` — Current column-oriented get result interface
- `src/main/java/tech/amikos/chromadb/v2/QueryResultImpl.java` — Query result implementation with immutability patterns
- `src/main/java/tech/amikos/chromadb/v2/GetResultImpl.java` — Get result implementation with immutability patterns
- `src/main/java/tech/amikos/chromadb/v2/Include.java` — Include enum (EMBEDDINGS, DOCUMENTS, METADATAS, DISTANCES, URIS)

### Filter DSL
- `src/main/java/tech/amikos/chromadb/v2/WhereDocument.java` — WhereDocument with stubs throwing UnsupportedOperationException
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — Fully implemented Where class — blueprint for WhereDocument implementation

### Collection API
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — Collection interface with QueryBuilder, GetBuilder definitions

### Tests
- `src/test/java/tech/amikos/chromadb/v2/WhereDocumentTest.java` — Existing WhereDocument tests (verifies stubs throw)
- `src/test/java/tech/amikos/chromadb/v2/WhereTest.java` — Where tests — pattern reference for WhereDocument tests
- `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` — Integration tests showing current column-oriented result access

### External references
- Chroma Python `GetResult`/`QueryResult` types: column-oriented, no row access
- Chroma Go `SearchResult.Rows()`: row access with projection keys (KID, KDocument, KScore)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Where` class: Complete implementation of typed filter DSL with static factories, instance chaining, `MapWhere` inner class, immutability helpers — direct blueprint for WhereDocument
- `QueryResultImpl` / `GetResultImpl`: Factory methods from DTOs, defensive copy helpers — extend to produce `ResultGroup` instances
- `Include` enum: Already defines which fields can be requested — `ResultRow` null behavior is driven by this

### Established Patterns
- **Immutability**: All v2 value objects use private constructors, factory methods, defensive copies, `Collections.unmodifiableList()`
- **Interface-first**: Public types are interfaces (`Collection`, `Client`, `QueryResult`, `GetResult`); implementations are package-private
- **Filter DSL**: Abstract class → private inner `Map*` implementation → `toMap()` for serialization
- **Builder termination**: Fluent builders end with `execute()` returning result type

### Integration Points
- `QueryResult` interface: Add `rows(int)`, `groupCount()`, `stream()` methods
- `GetResult` interface: Add `rows()` method
- `QueryResultImpl` / `GetResultImpl`: Implement new methods, construct `ResultGroup` from existing column data
- `WhereDocument`: Replace `UnsupportedOperationException` stubs with real implementations
- `Collection.QueryBuilder` / `Collection.GetBuilder`: Already accept `WhereDocument` — no changes needed

</code_context>

<deferred>
## Deferred Ideas

- `data` field on ResultRow (Python has generic `data` for multimodal) — belongs in Phase 4 (Embedding Ecosystem)
- `SearchResult` with `SearchResultRow` having `getScore()` — belongs in Phase 3 (Search API), will reuse `ResultRow` and `ResultGroup` from this phase
- `included()` field tracking which fields were requested (Python pattern) — nice-to-have, not in requirements

</deferred>

---

*Phase: 01-result-ergonomics-wheredocument*
*Context gathered: 2026-03-20*
