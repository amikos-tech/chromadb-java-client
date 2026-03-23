# Phase 3: Search API - Research

**Researched:** 2026-03-22
**Domain:** ChromaDB v1.5+ Search endpoint — Java client implementation
**Confidence:** HIGH

## Summary

Phase 3 implements the Chroma Search endpoint (`/api/v2/.../search`) in the Java v2 client, matching Go client capabilities. The Search API is distinct from Query: it uses a different wire format (`searches[]` envelope, `rank` instead of embeddings, `filter` not `where`, `select` instead of `include`, scores not distances), and it is Cloud-only in practice. All decisions in CONTEXT.md are locked; research focuses on verifying wire format, Go client structural patterns, and integration test design.

The existing codebase provides strong patterns to follow. The `QueryBuilder`/`QueryResult`/`QueryResultImpl`/`QueryResultRowImpl` chain is the exact pattern to replicate for `SearchBuilder`/`SearchResult`/`SearchResultImpl`/`SearchResultRowImpl`. All infrastructure (Gson serialization, OkHttp transport, ChromaApiPaths, ChromaDtos, inner-builder classes) is in place and ready to extend.

**Primary recommendation:** Model Phase 3 types directly on Go client struct shapes. The Java implementation is a translation, not a redesign — match field names, nesting depth, and JSON key names from Go's MarshalJSON output.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Search Builder API Shape**
- D-01: Hybrid approach — convenience shortcuts on SearchBuilder for simple KNN (`queryText()`, `queryEmbedding()` directly on the builder), explicit `Search` objects via `searches(Search...)` for batch and complex cases.
- D-02: Simple KNN case must be as frictionless as possible: `collection.search().queryText("headphones").limit(3).execute()`.
- D-03: Batch search is first-class: `collection.search().searches(search1, search2).limit(5).execute()`.
- D-04: Both per-search filters and global filters supported; they combine (AND) when both present.
- D-05: Naming: `limit()` and `offset()` (not `nResults()`).

**Result Type Design**
- D-06: Single `SearchResult` interface — no compile-time split between grouped and ungrouped.
- D-07: Flat access via `rows(searchIndex)` returns `ResultGroup<SearchResultRow>`.
- D-08: Grouped access via `groups(searchIndex)` returns `List<SearchResultGroup>`.
- D-09: `isGrouped()` method makes the response self-describing.
- D-10: Column-oriented accessors preserved: `getIds()`, `getDocuments()`, `getMetadatas()`, `getEmbeddings()`, `getScores()`.
- D-11: `SearchResultRow` extends `ResultRow`, adds `getScore()` returning `Float` (null if not included).
- D-12: Dual access (column-oriented + row-oriented) matches existing QueryResult/GetResult pattern.

**Field Projection (Select)**
- D-13: Search uses `Select` class exclusively — no `Include` enum on search builders.
- D-14: Standard field constants: `Select.DOCUMENT` (`#document`), `Select.SCORE` (`#score`), `Select.EMBEDDING` (`#embedding`), `Select.METADATA` (`#metadata`), `Select.ID` (`#id`).
- D-15: Custom metadata key projection via `Select.key("fieldName")`.
- D-16: `select()` is per-search (on `Search` builder), not global on SearchBuilder.
- D-17: `selectAll()` convenience method sets all 5 standard fields.
- D-18: Wire format: `{"select": {"keys": ["#document", "#score", "title"]}}`.

**Sparse Vector Support**
- D-19: `SparseVector` value type (indices + values) as an immutable value object in Phase 3.
- D-20: `Knn.querySparseVector(SparseVector)` available in Phase 3.
- D-21: `SparseEmbeddingFunction` implementations deferred to Phase 4.

### Claude's Discretion
- DTO structure and serialization details (ChromaDtos inner classes, Gson annotations)
- HTTP path construction in ChromaApiPaths
- Builder inner class implementation details in ChromaHttpCollection
- Test scaffolding structure and helpers
- Exact GroupBy builder API shape (following Go patterns)
- ReadLevel enum values and wire format
- RRF builder details (ranks, weights, k parameter)

### Deferred Ideas (OUT OF SCOPE)
- `SparseEmbeddingFunction` interface and implementations (BM25, Splade) — Phase 4 (EMB-05)
- Arithmetic combinators on rank expressions (multiply, add for score transformation) — evaluate if needed post-MVP
- `queryImage()` / multimodal KNN input — Phase 4 (EMB-06)
- Search result caching / client-side pagination helpers — post-milestone
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SEARCH-01 | User can execute `collection.search()` with KNN ranking (queryText, queryVector, querySparseVector) and get typed `SearchResult`. | Wire format for KNN rank confirmed. QueryBuilder pattern is direct template. API path: `{collectionId}/search`. |
| SEARCH-02 | User can compose RRF from multiple weighted rank expressions. | RRF structure confirmed: `{"rrf": {"ranks": [...], "k": 60}}`. Weights per rank, normalize flag. |
| SEARCH-03 | User can project specific fields (`#id`, `#document`, `#embedding`, `#score`, `#metadata`, custom keys) in search results. | Select wire format confirmed: `{"select": {"keys": ["#document", "#score", "myfield"]}}`. |
| SEARCH-04 | User can group search results by metadata key with min/max K controls, and specify read level (INDEX_AND_WAL vs INDEX_ONLY). | ReadLevel wire values: `"index_and_wal"` / `"index_only"`. GroupBy per Go patterns. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Gson | Already in project | JSON serialization of search DTOs | Established pattern throughout all ChromaDtos |
| OkHttp | Already in project | HTTP transport for search POST | All existing API calls use ChromaApiClient.post() |
| JUnit 4 | Already in project | Unit and integration tests | Established project test framework |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| TestContainers chromadb | Already in project | Integration tests against Chroma >= 1.5 | Search requires 1.5+; use `assumeMinVersion("1.5.0")` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Gson @SerializedName + custom MarshalJSON style | Jackson | Gson is already project standard — no switch |
| Inner builder classes in ChromaHttpCollection | Separate top-level impl classes | Inner classes are established project pattern |

**No installation required.** All dependencies are already in pom.xml.

## Architecture Patterns

### New Types Required

```
src/main/java/tech/amikos/chromadb/v2/
├── SparseVector.java          # Immutable value object: int[] indices, float[] values
├── Select.java                # Projection: DOCUMENT, SCORE, EMBEDDING, METADATA, ID + key(String)
├── Search.java                # Per-search builder interface (knn/rrf, filter, select, groupBy, limit/offset)
├── Knn.java                   # KNN rank: queryText/queryEmbedding/querySparseVector, key, limit, default, returnRank
├── Rrf.java                   # RRF rank: ranks[], k, normalize
├── GroupBy.java               # GroupBy: key, minK, maxK
├── ReadLevel.java             # Enum: INDEX_AND_WAL, INDEX_ONLY
├── SearchResult.java          # Interface: rows(searchIndex), groups(searchIndex), isGrouped(), column accessors
├── SearchResultRow.java       # Interface extends ResultRow: getScore() returning Float
└── SearchResultGroup.java     # Interface: getKey(), rows() returning ResultGroup<SearchResultRow>
```

Plus additions to existing files:
```
src/main/java/tech/amikos/chromadb/v2/
├── Collection.java            # Add: SearchBuilder search()
├── ChromaHttpCollection.java  # Add: SearchBuilderImpl inner class + Search/Knn/Rrf impl inner classes
├── ChromaDtos.java            # Add: SearchRequest, SearchResponse DTOs
├── ChromaApiPaths.java        # Add: collectionSearch() path builder
```

### Pattern 1: Inner Builder in ChromaHttpCollection (established pattern)
**What:** Each builder is a private final inner class within ChromaHttpCollection, implementing the public interface.
**When to use:** All record operation builders follow this — AddBuilderImpl, QueryBuilderImpl, etc.
**Example:**
```java
// Source: src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java (QueryBuilderImpl pattern)
private final class SearchBuilderImpl implements Collection.SearchBuilder {
    private List<Search> searches;
    private Where globalWhere;
    private Integer globalLimit;
    private Integer globalOffset;
    private ReadLevel readLevel;

    @Override
    public Collection.SearchBuilder searches(Search... searches) {
        this.searches = Arrays.asList(searches);
        return this;
    }

    @Override
    public Collection.SearchBuilder queryText(String text) {
        // convenience shortcut -- creates a single Search with Knn internally
        this.searches = Collections.singletonList(Search.builder().knn(Knn.queryText(text)).build());
        return this;
    }

    @Override
    public SearchResult execute() {
        String path = ChromaApiPaths.collectionSearch(tenant.getName(), database.getName(), id);
        ChromaDtos.SearchResponse dto = apiClient.post(path,
            buildRequest(), ChromaDtos.SearchResponse.class);
        return SearchResultImpl.from(dto);
    }
}
```

### Pattern 2: DTO with Gson @SerializedName (established pattern)
**What:** Package-private static inner classes in ChromaDtos with Gson annotations for wire format control.
**When to use:** All request/response JSON structures are modeled as ChromaDtos inner classes.

```java
// Source: src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java (established DTO pattern)
static final class SearchRequest {
    final List<SearchItem> searches;
    @SerializedName("read_level")
    final String readLevel;  // "index_and_wal" or "index_only"

    SearchRequest(List<SearchItem> searches, String readLevel) {
        this.searches = searches;
        this.readLevel = readLevel;
    }
}

static final class SearchItem {
    final Map<String, Object> filter;   // serialized Where + IDIn
    final Object rank;                   // KnnDto or RrfDto
    final SearchSelectDto select;
    final SearchPageDto limit;
    @SerializedName("group_by")
    final GroupByDto groupBy;
}
```

### Pattern 3: DTO-to-immutable-result conversion (established pattern)
**What:** `SearchResultImpl.from(ChromaDtos.SearchResponse dto)` — converts raw DTO into immutable result object.
**When to use:** Matches `QueryResultImpl.from(ChromaDtos.QueryResponse dto)` exactly.

```java
// Source: src/main/java/tech/amikos/chromadb/v2/QueryResultImpl.java (from() pattern)
static SearchResultImpl from(ChromaDtos.SearchResponse dto) {
    if (dto.ids == null) {
        throw new ChromaDeserializationException(
            "Server returned search result without required ids field", 200);
    }
    // ... convert and wrap
    return new SearchResultImpl(dto.ids, dto.documents, dto.metadatas, embeddings, dto.scores);
}
```

### Pattern 4: ResultRow composition (established pattern)
**What:** `SearchResultRowImpl` delegates base `ResultRow` fields to a composed `ResultRowImpl`.
**When to use:** Matches `QueryResultRowImpl` exactly — extends via composition, not inheritance.

```java
// Source: src/main/java/tech/amikos/chromadb/v2/QueryResultRowImpl.java (composition pattern)
final class SearchResultRowImpl implements SearchResultRow {
    private final ResultRowImpl base;
    private final Float score;

    @Override public String getId() { return base.getId(); }
    @Override public Float getScore() { return score; }
    // ...
}
```

### Anti-Patterns to Avoid
- **Using `Include` enum in Search builders:** Search uses `Select` class only (D-13). `Include` is query/get territory.
- **Naming the result field `distances`:** Search returns `scores` (relevance, higher=better). `distances` is query-only.
- **Flat HTTP path omitting the `search` suffix:** The endpoint is `{collectionById}/search`, not `/query`.
- **Single SearchResult for grouped vs flat:** Keep single `SearchResult` interface with `isGrouped()` (D-06/D-09).
- **Auto-flattening grouped results:** Grouped results return via `groups()`, flat via `rows()`. No magic (D-09).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON serialization of rank expressions | Custom serializer with reflection | Gson with explicit DTO classes (KnnDto, RrfDto) | Gson is already used; explicit DTOs are testable and type-safe |
| HTTP transport | New HTTP client | `ChromaApiClient.post(path, body, responseClass)` | Already handles errors, auth, timeout |
| Filter serialization | New filter to map code | `where.toMap()` (already exists on `Where` class) | Where DSL already serializes correctly |
| Embedding list conversion | New float[] ↔ List<Float> conversion | `ChromaDtos.toFloatList()` and `ChromaDtos.toFloatArray()` | These helpers already exist and are tested |
| Immutable list wrapping | Custom immutable collection | `Collections.unmodifiableList(new ArrayList<>(...))` | Pattern established in QueryResultImpl |
| Test container setup | Custom Docker management | `AbstractChromaIntegrationTest` base class | TestContainers setup already abstracted |

**Key insight:** The entire infrastructure layer (transport, auth, error handling, deserialization, test containers) is already built. Phase 3 is purely additive — new types and a new HTTP path.

## Wire Format Reference

This is the authoritative wire format for the Chroma Search endpoint, verified from Go client MarshalJSON methods and Chroma docs.

### Request envelope (POST `/api/v2/tenants/{t}/databases/{d}/collections/{id}/search`)

```json
{
  "searches": [
    {
      "rank": { "knn": { "query": "headphones", "key": "#embedding", "limit": 10 } },
      "filter": { "#id": { "$in": ["a","b"] }, "category": { "$eq": "electronics" } },
      "select": { "keys": ["#id", "#score", "#document"] },
      "limit": { "limit": 5, "offset": 0 },
      "group_by": { "key": "category", "min_k": 1, "max_k": 3 }
    }
  ],
  "read_level": "index_and_wal"
}
```

### KNN rank object
```json
{ "knn": { "query": "text query here",      "key": "#embedding", "limit": 10 } }
{ "knn": { "query": [0.1, 0.2, 0.3],        "key": "#embedding", "limit": 10 } }
{ "knn": { "query": {"indices":[1,5],"values":[0.3,0.7]}, "key": "sparse_field" } }
```

### RRF rank object
```json
{
  "rrf": {
    "ranks": [
      { "rank": { "knn": { "query": "audio", "return_rank": true } }, "weight": 0.7 },
      { "rank": { "knn": { "query": "wireless", "return_rank": true } }, "weight": 0.3 }
    ],
    "k": 60,
    "normalize": false
  }
}
```

### Select object
```json
{ "select": { "keys": ["#id", "#document", "#score", "#embedding", "#metadata", "custom_field"] } }
```

### GroupBy object (wire format inferred from Go patterns)
```json
{ "group_by": { "key": "category", "min_k": 1, "max_k": 3 } }
```

### ReadLevel wire values
- `"index_and_wal"` — default, includes WAL (all writes visible)
- `"index_only"` — fastest, only indexed records visible

### Response structure
```json
{
  "ids":        [["id1", "id2"], ["id3"]],
  "documents":  [["doc1", "doc2"], ["doc3"]],
  "metadatas":  [[{"k":"v"}, {"k":"v2"}], [{"k":"v3"}]],
  "embeddings": null,
  "scores":     [[0.95, 0.83], [0.77]]
}
```

Response is always `[][]` nested arrays — outer index = search index (for batch), inner index = result row. Grouped results also return in this same flat wire shape; grouping metadata is TBD based on actual server response (see Open Questions).

## Common Pitfalls

### Pitfall 1: Confusing `filter` vs `where` JSON key
**What goes wrong:** The search endpoint uses `"filter"` as the JSON key; the query endpoint uses `"where"`. Using `where` in search requests will be silently ignored or cause a bad request.
**Why it happens:** The `Where` class is reused for search filters, but the JSON key changes.
**How to avoid:** In `SearchItem` DTO, use field name `filter` (not `where`). Apply `Where.toMap()` to get the map, put it under `"filter"` key.
**Warning signs:** Server returns empty results where non-empty expected, or 400 bad request on filter operations.

### Pitfall 2: Using `float` instead of `double` for scores
**What goes wrong:** Scores in the response are `float64` in Go / `double` in JSON. If deserialized as `float`, precision is lost.
**Why it happens:** `QueryResult` uses `List<Float>` for distances; copy-paste risk.
**How to avoid:** Use `List<Double>` for scores in `SearchResponse` DTO and `List<List<Double>>` for all search score fields. The `SearchResultRow.getScore()` returns `Float` per D-11 (user API), but internally handle as `Double` and downcast.
**Warning signs:** Scores appear as very slightly different values than expected.

### Pitfall 3: Forgetting `return_rank: true` on KNN inside RRF
**What goes wrong:** RRF scoring requires each constituent KNN to return rank positions, not distances. Without `"return_rank": true`, RRF results are incorrect or empty.
**Why it happens:** `return_rank` is only needed when a KNN is used as an RRF input. It is silently ignored for direct KNN search.
**How to avoid:** When building RRF DTOs, always set `return_rank: true` on inner KNN objects. The `Rrf.builder()` should auto-set this on wrapped Knn instances.
**Warning signs:** RRF returns 0 results or all scores are equal.

### Pitfall 4: IDIn/IDNotIn conflicts with global Where filter
**What goes wrong:** Per D-04, per-search filters and global filters combine with AND. If both include IDIn clauses pointing to disjoint sets, the result is empty.
**Why it happens:** The wire format merges filter maps; if both have `"#id"` key, one will overwrite the other during serialization.
**How to avoid:** When merging per-search and global filters in the DTO, detect `"#id"` key conflicts and either raise an IllegalArgumentException or prefer the per-search filter. Document this behavior in Javadoc.
**Warning signs:** Empty results when combining IDIn filters.

### Pitfall 5: PublicInterfaceCompatibilityTest will fail
**What goes wrong:** `PublicInterfaceCompatibilityTest` counts methods on `Collection` interface and will fail when `search()` is added.
**Why it happens:** `EXPECTED_COLLECTION_METHOD_COUNT = 21` — adding `search()` makes it 22.
**How to avoid:** Update `EXPECTED_COLLECTION_METHOD_COUNT` in `PublicInterfaceCompatibilityTest` when adding `SearchBuilder search()`.
**Warning signs:** `testCollectionInterfaceMethodCount` test fails.

### Pitfall 6: Search is Cloud-only in practice (Chroma >= 1.5)
**What goes wrong:** Running search integration tests against self-hosted Chroma < 1.5 will return 404 or 405.
**Why it happens:** The Search endpoint was added in Chroma 1.5. Self-hosted tests use TestContainers.
**How to avoid:** Add `assumeMinVersion("1.5.0")` in all search integration tests. The default container version in `AbstractChromaIntegrationTest` is `1.5.5` so this should pass, but add the guard for matrix test safety.
**Warning signs:** 404 Not Found on POST to `/search` path.

## Code Examples

### Simple KNN search (D-02)
```java
// Source: Pattern derived from QueryBuilderImpl in ChromaHttpCollection.java
SearchResult result = collection.search()
    .queryText("wireless headphones")
    .limit(5)
    .execute();

for (SearchResultRow row : result.rows(0)) {
    System.out.println(row.getId() + " score=" + row.getScore());
}
```

### Batch search (D-03)
```java
// Source: Pattern derived from Go client SearchQuery{Searches: [...]]}
Search s1 = Search.builder().knn(Knn.queryText("headphones")).limit(3).build();
Search s2 = Search.builder().knn(Knn.queryText("organic tea")).limit(3).build();
SearchResult result = collection.search().searches(s1, s2).execute();

ResultGroup<SearchResultRow> results0 = result.rows(0); // headphones
ResultGroup<SearchResultRow> results1 = result.rows(1); // organic tea
```

### RRF hybrid search (SEARCH-02)
```java
// Source: Go RrfRank MarshalJSON → {"rrf":{"ranks":[...],"k":60}}
Knn knn1 = Knn.queryText("wireless audio").returnRank(true);
Knn knn2 = Knn.queryText("noise cancelling headphones").returnRank(true);
Rrf rrf = Rrf.builder().ranks(knn1, 0.7f).ranks(knn2, 0.3f).k(60).build();
SearchResult result = collection.search()
    .searches(Search.builder().rrf(rrf).limit(5).build())
    .execute();
```

### Field projection with Select (SEARCH-03, D-13 to D-18)
```java
// Source: Select wire format {"select":{"keys":["#id","#score","category"]}}
Search s = Search.builder()
    .knn(Knn.queryText("headphones"))
    .select(Select.ID, Select.SCORE, Select.key("category"))
    .limit(5)
    .build();
SearchResult result = collection.search().searches(s).execute();
// result.rows(0).get(0).getScore() is populated
// result.rows(0).get(0).getDocument() is null (not selected)
```

### GroupBy search (SEARCH-04, D-08)
```java
// Source: Go GroupBy pattern; wire format {"group_by":{"key":"category","min_k":1,"max_k":3}}
Search s = Search.builder()
    .knn(Knn.queryText("product"))
    .groupBy(GroupBy.builder().key("category").minK(1).maxK(3).build())
    .limit(15)
    .build();
SearchResult result = collection.search().searches(s).execute();
assertTrue(result.isGrouped());
for (SearchResultGroup group : result.groups(0)) {
    System.out.println("Group: " + group.getKey() + " count=" + group.rows().size());
}
```

### ReadLevel (SEARCH-04)
```java
// Source: Go ReadLevel → "index_and_wal" / "index_only"
SearchResult result = collection.search()
    .queryText("headphones")
    .readLevel(ReadLevel.INDEX_AND_WAL)
    .limit(5)
    .execute();
```

### Sparse vector KNN (SEARCH-01, D-20)
```java
// Source: Go KnnRank query with SparseVector → {"indices":[1,5],"values":[0.3,0.7]}
SparseVector sv = SparseVector.of(new int[]{1, 5, 10}, new float[]{0.3f, 0.7f, 0.2f});
Search s = Search.builder()
    .knn(Knn.querySparseVector(sv).key("sparse_field"))
    .limit(5)
    .build();
```

### ChromaDtos.SearchRequest structure (for implementer)
```java
// Source: Claude's discretion per CONTEXT.md — follows established ChromaDtos patterns
static final class SearchRequest {
    final List<SearchItemDto> searches;
    @SerializedName("read_level")
    final String readLevel;
}

static final class SearchItemDto {
    final Map<String, Object> filter;  // from Where.toMap()
    final Object rank;                  // KnnDto or RrfDto (must serialize polymorphically)
    final SearchSelectDto select;
    @SerializedName("limit")
    final SearchPageDto page;
    @SerializedName("group_by")
    final GroupByDto groupBy;
}

static final class KnnDto {
    final Object query;   // String, List<Float>, or SparseVectorDto
    final String key;     // "#embedding" or custom sparse field name
    final Integer limit;
    @SerializedName("default")
    final Double defaultScore;
    @SerializedName("return_rank")
    final Boolean returnRank;
}

static final class SparseVectorDto {
    final List<Integer> indices;
    final List<Float> values;
}

static final class RrfDto {
    final List<RrfRankItemDto> ranks;
    final Integer k;
    final Boolean normalize;
}

static final class RrfRankItemDto {
    final Object rank;   // KnnDto wrapped in {"knn":{...}} — needs custom serialization
    final Double weight;
}

static final class SearchSelectDto {
    final List<String> keys;
}

static final class SearchPageDto {
    final Integer limit;
    final Integer offset;
}

static final class GroupByDto {
    final String key;
    @SerializedName("min_k")
    final Integer minK;
    @SerializedName("max_k")
    final Integer maxK;
}

static final class SearchResponse {
    List<List<String>> ids;
    List<List<String>> documents;
    List<List<Map<String, Object>>> metadatas;
    List<List<List<Float>>> embeddings;
    List<List<Double>> scores;
}
```

**Serialization challenge:** The `rank` field in `SearchItemDto` must serialize as `{"knn":{...}}` or `{"rrf":{...}}`. Since Gson doesn't support polymorphism out of the box, use a custom Gson `TypeAdapter` or wrap KnnDto/RrfDto in an outer object with a named field. Alternatively, use `JsonObject` assembly directly. The Go client uses `MarshalJSON()` methods — the Java equivalent is a custom serializer.

**Recommended approach:** Create a `RankSerializer` (implements `JsonSerializer<Object>`) registered on the Gson instance used by `ChromaApiClient`, or use `Map<String, Object>` assembly for the rank field in `SearchItemDto`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Include enum for field selection | Select class with `#`-prefixed keys | Chroma 1.5 Search API | Search uses Select, query still uses Include |
| Single query endpoint | Separate search endpoint | Chroma 1.5 | POST to `/search`, distinct from `/query` |
| Distance scores (lower=better) | Relevance scores (higher=better) | Chroma 1.5 Search API | Score semantics are inverted vs query distances |
| QueryBuilder → `include(Include...)` | SearchBuilder → `select(Select...)` per-search | Phase 3 | Clean API separation per D-13 |

**Current production state:**
- Chroma default container in project: `1.5.5` (supports Search)
- `SearchApiCloudIntegrationTest.java` exists as a placeholder (class structure + seed collection + CLOUD-02/CLOUD-03 tests, but NO search method calls yet — Phase 5 plan 02 is blocked on Phase 3)
- `PublicInterfaceCompatibilityTest` counts `EXPECTED_COLLECTION_METHOD_COUNT = 21` — must be updated to 22 after adding `search()`

## Open Questions

1. **GroupBy wire format for min_k/max_k**
   - What we know: Go uses `GroupBy` with a key and aggregation strategy
   - What's unclear: Exact JSON field names (`min_k`/`max_k` vs `minK`/`maxK`), whether `min_k`/`max_k` are optional
   - Recommendation: Use `min_k`/`max_k` (snake_case matches all other Chroma API fields). Make both optional (no default in request if not set).

2. **Grouped response wire format**
   - What we know: The Go client SearchResultImpl uses `[][]` nested arrays in all cases; Go `buildRow()` handles grouped access
   - What's unclear: Does the server actually return a different structure for grouped results, or does the `ids[][]` simply have one entry per group in the inner array?
   - Recommendation: Implement `isGrouped()` based on whether `groupBy` was set in the request (track server-side grouping via a flag on SearchResultImpl), and treat the outer `ids[]` dimension as group index for grouped results.

3. **Rank polymorphic serialization strategy**
   - What we know: Gson doesn't natively support `{"knn":{...}}` vs `{"rrf":{...}}` discrimination
   - What's unclear: Whether to use a `JsonSerializer`, `Map<String,Object>` assembly, or `JsonObject` for rank field
   - Recommendation: Use `Map<String, Object>` assembly in builder implementations when constructing the request — convert Knn → `{"knn": knnMap}` and Rrf → `{"rrf": rrfMap}` directly, avoiding polymorphic Gson complexity. This is simpler than a TypeAdapter and follows the existing `where.toMap()` pattern.

4. **IDIn/IDNotIn in search filter format**
   - What we know: `Where.idIn()` serializes to `{"#id": {"$in": [...]}}` and `Where.toMap()` returns this
   - What's unclear: Whether Chroma Search accepts the same `where`-style filter format under the `"filter"` key, or if it has a different IDIn representation
   - Recommendation: Use `where.toMap()` output directly under `"filter"` key — this matches how Go client constructs SearchFilter from Where + IDIn. Verify in integration test.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (already in project) |
| Config file | none — Maven Surefire picks up `**/*Test.java` |
| Quick run command | `mvn test -Dtest=SearchResultTest,SelectTest,KnnTest,SparseVectorTest` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SEARCH-01 | KNN search returns ranked SearchResult | unit | `mvn test -Dtest=SearchApiUnitTest#testKnnQueryText` | ❌ Wave 0 |
| SEARCH-01 | KNN search with queryEmbedding | unit | `mvn test -Dtest=SearchApiUnitTest#testKnnQueryEmbedding` | ❌ Wave 0 |
| SEARCH-01 | KNN with sparse vector | unit | `mvn test -Dtest=SparseVectorTest` | ❌ Wave 0 |
| SEARCH-01 | Integration: KNN returns results | integration | `mvn test -Dtest=SearchApiIntegrationTest#testKnnSearch` | ❌ Wave 0 |
| SEARCH-02 | RRF builds correct DTO | unit | `mvn test -Dtest=SearchApiUnitTest#testRrfDtoStructure` | ❌ Wave 0 |
| SEARCH-02 | Integration: RRF returns ranked results | integration | `mvn test -Dtest=SearchApiIntegrationTest#testRrfSearch` | ❌ Wave 0 |
| SEARCH-03 | Select serializes correct keys | unit | `mvn test -Dtest=SelectTest` | ❌ Wave 0 |
| SEARCH-03 | Integration: projection excludes unselected fields | integration | `mvn test -Dtest=SearchApiIntegrationTest#testSelectProjection` | ❌ Wave 0 |
| SEARCH-04 | GroupBy builder creates correct DTO | unit | `mvn test -Dtest=SearchApiUnitTest#testGroupByDto` | ❌ Wave 0 |
| SEARCH-04 | ReadLevel enum values correct | unit | `mvn test -Dtest=SearchApiUnitTest#testReadLevelWireValues` | ❌ Wave 0 |
| SEARCH-04 | Integration: INDEX_AND_WAL vs INDEX_ONLY | integration | `mvn test -Dtest=SearchApiIntegrationTest#testReadLevel` | ❌ Wave 0 |
| SEARCH-04 | PublicInterface count updated | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | ✅ (needs update) |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=SearchApiUnitTest,SelectTest,SparseVectorTest`
- **Per wave merge:** `mvn test` (full suite)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiUnitTest.java` — covers SEARCH-01 through SEARCH-04 unit behaviors (DTO structure, wire format, serialization)
- [ ] `src/test/java/tech/amikos/chromadb/v2/SelectTest.java` — covers Select constants and key() projection
- [ ] `src/test/java/tech/amikos/chromadb/v2/SparseVectorTest.java` — covers SparseVector immutability and validation
- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiIntegrationTest.java` — integration tests against Chroma >= 1.5 via TestContainers (extends AbstractChromaIntegrationTest, uses `assumeMinVersion("1.5.0")`)

## Sources

### Primary (HIGH confidence)
- Go client `pkg/api/v2/search.go` (GitHub raw) — SearchQuery, SearchRequest, SearchSelect, SearchPage, SearchResultImpl struct definitions with JSON tags
- Go client `pkg/api/v2/rank.go` (GitHub raw) — KnnRank, RrfRank, RankWithWeight struct definitions and MarshalJSON format
- Chroma docs `docs.trychroma.com/cloud/search-api/ranking` — KNN query types, parameter semantics
- Chroma docs `docs.trychroma.com/cloud/search-api/hybrid-search` — RRF structure, weights, k parameter, normalize flag
- Chroma docs `docs.trychroma.com/cloud/search-api/pagination-selection` — Select field keys, limit/offset semantics
- Existing `ChromaHttpCollection.java` / `ChromaDtos.java` / `QueryResultImpl.java` — pattern authority for Java implementation

### Secondary (MEDIUM confidence)
- Go client `go-client.chromadb.dev/search/` — API doc page confirming KID/KDocument/KEmbedding/KMetadata/KScore constants and ReadLevel values
- Chroma docs `docs.trychroma.com/cloud/search-api/overview` — confirmed Cloud-only status, Search is v1.5+

### Tertiary (LOW confidence)
- GroupBy `min_k`/`max_k` field names — inferred from Go patterns and snake_case convention; not directly verified from a raw Go file

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — same libs already in project
- Wire format (KNN, RRF, Select, ReadLevel): HIGH — verified from Go MarshalJSON methods and official docs
- Wire format (GroupBy min_k/max_k): MEDIUM — inferred from Go client patterns, field names are snake_case educated guess
- Architecture: HIGH — direct translation of established QueryBuilder pattern
- Pitfalls: HIGH — derived from direct code inspection and known Gson limitations

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (30 days — Chroma Search API is now stable)
