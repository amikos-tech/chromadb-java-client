# Phase 2: Collection API Extensions - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Add cloud-relevant collection operations (fork, fork count, indexing status) to the v2 Collection interface and audit cloud feature parity for all v2 operations. No new embedding, search, or record operation work — this phase extends the collection-level API surface only.

</domain>

<decisions>
## Implementation Decisions

### fork() API surface
- **D-01:** `Collection fork(String newName)` — single parameter, returns new Collection reference.
- **D-02:** No options/builder overload — the Chroma server only accepts `new_name`, no metadata or config overrides.
- **D-03:** Fork always creates the new collection in the same tenant/database as the source (no cross-tenant/database targeting).
- **D-04:** Server errors propagate naturally — no client-side cloud guard. Self-hosted will return 404, which maps through the existing exception hierarchy. Future-proof if Chroma adds fork to self-hosted.
- **D-05:** The forked collection inherits the source's embedding function reference (same pattern as Go client).

### forkCount() API surface
- **D-06:** `int forkCount()` — bare noun, returns the number of forks for this collection.
- **D-07:** Added to Phase 2 scope (not in original requirements). Present in Python/Rust/JS clients, missing from Go client — Java gets parity with Python/Rust/JS here.
- **D-08:** Endpoint: `GET .../collections/{id}/fork_count` → `{"count": N}`.

### indexingStatus() API surface
- **D-09:** `IndexingStatus indexingStatus()` — bare noun on Collection, consistent with `fork()`, `forkCount()`, `count()`.
- **D-10:** `IndexingStatus` is an immutable value object with JavaBean getters:
  - `long getNumIndexedOps()` — operations compacted into the index
  - `long getNumUnindexedOps()` — operations still in the WAL
  - `long getTotalOps()` — num_indexed + num_unindexed
  - `double getOpIndexingProgress()` — 0.0 to 1.0
- **D-11:** Raw fields only — no convenience methods (e.g., no `isComplete()`). Matches Go client.
- **D-12:** Cloud-only, same server-error-propagation strategy as fork (D-04).

### Naming conventions
- **D-13:** Bare noun method names for all new operations: `fork()`, `forkCount()`, `indexingStatus()` — consistent with existing `count()`, `add()`, `query()`.
- **D-14:** Javadoc on each cloud-only method uses `<strong>Availability:</strong>` tag documenting cloud-only status and expected self-hosted error behavior.

### Testing strategy
- **D-15:** Two-layer testing, aligned with chroma-go:
  - **Unit tests** with mock HTTP server (canned JSON responses) — deterministic, runs in CI.
  - **Cloud integration tests** against real Chroma Cloud — gated by credentials from `.env`.
- **D-16:** Fork cloud tests skip in CI (forking is expensive at $0.03/call). Indexing status cloud tests can run in CI.
- **D-17:** TestContainers integration tests that call fork/indexingStatus against self-hosted — currently skip (404), auto-activate if Chroma adds self-hosted support later.

### Cloud parity audit
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

</decisions>

<specifics>
## Specific Ideas

- Align with chroma-go's `Fork(ctx, newName) (Collection, error)` and `IndexingStatus(ctx) (*IndexingStatus, error)` — Java drops ctx (no context.Context in Java 8) but keeps the same signatures.
- Go client testing uses `httptest.NewServer` with regex URL matching and hardcoded JSON — Java equivalent is OkHttp MockWebServer or similar lightweight approach.
- Fork is copy-on-write on the server (shared data blocks, instant regardless of size) — this is useful context for Javadoc.
- Fork has a 256 fork-edge limit per tree. Exceeding triggers a quota error. This should be noted in Javadoc.
- `forkCount()` is ahead of Go client (which doesn't have it) — differentiator alongside comprehensive parity table.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Collection interface & implementation
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — Current Collection interface, add fork/forkCount/indexingStatus here
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — HTTP implementation, implement new methods here
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java` — Endpoint path builders, add fork/forkCount/indexingStatus paths
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` — Request/response DTOs, add fork request and indexing status response
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiClient.java` — HTTP transport (get/post/put/delete methods)

### Client & session context
- `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java` — Client implementation, reference for how Collection instances are created and cached

### Existing value objects (patterns to follow)
- `src/main/java/tech/amikos/chromadb/v2/Tenant.java` — Immutable value object pattern (getName(), equals/hashCode)
- `src/main/java/tech/amikos/chromadb/v2/Database.java` — Immutable value object pattern
- `src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java` — Complex immutable value object with builder

### Exception hierarchy
- `src/main/java/tech/amikos/chromadb/v2/ChromaException.java` — Base exception
- `src/main/java/tech/amikos/chromadb/v2/ChromaExceptions.java` — Factory: `fromHttpResponse(statusCode, message, errorCode)`

### Testing infrastructure
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` — TestContainers base with `assumeMinVersion()`
- `src/test/java/tech/amikos/chromadb/v2/CloudParityIntegrationTest.java` — Cloud test base with credential gating
- `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` — Integration test patterns

### External references
- Chroma fork API: `POST /api/v2/tenants/{t}/databases/{d}/collections/{id}/fork` — body: `{"new_name": "..."}`
- Chroma fork_count API: `GET /api/v2/tenants/{t}/databases/{d}/collections/{id}/fork_count` — response: `{"count": N}`
- Chroma indexing_status API: `GET /api/v2/tenants/{t}/databases/{d}/collections/{id}/indexing_status` — response: `{"num_indexed_ops":N, "num_unindexed_ops":N, "total_ops":N, "op_indexing_progress":F}`
- chroma-go Collection interface: `pkg/api/v2/collection.go` — Fork and IndexingStatus signatures
- chroma-go HTTP impl: `pkg/api/v2/collection_http.go` — Fork and IndexingStatus implementations
- chroma-go unit tests: `pkg/api/v2/collection_http_test.go` — Mock server testing pattern
- chroma-go cloud tests: `pkg/api/v2/client_cloud_test.go` — Cloud integration testing pattern

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChromaHttpCollection.modifyName(String)`: Direct HTTP call pattern (validate → build path → apiClient.put → update local state) — blueprint for fork()
- `ChromaHttpCollection.count()`: Simple GET returning a primitive — blueprint for forkCount()
- `ChromaHttpCollection.from(CollectionResponse, ...)`: Static factory for wrapping server response as Collection — reuse for fork() return value
- `Tenant` / `Database`: Immutable value objects with equals/hashCode — pattern for IndexingStatus

### Established Patterns
- **Interface-first**: Public interface on `Collection`, package-private `ChromaHttpCollection` implementation
- **Immutability**: Private constructor, factory method, defensive copies, unmodifiable collections
- **JavaBean getters**: `getName()`, `getId()`, `getMetadata()` — follow for IndexingStatus
- **Path builders**: Static methods on `ChromaApiPaths` — add `collectionFork()`, `collectionForkCount()`, `collectionIndexingStatus()`
- **DTO inner classes**: All in `ChromaDtos` as static inner classes with Gson annotations

### Integration Points
- `Collection` interface: Add `fork(String)`, `forkCount()`, `indexingStatus()` method signatures
- `ChromaHttpCollection`: Implement the three new methods
- `ChromaApiPaths`: Add three new endpoint path builders
- `ChromaDtos`: Add `ForkCollectionRequest`, `ForkCountResponse`, `IndexingStatusResponse`
- `IndexingStatus`: New public immutable value object in `tech.amikos.chromadb.v2`
- `README.md`: Add cloud vs self-hosted parity table
- `CHANGELOG.md`: Document new operations

</code_context>

<deferred>
## Deferred Ideas

- Cross-tenant/cross-database fork targeting — not supported by Chroma server, revisit if server adds it
- `IndexingStatus.isComplete()` convenience method — users can check `getOpIndexingProgress() >= 1.0` themselves
- Polling helper for indexing status (e.g., `awaitIndexing(Duration timeout)`) — application-level concern, not client library
- Fork with metadata/config overrides — not supported by Chroma server
- Fork quota management APIs — depends on Chroma server adding quota introspection endpoints

</deferred>

---

*Phase: 02-collection-api-extensions*
*Context gathered: 2026-03-21*
