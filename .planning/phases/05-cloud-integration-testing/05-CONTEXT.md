# Phase 5: Cloud Integration Testing - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Build deterministic cloud parity test suites that validate search, schema/index, and array metadata behavior against Chroma Cloud. Three requirements: CLOUD-01 (search parity), CLOUD-02 (schema/index), CLOUD-03 (array metadata). No new API surface — this phase adds cloud integration tests only.

</domain>

<decisions>
## Implementation Decisions

### Test suite structure
- **D-01:** Single test class for all Phase 5 cloud tests — no splitting per requirement. Extends the existing cloud test pattern.
- **D-02:** `Assume.assumeTrue()` gating on missing credentials (skip, don't fail) — consistent with chroma-go and core Chroma.
- **D-03:** Tests run in GitHub Actions CI with secrets, not manual-only.

### Data seeding strategy
- **D-04:** Shared realistic seed collection (10-20 records) for read-only tests, created once in `@BeforeClass`. Dataset models a realistic domain (e.g., product catalog with titles, categories, prices, tags).
- **D-05:** Isolated per-test collections for any test that mutates data (upsert, delete, schema changes).
- **D-06:** Seed data uses the default embedding function (server-side) — tests the full cloud path rather than explicit embeddings.

### Search parity (CLOUD-01)
- **D-07:** Cloud integration tests cover both KNN and RRF end-to-end — going beyond chroma-go baseline which only unit-tests RRF.
- **D-08:** Cloud integration tests cover GroupBy with MinK/MaxK aggregation end-to-end.
- **D-09:** Polling loop on `collection.indexingStatus()` to wait for indexing completion before search assertions — more deterministic than fixed sleep. Leverages Phase 2's `indexingStatus()` implementation.
- **D-10:** Batch search tested (multiple independent `Search` objects in one call) — batch is an important capability to validate in cloud.
- **D-11:** Explicit test for `Knn.limit` (candidate pool) vs `Search.limit` (final result count) distinction — e.g., KNN limit=10 but search limit=3 returns exactly 3.
- **D-12:** Read level tests: `INDEX_AND_WAL` asserts all records immediately (no polling wait), `INDEX_ONLY` asserts count <= total (index may not be compacted yet).

### Search filter combinations (CLOUD-01)
- **D-13:** Small but varied matrix of filter combinations covering:
  - Where metadata filter alone
  - IDIn / IDNotIn alone
  - DocumentContains / DocumentNotContains alone
  - IDNotIn + metadata filter combined
  - Where + DocumentContains combined
  - Where + IDIn + DocumentContains triple combination
- **D-14:** Pagination tests: basic limit, limit+offset (page 2), and client-side validation for obviously invalid inputs (e.g., limit=0, negative offset) that should fail without sending requests.

### Search projection (CLOUD-01)
- **D-15:** Test that selected fields are present and excluded fields are truly absent (null). E.g., select only `#id` + `#score`, assert `#document` is null.
- **D-16:** Test custom metadata key projection (select specific metadata keys by name, not just `#metadata` blob).

### Schema/index parity (CLOUD-02)
- **D-17:** Extend existing `testCloudConfigurationParityWithRequestAuthoritativeFallback()` pattern from `CloudParityIntegrationTest` — Phase 2 already covers HNSW/SPANN detection and config round-trips.
- **D-18:** Test distance space variants (cosine, l2, ip) — create collection with each, verify round-trip.
- **D-19:** Test invalid config transitions (e.g., attempt to change distance space after data inserted) — assert appropriate error response.
- **D-20:** Test HNSW and SPANN config paths independently — verify config round-trip for each index type.

### Array metadata (CLOUD-03)
- **D-21:** Test string, number, and bool arrays independently — each type gets its own records in seed data and dedicated assertions.
- **D-22:** Mixed-type arrays (e.g., `["foo", 42, true]`) must be rejected at the client level before sending to the server. No undefined behavior allowed. If client validation doesn't exist yet, add it.
- **D-23:** Round-trip assertions verify both values AND types. Floats must not become integers and vice versa. Test type fidelity explicitly.
- **D-24:** `contains`/`not_contains` filter edge cases all covered:
  - Contains on a single-element array
  - Contains where no documents match (empty result set)
  - Not_contains where all documents match (empty result set)
  - Contains on a metadata key that doesn't exist on some documents
- **D-25:** Empty arrays (`"tags": []`) tested for storage and retrieval — verify whether cloud preserves, drops, or nullifies them. Document the actual behavior regardless of outcome.

### Claude's Discretion
- Exact realistic seed data domain and field names
- Polling loop timeout and interval for `indexingStatus()` wait
- Test method naming conventions within the single class
- Order of test methods within the class
- Specific embedding dimension for seed data
- Whether to use `@FixMethodOrder` or rely on JUnit default ordering
- Exact filter combination matrix layout (which specific metadata fields to filter on)
- How to structure the `@BeforeClass` seed method (helper methods, constants, etc.)

</decisions>

<specifics>
## Specific Ideas

- Align with chroma-go's cloud test patterns where applicable: unique collection names with UUID suffix, best-effort cleanup in tearDown, credential loading from `.env` via `Utils.loadEnvFile()`
- chroma-go uses 2-second `time.Sleep` after data insertion before searching — Java should use `indexingStatus()` polling instead for determinism
- The `Knn.limit` vs `Search.limit` distinction is a documented source of user confusion — the test should make this crystal clear
- chroma-go has no cloud integration tests for RRF or GroupBy — Java gets ahead here
- ReadLevelIndexAndWAL test should deliberately skip the polling wait to verify WAL consistency (same pattern as chroma-go)

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Search API (CLOUD-01)
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — Search method signatures and builder API
- `src/main/java/tech/amikos/chromadb/v2/QueryResult.java` — Result structure (rows, row groups, at() accessor)
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — Filter DSL (in, nin, eq, gt, etc.)
- `src/main/java/tech/amikos/chromadb/v2/WhereDocument.java` — Document filter DSL (contains, notContains)
- Chroma Search API docs: https://docs.trychroma.com/cloud/search-api/overview

### Schema/Index (CLOUD-02)
- `src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java` — HNSW/SPANN parameters, builder
- `src/main/java/tech/amikos/chromadb/v2/UpdateCollectionConfiguration.java` — Config mutation
- `src/main/java/tech/amikos/chromadb/v2/Schema.java` — Schema structure, value types
- `src/main/java/tech/amikos/chromadb/v2/HnswIndexConfig.java` — HNSW index configuration
- `src/main/java/tech/amikos/chromadb/v2/VectorIndexConfig.java` — Vector index configuration
- `src/main/java/tech/amikos/chromadb/v2/DistanceFunction.java` — Distance space enum (cosine, l2, ip)

### Array metadata (CLOUD-03)
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — in/nin/contains/notContains operators
- `src/test/java/tech/amikos/chromadb/v2/WhereTest.java` — Existing filter unit tests

### Existing cloud test infrastructure
- `src/test/java/tech/amikos/chromadb/v2/CloudParityIntegrationTest.java` — 8 existing cloud parity tests (CRUD, filters, config round-trips)
- `src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsCloudTest.java` — fork/forkCount/indexingStatus cloud tests
- `src/test/java/tech/amikos/chromadb/v2/CloudAuthIntegrationTest.java` — Auth provider cloud tests
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` — TestContainers base class, utility methods

### External references (chroma-go baseline)
- chroma-go cloud search tests: `pkg/api/v2/client_cloud_test.go` — TestCloudClientSearch subtests
- chroma-go search unit tests: `pkg/api/v2/search_test.go` — Request building, serialization, result parsing
- chroma-go rank tests: `pkg/api/v2/rank_test.go` — KNN, RRF, arithmetic, math functions
- chroma-go groupby tests: `pkg/api/v2/groupby_test.go` — MinK/MaxK aggregate construction

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CloudParityIntegrationTest`: Credential loading, cloud client creation, cleanup pattern, `assumeCloudChroma()` — direct blueprint for Phase 5 test class
- `CollectionApiExtensionsCloudTest`: `indexingStatus()` polling pattern — reuse for D-09 deterministic wait
- `Utils.loadEnvFile(".env")` / `Utils.getEnvOrProperty()`: Environment/credential loading infrastructure
- `AbstractChromaIntegrationTest.embedding(int dim)` / `embeddings(int count, int dim)`: Test embedding generators (though D-06 uses server-side EF)
- `Where.in()`, `Where.nin()`, `Where.eq()`, `Where.gt()`: Filter factory methods for test assertions
- `WhereDocument.contains()`, `WhereDocument.notContains()`: Document filter factory methods

### Established Patterns
- **Cloud client creation**: `ChromaClient.cloud().apiKey(key).tenant(t).database(d).timeout(Duration.ofSeconds(45)).build()`
- **Credential gating**: `Assume.assumeTrue("Missing CHROMA_API_KEY", apiKey != null && !apiKey.isEmpty())`
- **Collection cleanup**: `@After` method with try/catch around `client.deleteCollection(name)` for each created collection
- **Unique naming**: Collection names with UUID suffix to prevent cross-test interference
- **JUnit 4**: All existing tests use JUnit 4 (not JUnit 5) — `@Test`, `@Before`, `@After`, `@BeforeClass`, `Assume`

### Integration Points
- New test class in `src/test/java/tech/amikos/chromadb/v2/` alongside existing cloud tests
- Shares `.env` credential loading with existing cloud tests
- Reuses `ChromaClient.cloud()` builder from v2 package
- Depends on Phase 3 Search API implementation (search builders, result types, ranking expressions)
- Depends on Phase 2 `indexingStatus()` for polling wait strategy

</code_context>

<deferred>
## Deferred Ideas

- Performance benchmarking of cloud search latency — observability concern, not parity testing
- Cross-region cloud testing — infrastructure concern beyond Phase 5 scope
- Cloud rate limit / quota exhaustion tests — operational concern, not functional parity
- Eventual consistency stress testing (high-write + immediate-read) — production readiness concern for future milestone
- Comparing self-hosted vs cloud result ordering for identical queries — interesting but requires both environments simultaneously

</deferred>

---

*Phase: 05-cloud-integration-testing*
*Context gathered: 2026-03-22*
