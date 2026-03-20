# Phase 2: API Coverage Completion - Research

**Researched:** 2026-03-18
**Domain:** Chroma v2 lifecycle + collection/record parity for Java client contracts
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Lifecycle parity strictness
- Lifecycle reads stay strict fail-fast on malformed payloads (for example missing/blank tenant/database/collection required fields).
- `createTenant` and `createDatabase` are response-authoritative when response fields are present.
- Compatibility fallback is allowed for `createTenant`/`createDatabase` only: if response name field is missing/null, return request-authoritative value.
- That fallback does not apply to `getTenant`/`getDatabase` or list endpoints.

### Schema/config/CMEK round-trip semantics
- Top-level `schema` is canonical when both top-level `schema` and `configuration.schema` are present.
- On create/get-or-create request construction, if caller sets both schema locations and they conflict, allow both and treat top-level `schema` as authoritative behavior.
- Preserve unknown schema/config/CMEK keys losslessly where possible.
- Delivery priority is read-path preservation first (`getCollection`/`listCollections` mapping fidelity).

### Record operation behavior contract
- Client validates structural request shape; server remains source of truth for semantic operator/type rules.
- For `include(...)`: if omitted, defer to server defaults; if provided, send exactly caller-requested include values.
- Query parity follows current Python/Go behavior: allow both `queryTexts` and `queryEmbeddings`, treat embeddings as authoritative, and only embed query texts when embeddings are absent.

### Parity proof depth and gating
- Phase completion gate requires targeted contract + integration suites for Phase 2 scope.
- Full `mvn test` is best-effort for this phase; if blocked by known external/environment stalls, document blocker explicitly rather than silently ignoring.
- Compatibility baseline targets latest Chroma `1.5.5`, plus at least one additional representative `1.x` targeted check.
- Per endpoint family under Phase 2 scope, tests must assert both request contract and response mapping.
- If broader/new capability work appears, stop and open an inserted phase decision instead of expanding scope in-place.

### Claude's Discretion
- Exact split of test classes and scenarios across 02-01/02-02/02-03 plans.
- Selection of the additional representative `1.x` version beyond `1.5.5`.
- Exact assertion style/fixtures as long as request-contract + response-mapping gates are satisfied.

### Deferred Ideas (OUT OF SCOPE)
None - discussion stayed within Phase 2 scope. New capability discoveries during implementation should trigger an inserted-phase decision instead of in-phase expansion.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| API-01 | User can manage tenants and databases through v2 client methods (create, get, list, delete) with typed results. | Verified v2 endpoint/method surface, lifecycle strictness/fallback contract, and test mapping for tenant/database CRUD + list behavior. |
| API-02 | User can manage collections with schema/config/CMEK options without lossy serialization. | Verified collection/schema/config payload contracts, precedence rules, and identified preservation gaps for unknown keys and config-shape drift. |
| API-03 | User can execute add/get/query/update/upsert/delete with full filter/include/queryTexts behavior expected by Chroma v2. | Verified record operation payload contracts, include/filter expectations, and query input precedence behavior across Chroma Python/Go implementations. |
</phase_requirements>

## Summary

Phase 2 is mostly a contract-hardening and parity-closure phase, not a greenfield implementation. The current v2 Java client already covers the primary lifecycle and record endpoints, but there are concrete parity gaps against locked decisions: `createTenant`/`createDatabase` currently ignore response fields and always return request values; query builder currently rejects mixed `queryTexts` + `queryEmbeddings`; schema/config/CMEK mapping remains lossy for unknown keys.

Official Chroma references are not fully self-consistent: docs/OpenAPI emphasize `query_embeddings` transport for record query, while current Python and Go client implementations both allow mixed query inputs and treat embeddings as authoritative when provided. That behavior exactly matches the locked phase decision, so this phase should codify it explicitly and gate it with tests rather than rely on ambiguous docs language.

Compatibility baseline should be enforced in tests as decided: Chroma `1.5.5` plus one additional representative `1.x`. In this repo, targeted integration checks were runnable with `CHROMA_VERSION=1.5.5` and `CHROMA_VERSION=1.3.7`, so the plan can safely include a two-version gate for parity-sensitive scenarios.

**Primary recommendation:** Implement Phase 2 as three focused tracks: (1) lifecycle response-authoritative mapping with create-only fallback, (2) lossless schema/config/CMEK mapping boundaries, and (3) query/input/include parity plus contract+integration gate coverage across `1.5.5` and `1.3.7`.

## Standard Stack

### Core
| Library/Component | Version | Purpose | Why Standard |
|-------------------|---------|---------|--------------|
| Java | 8 target | Runtime/API compatibility baseline | Project constraint and public compatibility guardrail. |
| OkHttp | 4.12.0 (repo pin) | HTTP transport | Existing client transport with stable sync behavior. |
| Gson | 2.10.1 (repo pin) | DTO serialization/deserialization | Existing DTO contract layer used across v2 client. |
| Chroma v2 API | OpenAPI 3.1 (`info.version`=`1.0.0`) | Server contract reference | Authoritative endpoint/payload reference for parity work. |

### Supporting
| Library/Component | Version | Purpose | When to Use |
|-------------------|---------|---------|-------------|
| JUnit | 4.13.2 | Unit/contract tests | Endpoint payload mapping and validation-path tests. |
| WireMock | 2.35.2 | HTTP contract stubbing | Request/response contract assertions for lifecycle/record APIs. |
| Testcontainers | BOM 1.21.4 (repo pin) | Integration test runtime | Versioned Chroma parity checks (`CHROMA_VERSION`). |
| Chroma Docker image | `1.5.5` + `1.3.7` | Compatibility matrix targets for this phase | Required by locked parity gate depth. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Existing hand-written DTO layer | OpenAPI-generated Java client | Too disruptive for Phase 2; would shift scope from parity closure to architecture rewrite. |
| Current fluent sync API | Search API or async-first client surface | Out of Phase 2 scope and not required by API-01/02/03. |

**Installation:**
```bash
# No new dependencies required for Phase 2.
mvn -q clean compile
```

**Version verification:** verified from authoritative registries/docs during research.
```bash
# Chroma API contract
curl -sS https://api.trychroma.com:8000/openapi.json | jq '.openapi, .info.version'

# Chroma image tags (stable 1.x)
curl -sS 'https://registry.hub.docker.com/v2/repositories/chromadb/chroma/tags?page_size=100'
```

## Architecture Patterns

### Recommended Project Structure
```text
src/
├── main/java/tech/amikos/chromadb/v2/   # API contracts, DTO mapping, transport client
├── test/java/tech/amikos/chromadb/v2/   # unit + contract + integration tests
└── test/resources/                       # fixture payloads (add for parity edge-cases)
```

### Pattern 1: Response-Authoritative Create with Scoped Fallback
**What:** For `createTenant`/`createDatabase`, use response name when present; fallback to request name only when response is missing/null/blank.
**When to use:** Create operations only.
**Example:**
```java
ChromaDtos.TenantResponse dto = apiClient.post(path, request, ChromaDtos.TenantResponse.class);
String effectiveName = (dto != null && dto.name != null && !dto.name.trim().isEmpty())
        ? dto.name.trim()
        : requestName;
return Tenant.of(effectiveName);
```

### Pattern 2: Schema Precedence + Lossless Mapping Boundary
**What:** Keep top-level `schema` canonical for reads while preserving unknown schema/config/CMEK keys where possible.
**When to use:** `getCollection`, `listCollections`, and collection create/get-or-create payload composition.
**Example:**
```java
Schema effectiveSchema = topLevelSchema != null ? topLevelSchema : configSchema;
// Preserve unknown keys by retaining raw maps alongside typed projections.
```

### Pattern 3: Query Input Precedence (Embeddings First)
**What:** Allow both query inputs; if embeddings are present, use them directly and skip text embedding.
**When to use:** `Collection.query()` builder execution.
**Example:**
```java
List<float[]> resolved = queryEmbeddings;
if ((resolved == null || resolved.isEmpty()) && queryTexts != null && !queryTexts.isEmpty()) {
    resolved = embedQueryTexts(queryTexts);
}
```

### Anti-Patterns to Avoid
- **Setter-time exclusivity for query inputs:** blocks locked parity behavior; enforce precedence at execute-time instead.
- **Dropping unknown config/schema keys during parse/serialize:** causes lossy round-trip and breaks API-02.
- **Client-side semantic validation of server DSL/operator rules:** creates drift; keep only structural validation client-side.
- **Forcing include defaults client-side:** violates server-authoritative default behavior when `include` is omitted.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Endpoint URL assembly | Inline string concatenation in each method | `ChromaApiPaths` | Prevents path drift/encoding bugs across endpoint families. |
| HTTP error translation | Per-call status switch logic | `ChromaExceptions.fromHttpResponse(...)` | Keeps typed exception contract consistent. |
| JSON contract mapping | Ad hoc `Map<String,Object>` everywhere | `ChromaDtos` + typed value objects | Maintains predictable request/response behavior. |
| Integration runtime setup | Custom Docker shell orchestration | `AbstractChromaIntegrationTest` + Testcontainers | Reproducible versioned parity checks and less test flakiness. |

**Key insight:** Phase 2 is mainly contract correction; reusing existing path/DTO/test infrastructure minimizes risk and keeps the phase scoped to parity.

## Common Pitfalls

### Pitfall 1: Trusting docs text over runtime client behavior
**What goes wrong:** Planner assumes query inputs must be mutually exclusive.
**Why it happens:** Reference docs language and OpenAPI transport schema focus on `query_embeddings`.
**How to avoid:** Follow locked decision and validate against Python/Go implementation behavior and dedicated tests.
**Warning signs:** Tests assert mixed-input rejection; Java behavior diverges from chosen parity target.

### Pitfall 2: Lossy schema/config/CMEK mapping
**What goes wrong:** Unknown keys disappear across read-modify-write flows.
**Why it happens:** Current typed parsing keeps known fields only.
**How to avoid:** Add raw-map preservation strategy where feasible and verify round-trip fidelity with contract tests.
**Warning signs:** Snapshot payload diffs drop keys not represented by current value objects.

### Pitfall 3: Over-reading OpenAPI for tenant/database semantics
**What goes wrong:** Planner adds unsupported tenant list/delete tasks.
**Why it happens:** Requirement wording is broad; v2 tenant API surface is create/get/update only.
**How to avoid:** Scope API-01 to actual endpoint surface plus existing requirement intent.
**Warning signs:** New tasks target non-existent tenant endpoints.

### Pitfall 4: Version-matrix assumptions without execution
**What goes wrong:** Parity passes locally on one version and regresses on another.
**Why it happens:** Only default integration version is exercised.
**How to avoid:** Gate with baseline `1.5.5` plus representative `1.3.7`.
**Warning signs:** Assertions depend on response-shape nuances and fail across versions.

## Code Examples

Verified patterns from official/current sources:

### Mixed Query Inputs with Embeddings Authoritative
```java
// Accept both setters.
queryBuilder.queryTexts("q1").queryEmbeddings(new float[]{1f, 2f, 3f});
// Execute should use queryEmbeddings directly; queryTexts only used if embeddings missing.
```

### Lifecycle Create Fallback Contract Test Shape
```java
// 1) Response contains name -> assert response-authoritative value.
// 2) Response missing name -> assert request-authoritative fallback.
```

### OpenAPI Contract Snapshot for Endpoint Surface
```bash
curl -sS https://api.trychroma.com:8000/openapi.json \
  | jq -r '.paths | keys[]' | sort
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Treat query inputs as mutually exclusive | Cross-client behavior allows both inputs; embeddings take precedence | Verified in current Chroma Python/Go source (2026-03-18 research) | Java parity should align with locked Phase 2 behavior. |
| Assume flat-only config representation (`hnsw:*` keys) | v2 contracts also expose nested config objects (`hnsw`/`spann`) in API docs/OpenAPI | Observed in current OpenAPI (2026-03-18) | Mapping layer must avoid silent data loss/drift. |
| Assume tenant CRUD parity includes list/delete | Tenant API in current v2 references: create/get/update only | Verified from current docs/OpenAPI (2026-03-18) | Prevents out-of-scope tasks and false endpoint assumptions. |
| Default integration version (`1.5.2`) only | Locked gate requires `1.5.5` + one more `1.x` | Phase 2 decision (2026-03-18) | Plan must include explicit versioned parity checks. |

**Deprecated/outdated:**
- Query-builder rejection of mixed `queryTexts` + `queryEmbeddings` in Java tests/implementation is outdated for this phase target.
- Flat-only config parsing assumptions are risky against current API descriptions and should not drive new parity behavior.

## Open Questions

1. **Unknown-key preservation model boundary**
   - What we know: API-02 requires lossless preservation where possible; current typed model drops unknown keys.
   - What's unclear: Exact object model strategy (raw map shadow fields vs dedicated extension maps).
   - Recommendation: Decide this in plan 02-02 and lock one approach before broad test updates.

2. **Database list payload schema inconsistency in references**
   - What we know: OpenAPI schema for list databases appears generic/inconsistent; integration tests still pass with current mapping.
   - What's unclear: Whether this is docs/openapi generation artifact or runtime variation.
   - Recommendation: Add WireMock contract test fixture for both known response shapes; keep strict mapping errors explicit.

3. **Tenant update endpoint scope**
   - What we know: `PATCH /api/v2/tenants/{tenant_name}` exists in current docs/OpenAPI.
   - What's unclear: Whether API-01 intends this in current phase despite requirement wording.
   - Recommendation: Keep out unless planner explicitly reinterprets API-01; otherwise avoid scope expansion.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Maven Surefire 3.0.0 + Testcontainers ChromaDB |
| Config file | `pom.xml` |
| Quick run command | `mvn -q -Dtest=ChromaApiPathsTest,ChromaClientImplTest,ChromaHttpCollectionTest,ChromaDtosContractTest test` |
| Full suite command | `mvn -q test && CHROMA_VERSION=1.5.5 mvn -q -Pintegration test && CHROMA_VERSION=1.3.7 mvn -q -Pintegration test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-01 | Tenant/database create/get/list/delete typed mapping + create fallback policy | unit + integration | `mvn -q -Dtest=ChromaClientImplTest test && CHROMA_VERSION=1.5.5 mvn -q -Pintegration -Dtest=TenantDatabaseIntegrationTest test` | ✅ |
| API-02 | Collection schema/config/CMEK round-trip fidelity and precedence | unit + integration | `mvn -q -Dtest=ChromaDtosContractTest,ChromaClientImplTest,ChromaHttpCollectionTest test && CHROMA_VERSION=1.5.5 mvn -q -Pintegration -Dtest=CollectionLifecycleIntegrationTest,SchemaAndQueryTextsIntegrationTest test` | ✅ |
| API-03 | add/get/query/update/upsert/delete with full where/include/queryTexts behavior | unit + integration | `mvn -q -Dtest=ChromaHttpCollectionTest test && CHROMA_VERSION=1.5.5 mvn -q -Pintegration -Dtest=RecordOperationsIntegrationTest,SchemaAndQueryTextsIntegrationTest test` | ✅ |

### Sampling Rate
- **Per task commit:** `mvn -q -Dtest=ChromaClientImplTest,ChromaHttpCollectionTest,ChromaDtosContractTest test`
- **Per wave merge:** `mvn -q test` plus one targeted integration command for touched endpoint family
- **Phase gate:** Full suite green including both compatibility versions before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java` — add create lifecycle response-authoritative + fallback tests for tenant/database.
- [ ] `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` — replace mixed-input rejection assertions with embeddings-authoritative parity assertions.
- [ ] `src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java` — add unknown schema/config/CMEK key preservation tests.
- [ ] Add matrix helper (or documented commands) for `CHROMA_VERSION=1.5.5` and `CHROMA_VERSION=1.3.7` parity checks.

## Sources

### Primary (HIGH confidence)
- https://api.trychroma.com:8000/openapi.json - current v2 endpoint + schema contract surface.
- https://docs.trychroma.com/reference/chroma-api/tenant/get-tenant - tenant endpoint contract.
- https://docs.trychroma.com/reference/chroma-api/tenant/update-tenant - tenant endpoint navigation/surface (`create/get/update`).
- https://docs.trychroma.com/reference/chroma-api/record/get-records - get payload/response shape and include semantics.
- https://docs.trychroma.com/reference/chroma-api/record/query-collection - query payload/response shape and include semantics.
- https://docs.trychroma.com/docs/querying-collections/query-and-get - SDK-level query/get behavior guidance.
- https://github.com/chroma-core/chroma/blob/39a2162e6c5f987df0046f58a7257ab887ad31b8/chromadb/api/models/CollectionCommon.py#L295 - Python query request preparation behavior.
- https://github.com/chroma-core/chroma/blob/39a2162e6c5f987df0046f58a7257ab887ad31b8/chromadb/api/types.py#L418 - Python base query record validation behavior.
- https://github.com/amikos-tech/chroma-go/blob/8936935878afc5f4f9ac3c8d6f0161ebd1cf166b/pkg/api/v2/collection.go#L501 - Go query embedding precedence behavior.
- https://github.com/amikos-tech/chroma-go/blob/8936935878afc5f4f9ac3c8d6f0161ebd1cf166b/pkg/api/v2/options.go#L709 - Go query input option behavior.
- https://registry.hub.docker.com/v2/repositories/chromadb/chroma/tags?page_size=100 - available stable 1.x image tags used for matrix recommendation.

### Secondary (MEDIUM confidence)
- https://docs.trychroma.com/reference/typescript/client - admin/client method surface cross-check for tenant/database lifecycle expectations.
- https://cookbook.chromadb.dev/core/api/ - official pointer to `/openapi.json` and docs endpoints.

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - directly verified from repo pins, Maven registry, and active test execution.
- Architecture: MEDIUM - core patterns are clear, but unknown-key preservation design still requires implementation choice.
- Pitfalls: MEDIUM - based on confirmed docs/source mismatches and current code behavior.

**Research date:** 2026-03-18
**Valid until:** 2026-03-25
