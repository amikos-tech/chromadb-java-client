# Phase 2: API Coverage Completion - Context

**Gathered:** 2026-03-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Achieve complete and predictable v2 endpoint/operation parity for tenant/database lifecycle, collection schema/config behavior, and record-operation contracts required by current milestone requirements (`API-01`, `API-02`, `API-03`). This phase clarifies HOW parity is enforced; it does not add new product capabilities.

</domain>

<decisions>
## Implementation Decisions

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirement contracts
- `.planning/ROADMAP.md` — Phase 2 goal, success criteria, and plan scaffolding.
- `.planning/REQUIREMENTS.md` — API-01/API-02/API-03 acceptance targets.
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8 compatibility, sync API posture).
- `.planning/STATE.md` — current execution position and known test-environment caveat.

### Project conventions and architecture
- `CLAUDE.md` — repository conventions, test commands, and architecture notes.

### Client lifecycle and collection contract anchors
- `src/main/java/tech/amikos/chromadb/v2/Client.java` — public lifecycle/collection API contract.
- `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java` — lifecycle endpoint mapping, strict-field validation, list/get/create behavior.
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiPaths.java` — canonical v2 endpoint path construction.
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — record-operation API contract and builder semantics.
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — query/get/add/update/upsert/delete runtime behavior.
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` — request/response DTO serialization and schema/config parsing helpers.
- `src/main/java/tech/amikos/chromadb/v2/CreateCollectionOptions.java` — create/get-or-create option modeling.
- `src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java` — typed configuration model.
- `src/main/java/tech/amikos/chromadb/v2/Schema.java` — schema and CMEK typed model.

### Existing parity-focused tests to extend
- `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java` — lifecycle + mapping unit contract coverage.
- `src/test/java/tech/amikos/chromadb/v2/TenantDatabaseIntegrationTest.java` — tenant/database integration behavior.
- `src/test/java/tech/amikos/chromadb/v2/CollectionLifecycleIntegrationTest.java` — collection lifecycle parity.
- `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` — record-operation behavior parity.
- `src/test/java/tech/amikos/chromadb/v2/SchemaAndQueryTextsIntegrationTest.java` — schema/queryTexts integration paths.
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` — builder/runtime edge-case behavior.
- `src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java` — DTO/schema/config serialization/deserialization contracts.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChromaClient.ChromaClientImpl` already centralizes lifecycle endpoint mapping and strict deserialization guardrails.
- `ChromaHttpCollection` already owns record-operation builder validation and execution pathways.
- `ChromaDtos` already provides typed schema/config/CMEK parsing and map conversion helpers.
- Existing integration + contract suites already cover most endpoint families and can be expanded rather than replaced.

### Established Patterns
- Fail-fast typed exception behavior is treated as API contract, not implementation detail.
- Request payloads are composed via DTOs with explicit field names and minimal implicit normalization.
- Collection and record operations use fluent builders with validation at execute-time for multi-field consistency.

### Integration Points
- Lifecycle fallback policy changes integrate in `ChromaClient` create flows only.
- Schema/config precedence and unknown-key preservation touch `ChromaDtos` + `ChromaHttpCollection.from(...)` mapping boundaries.
- Query input parity policy (`queryTexts` + `queryEmbeddings`) integrates in `ChromaHttpCollection.QueryBuilderImpl`.
- Parity gating is enforced by extending current test suites rather than introducing a separate testing framework.

</code_context>

<specifics>
## Specific Ideas

- Align `queryTexts`/`queryEmbeddings` dual-input behavior with current Chroma Python/Go runtime parity (embeddings authoritative when both are present).
- Keep compatibility practical: latest Chroma `1.5.5` as baseline plus one additional `1.x` representative check.
- Preserve scope discipline by converting any new capability discovery into an inserted-phase decision.

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within Phase 2 scope. New capability discoveries during implementation should trigger an inserted-phase decision instead of in-phase expansion.

</deferred>

---

*Phase: 02-api-coverage-completion*
*Context gathered: 2026-03-18*
