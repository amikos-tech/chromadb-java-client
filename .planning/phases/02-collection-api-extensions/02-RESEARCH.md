# Phase 2: Collection API Extensions - Research

**Researched:** 2026-03-21
**Domain:** ChromaDB v2 Java client — collection-level HTTP operations (fork, forkCount, indexingStatus)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** `Collection fork(String newName)` — single parameter, returns new Collection reference.
- **D-02:** No options/builder overload — the Chroma server only accepts `new_name`, no metadata or config overrides.
- **D-03:** Fork always creates the new collection in the same tenant/database as the source (no cross-tenant/database targeting).
- **D-04:** Server errors propagate naturally — no client-side cloud guard. Self-hosted will return 404, which maps through the existing exception hierarchy. Future-proof if Chroma adds fork to self-hosted.
- **D-05:** The forked collection inherits the source's embedding function reference (same pattern as Go client).
- **D-06:** `int forkCount()` — bare noun, returns the number of forks for this collection.
- **D-07:** Added to Phase 2 scope (not in original requirements). Present in Python/Rust/JS clients, missing from Go client — Java gets parity with Python/Rust/JS here.
- **D-08:** Endpoint: `GET .../collections/{id}/fork_count` → `{"count": N}`.
- **D-09:** `IndexingStatus indexingStatus()` — bare noun on Collection, consistent with `fork()`, `forkCount()`, `count()`.
- **D-10:** `IndexingStatus` is an immutable value object with JavaBean getters:
  - `long getNumIndexedOps()` — operations compacted into the index
  - `long getNumUnindexedOps()` — operations still in the WAL
  - `long getTotalOps()` — num_indexed + num_unindexed
  - `double getOpIndexingProgress()` — 0.0 to 1.0
- **D-11:** Raw fields only — no convenience methods (e.g., no `isComplete()`). Matches Go client.
- **D-12:** Cloud-only, same server-error-propagation strategy as fork (D-04).
- **D-13:** Bare noun method names for all new operations: `fork()`, `forkCount()`, `indexingStatus()` — consistent with existing `count()`, `add()`, `query()`.
- **D-14:** Javadoc on each cloud-only method uses `<strong>Availability:</strong>` tag documenting cloud-only status and expected self-hosted error behavior.
- **D-15:** Two-layer testing:
  - Unit tests with mock HTTP server (WireMock) — deterministic, runs in CI.
  - Cloud integration tests against real Chroma Cloud — gated by `.env` credentials.
- **D-16:** Fork cloud tests skip in CI (forking is expensive at $0.03/call). Indexing status cloud tests can run in CI.
- **D-17:** TestContainers integration tests that call fork/indexingStatus against self-hosted — currently skip (404), auto-activate if Chroma adds self-hosted support later.
- **D-18:** Cloud integration tests prove parity — if tests pass, parity is confirmed.
- **D-19:** Javadoc on every v2 Collection and Client method with `<strong>Availability:</strong>` tag (cloud-only vs self-hosted + cloud).
- **D-20:** README.md gets a "Cloud vs Self-Hosted" section with a comprehensive parity table covering ALL v2 operations, not just Phase 2 additions.
- **D-21:** CHANGELOG entry documents new operations and their cloud-only status.

### Claude's Discretion
- Mock HTTP server implementation choice (OkHttp MockWebServer, httptest equivalent, or lightweight stub)
- DTO class naming for fork/indexing requests and responses in `ChromaDtos.java`
- `IndexingStatus` implementation details (equals/hashCode/toString)
- Exact README parity table layout and column structure
- How cloud test credentials are loaded (`.env` file, env vars, or both)
- Whether `forkCount()` gets its own DTO or reuses a simple int extraction

### Deferred Ideas (OUT OF SCOPE)
- Cross-tenant/cross-database fork targeting — not supported by Chroma server, revisit if server adds it
- `IndexingStatus.isComplete()` convenience method — users can check `getOpIndexingProgress() >= 1.0` themselves
- Polling helper for indexing status (e.g., `awaitIndexing(Duration timeout)`) — application-level concern, not client library
- Fork with metadata/config overrides — not supported by Chroma server
- Fork quota management APIs — depends on Chroma server adding quota introspection endpoints
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COLL-01 | User can fork a collection via `collection.fork("newName")` and receive a new Collection reference | POST endpoint confirmed; `ChromaHttpCollection.from()` factory is the return path; EF inheritance via `explicitEmbeddingFunction` field |
| COLL-02 | User can check indexing progress via `collection.indexingStatus()` returning IndexingStatus with progress metrics (Chroma >= 1.4.1) | GET endpoint confirmed; immutable value object pattern established; `assumeMinVersion("1.4.1")` for TestContainers test guard |
| COLL-03 | Cloud feature parity status for fork and indexing is explicitly audited, tested, and documented | `CloudParityIntegrationTest` pattern established; README parity table pattern established; CHANGELOG pattern established |
</phase_requirements>

## Summary

Phase 2 adds three new operations to the v2 `Collection` interface: `fork(String)`, `forkCount()`, and `indexingStatus()`. All three follow well-worn paths in the existing codebase. The implementation is essentially mechanical application of the existing patterns — each new method maps to: a path constant in `ChromaApiPaths`, a DTO inner class in `ChromaDtos`, an HTTP call in `ChromaHttpCollection`, and method signatures on the `Collection` interface.

The codebase already uses WireMock for unit-level mock HTTP tests (`ChromaHttpCollectionTest`, `Phase01ValidationTest`, `Phase02ValidationTest`) and `CloudParityIntegrationTest` for live-cloud tests gated by `.env` credentials. The test layer for this phase follows exactly these two patterns, with `Assume.assumeFalse` guards for fork cloud tests (cost-avoidance per D-16) and `assumeMinVersion("1.4.1")` for TestContainers tests that will 404 on older self-hosted versions.

The most non-trivial new artifact is `IndexingStatus` — a new public immutable value object in `tech.amikos.chromadb.v2`. The Tenant/Database pattern (private constructor, static factory `of()`, JavaBean getter, `equals`/`hashCode`/`toString`) is the template to follow exactly. A `Phase03ValidationTest` (the numbering for this phase's Nyquist tests is `Phase03` since the old Phase02 tests already exist for the prior phase) should be created — but given the CONTEXT naming and the fact that the file must match the project convention for "phase validation tests", it should be named `CollectionApiExtensionsValidationTest` or following the `PhaseXXValidationTest` naming convention (use `Phase06CollectionApiExtensionsValidationTest` only if needed to avoid collision — see note in Pitfalls).

**Primary recommendation:** Follow the `count()` blueprint for `forkCount()`, `modifyName()` + `ChromaHttpCollection.from()` blueprint for `fork()`, and `Tenant`/`Database` blueprint for `IndexingStatus`. No new libraries needed. All test infrastructure is already present.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| OkHttp | Already in pom.xml | HTTP transport | `ChromaApiClient` owns all HTTP calls; no new HTTP lib needed |
| Gson | Already in pom.xml | JSON (de)serialization | All DTOs use `@SerializedName`; `ChromaApiClient.gson()` is the instance |
| JUnit 4 | 4.13.2 | Test framework | Entire test suite is JUnit 4; `@Rule`, `@Before`, `@After` |
| WireMock JRE8 | Already in pom.xml | Mock HTTP server for unit tests | Used by `ChromaHttpCollectionTest`, `Phase02ValidationTest`, etc. |
| TestContainers | Already in pom.xml | Integration test container | `AbstractChromaIntegrationTest` base class already set up |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `tech.amikos.chromadb.Utils` | Internal | `.env` file loading | Cloud parity tests use `Utils.loadEnvFile(".env")` |
| `org.junit.Assume` | JUnit 4 built-in | Conditional test skip | `assumeMinVersion()`, `assumeCloudChroma()`, skip-fork-in-CI guard |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| WireMock | OkHttp MockWebServer | WireMock is already used project-wide — consistency wins |
| Manual `Assume.assumeTrue` | Custom annotation | Manual is simpler and consistent with existing `assumeMinVersion()` |

**Installation:** No new dependencies required. All libraries are already declared in `pom.xml`.

## Architecture Patterns

### Recommended Project Structure

New files created in this phase:

```
src/main/java/tech/amikos/chromadb/v2/
├── Collection.java              # Add fork(), forkCount(), indexingStatus() method signatures
├── ChromaHttpCollection.java    # Implement the three new methods
├── ChromaApiPaths.java          # Add collectionFork(), collectionForkCount(), collectionIndexingStatus()
├── ChromaDtos.java              # Add ForkCollectionRequest, ForkCountResponse (optional), IndexingStatusResponse
└── IndexingStatus.java          # New public immutable value object

src/test/java/tech/amikos/chromadb/v2/
├── CollectionApiExtensionsValidationTest.java  # Nyquist validation (WireMock, CI-safe)
├── CollectionApiExtensionsCloudTest.java       # Cloud integration (gated by .env)
└── CollectionApiExtensionsIntegrationTest.java # TestContainers (skip via assumeMinVersion / 404)
```

### Pattern 1: Simple GET returning a primitive — `forkCount()` blueprint

Blueprint: `count()` in `ChromaHttpCollection.java`.

```java
// ChromaApiPaths.java — add path builder
static String collectionForkCount(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/fork_count";
}

// ChromaDtos.java — no DTO class needed, or minimal inner class
// The server returns {"count": N}; deserialize to a helper or extract via Gson directly.
// Option A: reuse Integer.class like count() does — requires field extraction.
// Option B: create a minimal DTO:
static final class ForkCountResponse {
    int count;
}

// ChromaHttpCollection.java — implement method
@Override
public int forkCount() {
    String path = ChromaApiPaths.collectionForkCount(tenant.getName(), database.getName(), id);
    ChromaDtos.ForkCountResponse resp = apiClient.get(path, ChromaDtos.ForkCountResponse.class);
    return resp.count;
}
```

Note: `count()` uses `apiClient.get(path, Integer.class)` directly because the server returns a bare integer. `forkCount()` returns `{"count": N}` (a JSON object), so a DTO is required. Use `ForkCountResponse`.

### Pattern 2: POST returning a new Collection — `fork()` blueprint

Blueprint: `modifyName()` (HTTP call pattern) + `ChromaHttpCollection.from()` (collection construction).

```java
// ChromaApiPaths.java
static String collectionFork(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/fork";
}

// ChromaDtos.java
static final class ForkCollectionRequest {
    @SerializedName("new_name")
    final String newName;

    ForkCollectionRequest(String newName) {
        this.newName = newName;
    }
}

// ChromaHttpCollection.java
@Override
public Collection fork(String newName) {
    String normalizedName = requireNonBlankArgument("newName", newName);
    String path = ChromaApiPaths.collectionFork(tenant.getName(), database.getName(), id);
    ChromaDtos.CollectionResponse resp = apiClient.post(
            path,
            new ChromaDtos.ForkCollectionRequest(normalizedName),
            ChromaDtos.CollectionResponse.class
    );
    return ChromaHttpCollection.from(resp, apiClient, tenant, database, explicitEmbeddingFunction);
}
```

Key insight: `explicitEmbeddingFunction` (the private final field) is passed to `from()` so the forked collection inherits the source's EF (D-05). The forked collection is in the same tenant/database (D-03).

### Pattern 3: GET returning an immutable value object — `indexingStatus()` blueprint

Blueprint: `Tenant`/`Database` for the `IndexingStatus` value object; `count()` for the HTTP call.

```java
// ChromaApiPaths.java
static String collectionIndexingStatus(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/indexing_status";
}

// ChromaDtos.java — response DTO
static final class IndexingStatusResponse {
    @SerializedName("num_indexed_ops")
    long numIndexedOps;
    @SerializedName("num_unindexed_ops")
    long numUnindexedOps;
    @SerializedName("total_ops")
    long totalOps;
    @SerializedName("op_indexing_progress")
    double opIndexingProgress;
}

// ChromaHttpCollection.java
@Override
public IndexingStatus indexingStatus() {
    String path = ChromaApiPaths.collectionIndexingStatus(tenant.getName(), database.getName(), id);
    ChromaDtos.IndexingStatusResponse resp = apiClient.get(path, ChromaDtos.IndexingStatusResponse.class);
    return IndexingStatus.of(resp.numIndexedOps, resp.numUnindexedOps, resp.totalOps, resp.opIndexingProgress);
}
```

### Pattern 4: Immutable value object — `IndexingStatus` blueprint

Blueprint: `Tenant.java` and `Database.java`.

```java
// src/main/java/tech/amikos/chromadb/v2/IndexingStatus.java
public final class IndexingStatus {

    private final long numIndexedOps;
    private final long numUnindexedOps;
    private final long totalOps;
    private final double opIndexingProgress;

    private IndexingStatus(long numIndexedOps, long numUnindexedOps,
                           long totalOps, double opIndexingProgress) {
        this.numIndexedOps = numIndexedOps;
        this.numUnindexedOps = numUnindexedOps;
        this.totalOps = totalOps;
        this.opIndexingProgress = opIndexingProgress;
    }

    public static IndexingStatus of(long numIndexedOps, long numUnindexedOps,
                                    long totalOps, double opIndexingProgress) {
        return new IndexingStatus(numIndexedOps, numUnindexedOps, totalOps, opIndexingProgress);
    }

    public long getNumIndexedOps()       { return numIndexedOps; }
    public long getNumUnindexedOps()     { return numUnindexedOps; }
    public long getTotalOps()            { return totalOps; }
    public double getOpIndexingProgress(){ return opIndexingProgress; }

    @Override
    public boolean equals(Object o) { ... }

    @Override
    public int hashCode() { ... }

    @Override
    public String toString() {
        return "IndexingStatus{numIndexedOps=" + numIndexedOps
               + ", numUnindexedOps=" + numUnindexedOps
               + ", totalOps=" + totalOps
               + ", opIndexingProgress=" + opIndexingProgress + "}";
    }
}
```

### Pattern 5: Cloud parity test — `CloudParityIntegrationTest` blueprint

```java
// CollectionApiExtensionsCloudTest.java
public class CollectionApiExtensionsCloudTest {

    @Before
    public void setUp() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        // ...
        Assume.assumeTrue("CHROMA_API_KEY is required", isNonBlank(apiKey));
        // ...
    }

    @Test
    public void testCloudFork() {
        // Skip in CI — fork costs $0.03/call (D-16)
        Assume.assumeTrue("Fork cloud test skipped in CI (set CHROMA_RUN_FORK_TESTS=true to enable)",
                "true".equals(System.getenv("CHROMA_RUN_FORK_TESTS")));
        // ... create source, fork, assert forked collection exists
    }

    @Test
    public void testCloudIndexingStatus() {
        // No skip guard — indexing status is free and CI-safe (D-16)
        // ... create collection, add records, call indexingStatus(), assert fields
    }

    @Test
    public void testCloudForkCount() {
        // No skip guard — read-only, CI-safe
        // ... create collection, check forkCount() == 0 initially
    }
}
```

### Pattern 6: TestContainers integration test with 404-skip

```java
// CollectionApiExtensionsIntegrationTest.java (extends AbstractChromaIntegrationTest)
@Test
public void testForkSkipsWhenServerReturns404() {
    // Self-hosted does not support fork; expect ChromaNotFoundException and skip.
    Collection col = client.createCollection("source");
    try {
        col.fork("forked");
        // If server adds fork support, this test will start passing — auto-activate.
    } catch (ChromaNotFoundException e) {
        Assume.assumeTrue("fork not available on self-hosted Chroma " + configuredChromaVersion(), false);
    }
}

@Test
public void testIndexingStatusRequires141() {
    assumeMinVersion("1.4.1");
    Collection col = client.createCollection("idx_status_test");
    // Chroma >= 1.4.1 should return indexing status
    IndexingStatus status = col.indexingStatus();
    assertNotNull(status);
    assertTrue(status.getOpIndexingProgress() >= 0.0);
    assertTrue(status.getOpIndexingProgress() <= 1.0);
}
```

### Pattern 7: WireMock unit test — `ChromaHttpCollectionTest` blueprint

```java
// CollectionApiExtensionsValidationTest.java (Nyquist tests)
@Rule
public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

@Test
public void testForkSendsPostWithNewName() {
    stubFor(post(urlEqualTo(COLLECTIONS_PATH))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type","application/json")
                    .withBody("{\"id\":\"col-id-1\",\"name\":\"source\"}")));
    stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
            .withRequestBody(matchingJsonPath("$.new_name", equalTo("forked")))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type","application/json")
                    .withBody("{\"id\":\"forked-id\",\"name\":\"forked\"}")));

    Client client = ChromaClient.builder().baseUrl("http://localhost:" + wireMock.port()).build();
    Collection source = client.getOrCreateCollection("source");
    Collection forked = source.fork("forked");

    assertEquals("forked", forked.getName());
    assertEquals("forked-id", forked.getId());
    verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
            .withRequestBody(matchingJsonPath("$.new_name", equalTo("forked"))));
}

@Test
public void testForkCountReturnsCount() {
    // stub collection setup + GET .../fork_count → {"count": 3}
    assertEquals(3, collection.forkCount());
}

@Test
public void testIndexingStatusMapsFields() {
    // stub GET .../indexing_status → {"num_indexed_ops":10,...}
    IndexingStatus status = collection.indexingStatus();
    assertEquals(10, status.getNumIndexedOps());
    // ...
}
```

### Pattern 8: `PublicInterfaceCompatibilityTest` update

Adding `fork()`, `forkCount()`, `indexingStatus()` to `Collection` adds 3 new declared methods. Current count is `EXPECTED_COLLECTION_METHOD_COUNT = 18`. Update to `21`. Also add method-existence tests for each new method.

### Anti-Patterns to Avoid

- **Return `null` from `fork()` on 404**: Let the exception hierarchy propagate. The existing `execute()` path in `ChromaApiClient` already maps 404 to `ChromaNotFoundException`. No special handling needed.
- **Use a raw `int` as the server response type for `forkCount()`**: The server returns `{"count": N}`, not a bare integer. `count()` works with bare integers; `forkCount()` needs a DTO wrapper.
- **Use `synchronized` in `IndexingStatus`**: Immutable value objects need no synchronization. All fields are `final`, set once in constructor.
- **Skip `toString()` on `IndexingStatus`**: Debugging value objects requires `toString()`. Add it — it's part of the `Tenant`/`Database` blueprint.
- **Forget `PublicInterfaceCompatibilityTest` update**: The method count assertion will fail immediately if the test is not updated. This is a CI gate.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP mock server for unit tests | Custom `HttpServer` stub | WireMock JRE8 (already in pom.xml) | WireMock handles request matching, response stubs, and verification in a few lines |
| JSON serialization for DTOs | Manual `StringBuilder` JSON | Gson `@SerializedName` on DTO fields | All existing DTOs use Gson; adding a new DTO is a 5-line class |
| Collection construction from HTTP response | Custom factory | `ChromaHttpCollection.from(dto, ...)` | Already handles configuration/schema/EF parsing; forked collection reuses this path |
| URL path encoding | Manual `String.format` | `ChromaApiPaths.encode(segment)` | Already handles `+` → `%20` per RFC 3986 §3.3 |
| Version-conditional test skip | `if/else` + `fail()` | `Assume.assumeTrue(...)` | Consistent with `assumeMinVersion()` in `AbstractChromaIntegrationTest` |
| `.env` credential loading | `System.getenv()` only | `Utils.loadEnvFile(".env")` | `CloudParityIntegrationTest` already uses this; loads from `.env` before env var lookup |

## Common Pitfalls

### Pitfall 1: Forgetting `explicitEmbeddingFunction` in `fork()` return value

**What goes wrong:** The forked `Collection` is returned without the source's EF, causing subsequent text queries to fail with "no embedding function configured".

**Why it happens:** `ChromaHttpCollection.from()` takes an `explicitEmbeddingFunction` parameter. It's easy to pass `null` (treating the fork like a `getCollection()` call) instead of passing `this.explicitEmbeddingFunction`.

**How to avoid:** The `from()` call in `fork()` must pass `this.explicitEmbeddingFunction` as the last argument. This is the same field used in `ChromaHttpCollection`'s constructor.

**Warning signs:** Unit test for "forked collection inherits EF" fails; or cloud test with `queryTexts` on a forked collection throws `ChromaException: no embedding function`.

### Pitfall 2: Wrong response type for `forkCount()` — bare int vs JSON object

**What goes wrong:** `apiClient.get(path, Integer.class)` fails with `ChromaDeserializationException` because the server returns `{"count": 3}`, not `3`.

**Why it happens:** `count()` uses `Integer.class` because the `/count` endpoint returns a bare integer. The `/fork_count` endpoint returns a JSON object.

**How to avoid:** Use `ChromaDtos.ForkCountResponse` as the response type, then return `resp.count`.

**Warning signs:** WireMock unit test with `{"count": 3}` body throws `ChromaDeserializationException` instead of returning `3`.

### Pitfall 3: `IndexingStatusResponse` field types — `int` vs `long` vs `double`

**What goes wrong:** `num_indexed_ops` and `num_unindexed_ops` may be large (millions of WAL ops). If declared as `int` in the DTO, large values overflow silently.

**Why it happens:** Gson will silently truncate a JSON number to `int` if the field is `int`, even if the number exceeds `Integer.MAX_VALUE`.

**How to avoid:** Declare `numIndexedOps`, `numUnindexedOps`, and `totalOps` as `long` in both `IndexingStatusResponse` and `IndexingStatus`. Declare `opIndexingProgress` as `double`.

**Warning signs:** Unit test with large op counts produces incorrect values without any exception.

### Pitfall 4: `PublicInterfaceCompatibilityTest` count mismatch

**What goes wrong:** CI fails on `testCollectionInterfaceMethodCount` because `EXPECTED_COLLECTION_METHOD_COUNT` is still `18` after adding 3 methods.

**Why it happens:** The constant is a manually maintained guard. Easy to forget.

**How to avoid:** Update `EXPECTED_COLLECTION_METHOD_COUNT` from `18` to `21` in `PublicInterfaceCompatibilityTest.java` in the same task that adds the interface methods.

**Warning signs:** Test output: `"Collection public method count changed — update EXPECTED_COLLECTION_METHOD_COUNT if intentional"`.

### Pitfall 5: Collision with existing `Phase02ValidationTest`

**What goes wrong:** If the Nyquist validation test for this phase is named `Phase02ValidationTest`, it collides with the existing file for the old phase 2 (api-coverage-completion).

**Why it happens:** The project uses `PhaseXXValidationTest` naming. Phase 02 is already taken.

**How to avoid:** Name the new Nyquist validation test `CollectionApiExtensionsValidationTest` (descriptive, no number collision). Reference it from the phase plan explicitly.

**Warning signs:** Maven reports `duplicate class` or test results from the wrong phase appear.

### Pitfall 6: Fork cloud test running in CI and incurring costs

**What goes wrong:** Fork costs $0.03/call on Chroma Cloud. If `testCloudFork` runs in every CI job, the cost accumulates.

**Why it happens:** Missing `Assume.assumeTrue` guard.

**How to avoid:** Guard fork cloud tests with `Assume.assumeTrue("true".equals(System.getenv("CHROMA_RUN_FORK_TESTS")))`. CI does not set this env var. Local developer can set it to run fork tests explicitly.

**Warning signs:** Cloud spend increases; fork-test teardown leaves stale collections in cloud account.

### Pitfall 7: TestContainers fork test fails instead of skips

**What goes wrong:** The TestContainers fork test throws `ChromaNotFoundException` and the test is marked as FAILED instead of SKIPPED.

**Why it happens:** `ChromaNotFoundException` is not a skip signal — it's a test failure unless caught.

**How to avoid:** Wrap the `fork()` call in a `try/catch (ChromaNotFoundException e)` and call `Assume.assumeTrue(..., false)` inside the catch block. This marks the test as "skipped", not "failed". Auto-activates if the server adds fork support later.

**Warning signs:** TestContainers fork test shows as RED in the test report instead of YELLOW/skipped.

## Code Examples

All patterns below are derived from reading existing source files — HIGH confidence.

### Adding a path builder to `ChromaApiPaths`

```java
// Source: src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java
static String collectionFork(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/fork";
}

static String collectionForkCount(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/fork_count";
}

static String collectionIndexingStatus(String tenant, String db, String id) {
    return collectionById(tenant, db, id) + "/indexing_status";
}
```

### Adding a DTO to `ChromaDtos`

```java
// Source: src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java (pattern from existing DTOs)
static final class ForkCollectionRequest {
    @SerializedName("new_name")
    final String newName;

    ForkCollectionRequest(String newName) {
        this.newName = newName;
    }
}

static final class ForkCountResponse {
    int count;
}

static final class IndexingStatusResponse {
    @SerializedName("num_indexed_ops")
    long numIndexedOps;
    @SerializedName("num_unindexed_ops")
    long numUnindexedOps;
    @SerializedName("total_ops")
    long totalOps;
    @SerializedName("op_indexing_progress")
    double opIndexingProgress;
}
```

### Interface declaration in `Collection.java`

```java
// Source: src/main/java/tech/amikos/chromadb/v2/Collection.java (pattern from count(), modifyName())
/**
 * Creates a copy of this collection with the given name in the same tenant and database.
 *
 * <p>Fork is copy-on-write on the server: data blocks are shared instantly regardless
 * of collection size. A fork tree has a 256-edge limit; exceeding it returns a quota error.</p>
 *
 * <p><strong>Availability:</strong> Chroma Cloud only. Self-hosted Chroma returns
 * {@link ChromaNotFoundException} (404); this exception propagates naturally and will
 * auto-resolve if the server adds self-hosted fork support.</p>
 *
 * @param newName name for the forked collection; must not be blank
 * @return a new {@link Collection} reference for the forked collection
 * @throws IllegalArgumentException if {@code newName} is null or blank
 * @throws ChromaNotFoundException  on self-hosted Chroma (fork not supported)
 * @throws ChromaException          on other server errors
 */
Collection fork(String newName);

/**
 * Returns the number of forks originating from this collection.
 *
 * <p><strong>Availability:</strong> Chroma Cloud only. Self-hosted Chroma returns
 * {@link ChromaNotFoundException} (404).</p>
 *
 * @return number of forks (0 if never forked)
 * @throws ChromaNotFoundException on self-hosted Chroma (fork_count not supported)
 */
int forkCount();

/**
 * Returns the current indexing progress for this collection.
 *
 * <p><strong>Availability:</strong> Chroma Cloud only (requires Chroma &gt;= 1.4.1).
 * Self-hosted Chroma returns {@link ChromaNotFoundException} (404).</p>
 *
 * @return current {@link IndexingStatus} snapshot
 * @throws ChromaNotFoundException on self-hosted Chroma or Chroma &lt; 1.4.1
 */
IndexingStatus indexingStatus();
```

### WireMock stub for `fork()` POST

```java
// Source: src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java (pattern)
stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
        .withRequestBody(matchingJsonPath("$.new_name", equalTo("forked_name")))
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"fork-id-1\",\"name\":\"forked_name\"}")));
```

### WireMock stub for `forkCount()` GET

```java
stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork_count"))
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"count\":3}")));
```

### WireMock stub for `indexingStatus()` GET

```java
stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/indexing_status"))
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"num_indexed_ops\":100,\"num_unindexed_ops\":5,\"total_ops\":105,\"op_indexing_progress\":0.952}")));
```

### Cloud parity test teardown for forked collections

```java
// Source: src/test/java/tech/amikos/chromadb/v2/CloudParityIntegrationTest.java
// Track both the source AND forked collection name for cleanup:
trackCollection(sourceName);
Collection forked = source.fork(forkedName);
trackCollection(forkedName);  // must also be cleaned up in @After
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| No fork support in Java client | `fork()` maps to POST `.../fork` | Phase 2 (now) | Java reaches parity with Python/Rust/JS |
| No forkCount in Go client | `forkCount()` added to Java client | Phase 2 (now) | Java goes ahead of Go client |
| No indexingStatus in Java client | `indexingStatus()` returns immutable VO | Phase 2 (now) | Java reaches parity with Go client |

**Deprecated/outdated:**
- None for this phase. All additions are new surface, no removals.

## Open Questions

1. **Exact `indexingStatus()` minimum server version**
   - What we know: CONTEXT.md says "Chroma >= 1.4.1" for COLL-02.
   - What's unclear: Whether 1.4.1 is the correct threshold or if it might be available earlier. Not verified against official Chroma release notes.
   - Recommendation: Use `assumeMinVersion("1.4.1")` in TestContainers integration test as specified in CONTEXT.md. If tests fail on 1.4.1, bump the version guard.

2. **Fork API response payload**
   - What we know: The endpoint is `POST .../fork` with body `{"new_name": "..."}`. CONTEXT.md does not specify whether the response payload matches `CollectionResponse` exactly.
   - What's unclear: The Go client response type — it likely returns a full collection payload, but the exact fields are not verified here.
   - Recommendation: Assume the response is a standard `CollectionResponse` (same shape as create/get collection). Use `ChromaDtos.CollectionResponse.class` as the deserialization target. If the server returns a different shape, a `ChromaDeserializationException` will surface immediately and can be diagnosed.

3. **`forkCount()` response for collections that have never been forked**
   - What we know: The endpoint returns `{"count": N}`. It is reasonable to expect `{"count": 0}` when never forked.
   - What's unclear: Whether the server might omit the field entirely for count=0 (Gson would default `int count` to `0` for a missing field, which is correct behavior).
   - Recommendation: The default-zero behavior of Gson for missing int fields is acceptable. No special handling needed.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 |
| Config file | pom.xml (Maven Surefire plugin) |
| Quick run command | `mvn test -Dtest=CollectionApiExtensionsValidationTest` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| COLL-01 | `fork("name")` sends POST with `new_name` body and returns Collection with correct id/name | unit (WireMock) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testForkSendsPostAndReturnsCollection` | ❌ Wave 0 |
| COLL-01 | Forked collection inherits source EF | unit (WireMock) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testForkedCollectionInheritsEmbeddingFunction` | ❌ Wave 0 |
| COLL-01 | `fork(null)` throws NPE/IAE | unit (WireMock) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testForkRejectsBlankName` | ❌ Wave 0 |
| COLL-02 | `indexingStatus()` returns `IndexingStatus` with correct field values | unit (WireMock) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testIndexingStatusMapsAllFields` | ❌ Wave 0 |
| COLL-02 | `forkCount()` GET returns parsed int | unit (WireMock) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testForkCountReturnsCount` | ❌ Wave 0 |
| COLL-03 | Cloud fork test (gated by `CHROMA_RUN_FORK_TESTS`) | cloud integration | `mvn test -Dtest=CollectionApiExtensionsCloudTest#testCloudForkCreatesCollection` | ❌ Wave 0 |
| COLL-03 | Cloud indexingStatus test | cloud integration | `mvn test -Dtest=CollectionApiExtensionsCloudTest#testCloudIndexingStatusReturnsValidFields` | ❌ Wave 0 |
| COLL-03 | README parity table exists (structural) | unit (file check) | `mvn test -Dtest=CollectionApiExtensionsValidationTest#testReadmeContainsParityTable` | ❌ Wave 0 |
| COLL-03 | `PublicInterfaceCompatibilityTest` count updated for 3 new methods | unit (reflection) | `mvn test -Dtest=PublicInterfaceCompatibilityTest#testCollectionInterfaceMethodCount` | ✅ (needs constant update) |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=CollectionApiExtensionsValidationTest`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsValidationTest.java` — covers COLL-01, COLL-02, COLL-03 structural checks
- [ ] `src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsCloudTest.java` — covers COLL-03 live-cloud parity
- [ ] `src/test/java/tech/amikos/chromadb/v2/CollectionApiExtensionsIntegrationTest.java` — covers COLL-01/COLL-02 against self-hosted (skip/auto-activate pattern)
- [ ] `src/main/java/tech/amikos/chromadb/v2/IndexingStatus.java` — new public value object

## Sources

### Primary (HIGH confidence)
- Direct source code inspection of `ChromaHttpCollection.java` (implementation patterns)
- Direct source code inspection of `Collection.java` (interface patterns)
- Direct source code inspection of `ChromaApiPaths.java` (path builder patterns)
- Direct source code inspection of `ChromaDtos.java` (DTO patterns)
- Direct source code inspection of `ChromaApiClient.java` (HTTP transport: `get()`, `post()` signatures)
- Direct source code inspection of `Tenant.java` and `Database.java` (immutable value object pattern)
- Direct source code inspection of `CloudParityIntegrationTest.java` (cloud test structure)
- Direct source code inspection of `ChromaHttpCollectionTest.java` (WireMock unit test structure)
- Direct source code inspection of `AbstractChromaIntegrationTest.java` (TestContainers base)
- Direct source code inspection of `PublicInterfaceCompatibilityTest.java` (method count constants)
- Direct source code inspection of `Phase01ValidationTest.java`, `Phase02ValidationTest.java` (Nyquist test file conventions)

### Secondary (MEDIUM confidence)
- CONTEXT.md canonical references for Chroma fork/forkCount/indexingStatus endpoint shapes
- REQUIREMENTS.md for COLL-01, COLL-02, COLL-03 exact requirement descriptions

### Tertiary (LOW confidence)
- "1.4.1" minimum version for `indexingStatus()` — sourced from CONTEXT.md/REQUIREMENTS.md; not independently verified against Chroma release notes

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified in pom.xml; no new dependencies
- Architecture patterns: HIGH — derived directly from existing source code
- Pitfalls: HIGH — derived from code reading + known Gson/Java 8 behaviors
- API endpoint shapes: MEDIUM — sourced from CONTEXT.md, not independently verified against Chroma server source

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (Chroma API is stable; fork/indexingStatus endpoints were locked in Chroma 1.4.x)
