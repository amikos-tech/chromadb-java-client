# Phase 5: Cloud Integration Testing - Research

**Researched:** 2026-03-22
**Domain:** JUnit 4 cloud integration testing — Chroma Cloud Search API, schema/index config, array metadata
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Single test class for all Phase 5 cloud tests — no splitting per requirement. Extends the existing cloud test pattern.
- **D-02:** `Assume.assumeTrue()` gating on missing credentials (skip, don't fail) — consistent with chroma-go and core Chroma.
- **D-03:** Tests run in GitHub Actions CI with secrets, not manual-only.
- **D-04:** Shared realistic seed collection (10-20 records) for read-only tests, created once in `@BeforeClass`. Dataset models a realistic domain (e.g., product catalog with titles, categories, prices, tags).
- **D-05:** Isolated per-test collections for any test that mutates data (upsert, delete, schema changes).
- **D-06:** Seed data uses the default embedding function (server-side) — tests the full cloud path rather than explicit embeddings.
- **D-07:** Cloud integration tests cover both KNN and RRF end-to-end.
- **D-08:** Cloud integration tests cover GroupBy with MinK/MaxK aggregation end-to-end.
- **D-09:** Polling loop on `collection.indexingStatus()` to wait for indexing completion before search assertions.
- **D-10:** Batch search tested (multiple independent `Search` objects in one call).
- **D-11:** Explicit test for `Knn.limit` (candidate pool) vs `Search.limit` (final result count) distinction.
- **D-12:** Read level tests: `INDEX_AND_WAL` asserts all records immediately (no polling wait), `INDEX_ONLY` asserts count <= total (index may not be compacted yet).
- **D-13:** Small but varied matrix of filter combinations (Where alone, IDIn/IDNotIn alone, DocumentContains alone, IDNotIn + metadata combined, Where + DocumentContains combined, triple combination).
- **D-14:** Pagination tests: basic limit, limit+offset (page 2), and client-side validation for obviously invalid inputs.
- **D-15:** Test that selected fields are present and excluded fields are truly absent (null).
- **D-16:** Test custom metadata key projection (specific metadata keys, not just `#metadata` blob).
- **D-17:** Extend existing `testCloudConfigurationParityWithRequestAuthoritativeFallback()` pattern.
- **D-18:** Test distance space variants (cosine, l2, ip) — create collection with each, verify round-trip.
- **D-19:** Test invalid config transitions (e.g., change distance space after data inserted) — assert appropriate error.
- **D-20:** Test HNSW and SPANN config paths independently — verify config round-trip for each.
- **D-21:** Test string, number, and bool arrays independently — dedicated records and assertions per type.
- **D-22:** Mixed-type arrays must be rejected at client level. Add client validation if it doesn't exist.
- **D-23:** Round-trip assertions verify both values AND types. Floats must not become integers.
- **D-24:** `contains`/`not_contains` filter edge cases all covered (single-element, no-match, all-match, missing key).
- **D-25:** Empty arrays (`"tags": []`) tested for storage and retrieval — document actual cloud behavior.

### Claude's Discretion

- Exact realistic seed data domain and field names
- Polling loop timeout and interval for `indexingStatus()` wait
- Test method naming conventions within the single class
- Order of test methods within the class
- Specific embedding dimension for seed data
- Whether to use `@FixMethodOrder` or rely on JUnit default ordering
- Exact filter combination matrix layout (which specific metadata fields to filter on)
- How to structure the `@BeforeClass` seed method (helper methods, constants, etc.)

### Deferred Ideas (OUT OF SCOPE)

- Performance benchmarking of cloud search latency
- Cross-region cloud testing
- Cloud rate limit / quota exhaustion tests
- Eventual consistency stress testing
- Comparing self-hosted vs cloud result ordering for identical queries
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLOUD-01 | Cloud search parity tests cover pagination, IDIn/IDNotIn, document filters, metadata projection, and combined filter scenarios. | Search API patterns from `CloudParityIntegrationTest` + go-client docs confirm `#id`/`#document` inline filters are Cloud-only; `Where.idIn/idNotIn/documentContains/documentNotContains` already exist |
| CLOUD-02 | Cloud schema/index tests cover distance space variants, HNSW/SPANN config paths, invalid transitions, and schema round-trip assertions. | `CollectionConfiguration`, `UpdateCollectionConfiguration`, `DistanceFunction` (cosine/l2/ip) all exist; `detectIndexGroup` pattern in `CloudParityIntegrationTest` is reusable blueprint |
| CLOUD-03 | Cloud array metadata tests cover string/number/bool arrays, round-trip retrieval, and contains/not_contains filter behavior. | `Where.contains/notContains` for all types already implemented; mixed-type array validation may need to be added at metadata-serialization layer |
</phase_requirements>

---

## Summary

Phase 5 adds a single cloud integration test class (`SearchApiCloudIntegrationTest`) that exercises three distinct capability groups: (1) the Phase 3 Search API end-to-end against Chroma Cloud including KNN, RRF, GroupBy, batch, pagination, and filter projections; (2) distance-space and HNSW/SPANN config round-trips including invalid transition assertions; and (3) array metadata storage, round-trip type fidelity, and `contains`/`not_contains` filter edge cases.

All existing infrastructure is in place. `CloudParityIntegrationTest` provides the canonical blueprint for credential loading, cloud client construction, collection tracking and cleanup, and credential-gate skipping. `CollectionApiExtensionsCloudTest` provides the `indexingStatus()` polling pattern. The `Where.*` DSL already has `contains`, `notContains`, `idIn`, `idNotIn`, `documentContains`, and `documentNotContains`. The only open question is whether Phase 3 Search API types (`SearchResult`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel`) will be available when Phase 5 runs — this phase depends on Phase 3 completing first.

**Primary recommendation:** Name the test class `SearchApiCloudIntegrationTest` (suffix `IntegrationTest`) so the `integration` Maven profile and the `v2-integration-test` CI job run it with cloud credentials. Use `@BeforeClass` for shared seed collection setup and `@Before`/`@After` for per-test isolated collections.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 (`junit:junit`) | 4.13.2 | Test runner, `@Test`, `@Before`, `@After`, `@BeforeClass`, `Assume` | Already used throughout the codebase |
| `Assume.assumeTrue` | (part of JUnit 4) | Credential gating — skip without fail | Project-wide pattern, consistent with D-02 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `tech.amikos.chromadb.Utils` | project-local | `.env` loading, `getEnvOrProperty()` | All credential loading |
| `ChromaClient.cloud()` builder | v2 | Cloud client construction | Auth, tenant, database, timeout |
| Phase 2 `IndexingStatus` / `collection.indexingStatus()` | v2 | Polling wait for indexing completion (D-09) | After `add()` before search assertions |
| Phase 3 Search API (pending) | v2 | `SearchResult`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel` | All CLOUD-01 search tests |

### Installation
No new dependencies required. All tooling is already in `pom.xml`.

---

## Architecture Patterns

### Recommended Project Structure
```
src/test/java/tech/amikos/chromadb/v2/
├── SearchApiCloudIntegrationTest.java   (new — Phase 5)
├── CloudParityIntegrationTest.java      (existing — blueprint)
└── CollectionApiExtensionsCloudTest.java (existing — indexingStatus polling blueprint)
```

### Pattern 1: Credential Gate + Cloud Client (from `CloudParityIntegrationTest`)
**What:** `@Before` method loads credentials, gates with `Assume.assumeTrue`, builds cloud client.
**When to use:** Every cloud test class.
```java
// Source: CloudParityIntegrationTest.java (existing)
@Before
public void setUp() {
    Utils.loadEnvFile(".env");
    String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
    tenant = Utils.getEnvOrProperty("CHROMA_TENANT");
    database = Utils.getEnvOrProperty("CHROMA_DATABASE");

    Assume.assumeTrue("CHROMA_API_KEY is required for cloud integration tests", isNonBlank(apiKey));
    Assume.assumeTrue("CHROMA_TENANT is required for cloud integration tests", isNonBlank(tenant));
    Assume.assumeTrue("CHROMA_DATABASE is required for cloud integration tests", isNonBlank(database));

    client = ChromaClient.cloud()
            .apiKey(apiKey)
            .tenant(tenant)
            .database(database)
            .timeout(Duration.ofSeconds(45))
            .build();
}
```

### Pattern 2: Shared Seed Collection via `@BeforeClass`
**What:** `@BeforeClass` creates a shared collection once, populates 10-20 records, waits for indexing.
**When to use:** CLOUD-01 and CLOUD-03 read-only tests share one collection to minimize cloud API calls.
**Key constraint:** `@BeforeClass` cannot access instance fields — must use static fields for the shared client and collection. Credential loading in `@BeforeClass` should use `Assume.assumeTrue` to skip cleanly when credentials are absent.

```java
// Pattern (not copy-paste, but structure)
private static Client sharedClient;
private static Collection seedCollection;
private static String sharedCollectionName;

@BeforeClass
public static void setUpSharedSeedCollection() {
    Utils.loadEnvFile(".env");
    String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
    // ... credential checks with Assume.assumeTrue
    sharedClient = ChromaClient.cloud().apiKey(apiKey)...build();
    sharedCollectionName = "seed_" + UUID.randomUUID().toString().substring(0, 8);
    seedCollection = sharedClient.createCollection(sharedCollectionName);
    // add() records with server-side EF (no explicit embeddings — D-06)
    // poll indexingStatus() until complete
}

@AfterClass
public static void tearDownSharedSeedCollection() {
    if (sharedClient != null) {
        try { sharedClient.deleteCollection(sharedCollectionName); } catch (ChromaException ignored) {}
        sharedClient.close();
    }
}
```

### Pattern 3: IndexingStatus Polling (from `CollectionApiExtensionsCloudTest`)
**What:** Poll `indexingStatus()` until `opIndexingProgress >= 1.0` or timeout.
**When to use:** After `add()` before any search assertion (D-09). Do NOT use for `INDEX_AND_WAL` read level test which deliberately skips polling (D-12).
**Recommendation (Claude's discretion):** Timeout=60s, poll interval=2s.

```java
// Pattern structure
private static void waitForIndexing(Collection col, long timeoutMs, long pollIntervalMs)
        throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
        IndexingStatus status = col.indexingStatus();
        if (status.getOpIndexingProgress() >= 1.0 - 1e-6) {
            return;
        }
        Thread.sleep(pollIntervalMs);
    }
    // timeout — fail assertion so test surfaces the issue
    IndexingStatus final = col.indexingStatus();
    assertTrue("Indexing did not complete within timeout: " + final, false);
}
```

### Pattern 4: Best-Effort Cleanup (from `CloudParityIntegrationTest`)
**What:** `@After` iterates `createdCollections` in reverse, tries `deleteCollection`, swallows `ChromaException`.
**When to use:** For the per-test mutable collections (D-05). Shared seed collection uses `@AfterClass`.

### Pattern 5: Distance-Space Config Round-Trip
**What:** Create collection with explicit `space()` in `CollectionConfiguration`, verify `col.getConfiguration().getSpace()`.
**When to use:** CLOUD-02 distance space variant tests (D-18).

```java
// Using existing CollectionConfiguration builder
CreateCollectionOptions opts = CreateCollectionOptions.builder()
    .configuration(CollectionConfiguration.builder().space(DistanceFunction.COSINE).build())
    .build();
Collection col = client.createCollection(name, opts);
// After creation:
assertNotNull(col.getConfiguration());
assertEquals(DistanceFunction.COSINE, col.getConfiguration().getSpace());
```

### Pattern 6: Mixed-Type Array Client Validation (D-22)
**What:** Metadata map containing a Java `List` with elements of mixed types must be rejected before HTTP call.
**When to use:** CLOUD-03 mixed-array test.
**Gap to investigate:** The current codebase does not appear to have explicit mixed-type array validation at the metadata serialization level. If `ChromaDtos` or `ChromaHttpCollection` silently accepts `List<Object>` with mixed types, Phase 5 must add client-level validation. This needs confirmation during implementation.

### Anti-Patterns to Avoid
- **Fixed sleep instead of polling:** chroma-go uses `time.Sleep(2s)` — Java uses `indexingStatus()` polling per D-09.
- **Splitting tests per requirement:** D-01 mandates single class.
- **`@BeforeClass` without `@AfterClass`:** Always pair — cloud collections persist and accumulate cost.
- **Asserting exact result ordering without sorting:** Cloud result order for equal scores is not guaranteed. Assert set membership or sorted order explicitly.
- **Hard-coding collection names without UUID suffix:** Cross-test interference. Always suffix with UUID.
- **Not tracking per-test collections:** All collections created in a test must be tracked for cleanup even if assertions fail mid-test.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Credential loading | Custom env reader | `Utils.loadEnvFile(".env")` + `Utils.getEnvOrProperty()` | Already handles env/system-property fallback |
| Test skipping on missing credentials | System.exit or throw | `Assume.assumeTrue()` | JUnit 4 skips gracefully, not fails |
| Indexing wait | Thread.sleep loops | `indexingStatus()` polling pattern | Deterministic, testable, already in existing cloud tests |
| Cloud client construction | Raw HTTP client | `ChromaClient.cloud().apiKey().tenant().database().timeout().build()` | Auth, tenant scoping, timeout wired in |
| Filter DSL | Manual map construction | `Where.idIn()`, `Where.documentContains()`, `Where.contains()`, etc. | Type-safe, validated, already tested |

**Key insight:** Every infrastructure piece is already present. Phase 5 is purely test composition using existing building blocks.

---

## Common Pitfalls

### Pitfall 1: Test Class Naming — Integration Profile Mismatch
**What goes wrong:** If new class is named `SearchApiCloudTest.java` (not `IntegrationTest.java`), the Maven `integration` profile does NOT include it. The `v2-integration-test` CI job runs `mvn --batch-mode test -Pintegration` which only picks up `**/*IntegrationTest.java`.
**Why it happens:** `CollectionApiExtensionsCloudTest` (existing) uses `CloudTest` suffix and is effectively excluded from CI cloud execution — it only runs locally with `.env`. New Phase 5 class must follow `CloudParityIntegrationTest` naming pattern.
**How to avoid:** Name the class `SearchApiCloudIntegrationTest`. Verify CI job picks it up.
**Warning signs:** CI job shows 0 tests run (no failure, just skips) when credentials are absent in unit-tests job.

### Pitfall 2: `@BeforeClass` + `Assume` Interaction
**What goes wrong:** If `@BeforeClass` throws (e.g., null credential not caught), all tests in the class fail rather than skip.
**Why it happens:** JUnit 4 treats uncaught exceptions in `@BeforeClass` as class-level errors (fail), not skips.
**How to avoid:** Gate credentials with `Assume.assumeTrue()` in `@BeforeClass`. The `assumeTrue` in a `@BeforeClass` context causes all tests in the class to be skipped, which is the desired behavior (D-02).
**Warning signs:** Tests show `ERROR` (not `SKIPPED`) in Maven output when credentials are absent.

### Pitfall 3: Shared Seed Collection Pollution
**What goes wrong:** A test that's supposed to be read-only accidentally mutates the shared seed collection (e.g., by calling `upsert()` or `delete()`).
**Why it happens:** D-04 mandates shared seed for read-only tests — if a test that should use an isolated collection (D-05) uses the shared one instead, subsequent tests see corrupted state.
**How to avoid:** Clearly document in code which tests use shared seed vs isolated collection. All mutating tests (upsert, delete, schema changes) must create their own collection via the `@Before` instance-level client.
**Warning signs:** Flaky tests where result counts change across runs.

### Pitfall 4: Float/Integer Type Round-Trip (D-23)
**What goes wrong:** A float metadata value (e.g., `3.14`) is stored and retrieved as an integer or double, failing type-equality assertion.
**Why it happens:** JSON parsing layer may deserialize `3.0` as `Integer(3)` rather than `Float(3.0f)` or `Double(3.0)`. The metadata map is typed as `Map<String, Object>`, so runtime type is the deserialized type.
**How to avoid:** Assert using `instanceof Float` / `instanceof Double` checks plus value comparison, not `assertEquals(Float.class, val.getClass())` which is too brittle. Alternatively check `.toString()` and compare string representations.
**Warning signs:** Test assertion "expected `3.14` (Float) but was `3.14` (Double)".

### Pitfall 5: Search API Not Yet Implemented (Phase Dependency)
**What goes wrong:** Phase 5 depends on Phase 3 Search API (`SearchResult`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel`). If Phase 3 is not complete, Phase 5 tests cannot compile.
**Why it happens:** Phase ordering — current state shows Phase 3 pending.
**How to avoid:** Phase 5 must be planned but implementation blocked until Phase 3 ships. CLOUD-01 tests (KNN, RRF, GroupBy, batch, read level) are entirely gated on Phase 3 types. CLOUD-02 (schema/index) and CLOUD-03 (array metadata) can be implemented independently of Phase 3.
**Warning signs:** Compilation failure on import of `Search`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel` classes.

### Pitfall 6: INDEX_ONLY May Return Fewer Records Than Inserted
**What goes wrong:** Test asserts `count == 15` for `INDEX_ONLY` read level but gets 12 because compaction hasn't run yet.
**Why it happens:** `INDEX_ONLY` intentionally skips the WAL — recently inserted records may not be in the compacted index yet.
**How to avoid:** Per D-12, `INDEX_ONLY` tests use `<=` assertion (assert result count is at most total record count, not exactly). `INDEX_AND_WAL` tests assert exactly, and skip polling (WAL guarantees all records are visible).
**Warning signs:** Intermittent assertion failures on exact count after `INDEX_ONLY` search.

### Pitfall 7: Mixed-Type Array Validation Gap
**What goes wrong:** A `List` containing `["foo", 42, true]` is sent to the server without client-side rejection, resulting in undefined server behavior (may succeed, may return 400, may silently drop elements).
**Why it happens:** D-22 requires client-level rejection. Current metadata serialization (`ChromaDtos`) may not validate array element types.
**How to avoid:** During implementation, grep for metadata serialization code and add type validation for `List` values in metadata maps. The test for D-22 should assert a `ChromaBadRequestException` (or `IllegalArgumentException`) is thrown before any HTTP call.
**Warning signs:** Mixed-array test passes but actually sent a request — check with network mock if unclear.

---

## Code Examples

Verified patterns from existing source:

### Credential Gate (JUnit 4)
```java
// Source: CloudParityIntegrationTest.java (existing)
@Before
public void setUp() {
    Utils.loadEnvFile(".env");
    String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
    Assume.assumeTrue("CHROMA_API_KEY is required", isNonBlank(apiKey));
    client = ChromaClient.cloud().apiKey(apiKey).tenant(tenant).database(database)
            .timeout(Duration.ofSeconds(45)).build();
}

private static boolean isNonBlank(String value) {
    return value != null && !value.trim().isEmpty();
}
```

### Unique Collection Name
```java
// Source: CloudParityIntegrationTest.java (existing)
private static String uniqueCollectionName(String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "");
}
```

### Best-Effort Cleanup
```java
// Source: CloudParityIntegrationTest.java (existing)
@After
public void tearDown() {
    if (client != null) {
        for (int i = createdCollections.size() - 1; i >= 0; i--) {
            try { client.deleteCollection(createdCollections.get(i)); }
            catch (ChromaException ignored) {}
        }
        client.close();
    }
    createdCollections.clear();
}
```

### DistanceFunction Round-Trip
```java
// Source: DistanceFunction.java (existing enum values)
// CreateCollectionOptions.builder().configuration(CollectionConfiguration.builder()
//     .space(DistanceFunction.COSINE).build()).build()
// Verify: assertEquals(DistanceFunction.COSINE, col.getConfiguration().getSpace())
```

### IndexingStatus Check
```java
// Source: CollectionApiExtensionsCloudTest.java (existing)
IndexingStatus status = col.indexingStatus();
assertTrue(status.getOpIndexingProgress() >= 0.0 && status.getOpIndexingProgress() <= 1.0);
assertEquals(status.getTotalOps(), status.getNumIndexedOps() + status.getNumUnindexedOps());
```

### Contains/NotContains Filter (Array Metadata)
```java
// Source: Where.java (existing)
// Where.contains("tags", "electronics")   -> {"tags": {"$contains": "electronics"}}
// Where.notContains("tags", "furniture")  -> {"tags": {"$not_contains": "furniture"}}
// Where.contains("prices", 29.99f)        -> {"prices": {"$contains": 29.99}}
// Where.contains("flags", true)           -> {"flags": {"$contains": true}}
```

---

## CI and Test Execution Architecture

### Maven Profile Mechanics
| Command | Profile | Which Tests Run |
|---------|---------|----------------|
| `mvn test` | default (no profile) | All `*Test.java` EXCEPT `*IntegrationTest.java` |
| `mvn test -Pintegration` | integration | ONLY `*IntegrationTest.java` |
| `mvn test -Pquality` | quality | All `*Test.java` in v2 package |

### GitHub Actions Jobs
| Job | Command | Credentials | What Runs |
|-----|---------|------------|-----------|
| `unit-tests` | `mvn test` | None (OPENAI/COHERE/HF only) | Unit tests; cloud `*CloudTest.java` files run but `Assume` skips them |
| `integration-tests` | `mvn test -Pintegration` | None | TestContainers integration tests |
| `v2-integration-test` | `mvn test -Pintegration` | CHROMA_API_KEY/TENANT/DATABASE | `*IntegrationTest.java` — including cloud parity tests |

**Critical:** The new Phase 5 class MUST be named `SearchApiCloudIntegrationTest.java` (suffix `IntegrationTest`) to be included in the `v2-integration-test` CI job per D-03.

### Test Execution Commands
```bash
# Run Phase 5 cloud tests locally (requires .env with credentials)
mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest

# Run all cloud integration tests
mvn test -Pintegration

# Verify test skips cleanly (no credentials)
mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest
# Expected: test methods report as "skipped" not "failed"
```

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) |
| Config file | Maven Surefire — pom.xml `<integration>` profile |
| Quick run command | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest` |
| Full suite command | `mvn test -Pintegration` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CLOUD-01 | KNN search end-to-end | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudKnnSearch` | ❌ Wave 0 |
| CLOUD-01 | RRF hybrid search end-to-end | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudRrfSearch` | ❌ Wave 0 |
| CLOUD-01 | GroupBy with MinK/MaxK | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudGroupBySearch` | ❌ Wave 0 |
| CLOUD-01 | Batch search | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudBatchSearch` | ❌ Wave 0 |
| CLOUD-01 | Pagination (limit, offset) | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudSearchPagination` | ❌ Wave 0 |
| CLOUD-01 | Filter matrix (IDIn, IDNotIn, DocumentContains, combinations) | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudSearchFilterMatrix` | ❌ Wave 0 |
| CLOUD-01 | Field projection (present/absent) | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudSearchProjection` | ❌ Wave 0 |
| CLOUD-01 | ReadLevel INDEX_AND_WAL and INDEX_ONLY | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudSearchReadLevel` | ❌ Wave 0 |
| CLOUD-02 | Distance space round-trips (cosine/l2/ip) | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudDistanceSpaceRoundTrip` | ❌ Wave 0 |
| CLOUD-02 | HNSW config round-trip | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudHnswConfigRoundTrip` | ❌ Wave 0 |
| CLOUD-02 | SPANN config round-trip | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudSpannConfigRoundTrip` | ❌ Wave 0 |
| CLOUD-02 | Invalid config transition rejected | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudInvalidConfigTransitionRejected` | ❌ Wave 0 |
| CLOUD-03 | String array round-trip + contains filter | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudStringArrayMetadata` | ❌ Wave 0 |
| CLOUD-03 | Number array round-trip + contains filter | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudNumberArrayMetadata` | ❌ Wave 0 |
| CLOUD-03 | Bool array round-trip + contains filter | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudBoolArrayMetadata` | ❌ Wave 0 |
| CLOUD-03 | Mixed-type array rejected at client | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudMixedTypeArrayRejected` | ❌ Wave 0 |
| CLOUD-03 | Empty array stored/retrieved | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudEmptyArrayMetadata` | ❌ Wave 0 |
| CLOUD-03 | contains edge cases (single-element, no-match, all-match, missing key) | cloud integration | `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest#testCloudArrayContainsEdgeCases` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Pintegration -Dtest=SearchApiCloudIntegrationTest` (skips cleanly when no credentials)
- **Per wave merge:** `mvn test -Pintegration`
- **Phase gate:** Full suite green (or skipped) before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiCloudIntegrationTest.java` — covers CLOUD-01, CLOUD-02, CLOUD-03
- [ ] Mixed-type array client validation — if `ChromaDtos` metadata serialization does not reject `List<Object>` with mixed types, add validation before Phase 5 implementation begins

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `time.Sleep(2s)` (chroma-go) | `indexingStatus()` polling | Phase 2 added `indexingStatus()` | Deterministic wait; Java gets ahead of chroma-go baseline |
| Fixed `query()` / `get()` | Unified `search()` (Cloud-only) | Chroma Cloud launch | Phase 3 adds `search()` builder; Phase 5 tests it end-to-end |
| Separate cloud test per feature area | Single class per milestone area | Project convention | Simpler maintenance; D-01 |

---

## Open Questions

1. **Phase 3 Search API type signatures**
   - What we know: Phase 3 is pending. CONTEXT.md references `SearchResult`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel`, `Search` types. The go-client docs show `WithKnnLimit`, `WithKnnReturnRank`, `WithRrfRanks`, `WithRrfK`.
   - What's unclear: Exact Java method names and builder API for these types — they don't exist yet.
   - Recommendation: CLOUD-01 tests should be written to match Phase 3 API as it emerges. CLOUD-02 and CLOUD-03 tests do NOT depend on Phase 3 and can be planned/implemented independently.

2. **Mixed-type array validation location**
   - What we know: D-22 requires client-level rejection. `Where.java` validates inputs but metadata map values are not validated for type homogeneity.
   - What's unclear: Whether `ChromaDtos` or `ChromaHttpCollection`'s metadata serialization already rejects `List<Object>` with mixed types.
   - Recommendation: Implementation wave should grep `ChromaDtos` for metadata serialization and add validation if missing. Test asserts `ChromaBadRequestException` or `IllegalArgumentException` is thrown.

3. **Server behavior for empty arrays**
   - What we know: D-25 says test empty arrays and document actual behavior. Whether `"tags": []` is preserved, dropped, or nullified by Chroma Cloud is unknown without a live test.
   - What's unclear: Cloud response for `[]` — does it round-trip as `[]`, disappear, or return `null`?
   - Recommendation: Test asserts the actual observed behavior (e.g., `assertNull(tags)` if cloud drops it, or `assertEquals(0, tags.size())` if it preserves it) and adds a comment documenting the finding.

4. **SPANN availability in CI cloud account**
   - What we know: `detectIndexGroup()` in `CloudParityIntegrationTest` detects HNSW vs SPANN from server response because the default index type depends on account configuration.
   - What's unclear: Whether the CI cloud account uses HNSW or SPANN by default.
   - Recommendation: CLOUD-02 SPANN test should use the same fallback pattern as `testCloudConfigurationParityWithRequestAuthoritativeFallback` — try SPANN, catch `IllegalArgumentException` for index-group switch error, fallback. Or better: explicitly create collections with HNSW vs SPANN configuration via `CollectionConfiguration.builder().hnswM(16).build()` vs `.spannSearchNprobe(10).build()`.

---

## Sources

### Primary (HIGH confidence)
- `CloudParityIntegrationTest.java` — credential pattern, cloud client construction, cleanup, collection tracking
- `CollectionApiExtensionsCloudTest.java` — indexingStatus polling pattern
- `AbstractChromaIntegrationTest.java` — `assumeCloudChroma()`, `assumeMinVersion()` helpers
- `Where.java` — `contains`, `notContains`, `idIn`, `idNotIn`, `documentContains`, `documentNotContains`
- `CollectionConfiguration.java` — space (DistanceFunction), HNSW/SPANN parameters
- `UpdateCollectionConfiguration.java` — mutable config update
- `DistanceFunction.java` — `COSINE`, `L2`, `IP`
- `IndexingStatus.java` — `opIndexingProgress`, `totalOps`, `numIndexedOps`, `numUnindexedOps`
- `pom.xml` — Maven Surefire profiles (default, integration, quality), JUnit 4.13.2 version
- `.github/workflows/integration-test.yml` — CI job matrix, credentials injection, `v2-integration-test` job

### Secondary (MEDIUM confidence)
- [Chroma Cloud Search API Overview](https://docs.trychroma.com/cloud/search-api/overview) — confirmed Search API is Cloud-only, KNN/RRF/GroupBy/batch capabilities
- [ChromaDB Go Client Search API](https://go-client.chromadb.dev/search/) — confirmed `ReadLevelIndexAndWAL` / `ReadLevelIndexOnly` semantics, `WithKnnLimit` (candidate pool) vs search limit (result count), `KID/KDocument/KEmbedding/KMetadata/KScore` projection keys, RRF formula and `WithRrfK(60)` default

### Tertiary (LOW confidence)
- WebSearch results confirming GroupBy MinK/MaxK and batch search exist in go-client — not directly verified against official docs page (404 on sub-pages)

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all dependencies already in project, no new additions needed
- Architecture patterns: HIGH — verified from existing CloudParityIntegrationTest and CollectionApiExtensionsCloudTest
- Pitfalls: HIGH — naming pitfall verified from pom.xml surefire profile inspection; others derived from existing code patterns
- Phase 3 Search API types: LOW — Phase 3 not yet implemented; type names inferred from CONTEXT.md and go-client baseline

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (Search API docs are stable; Phase 3 implementation will define exact Java API surface)
