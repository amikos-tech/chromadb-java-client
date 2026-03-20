# Phase 3: Embeddings & ID Extensibility - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Ensure embedding and ID workflows are deterministic, extensible, and safe for production ingestion/query paths. Normalize provider behavior, lock embedding resolution precedence, harden default embedding reliability, and strengthen ID generator validation. This phase does not add new embedding providers or ID schemes.

</domain>

<decisions>
## Implementation Decisions

### Provider error normalization
- All provider errors must be wrapped in `ChromaException` at the v2 boundary with provider name + model in the message (e.g., "OpenAI embedding failed (model: text-embedding-3-small): 429 rate limit exceeded").
- Providers must validate inputs eagerly: reject null/empty text lists before calling remote APIs. Consistent with Phase 1 fail-fast pattern.
- Partial results (returned embedding count != input count) must fail fast with a count-mismatch `ChromaException`.
- A shared conformance test suite (abstract base class) must assert consistent behavior across all providers: null rejection, empty-list rejection, count-match, ChromaException wrapping.
- Conformance tests run with mocked HTTP responses — no real API keys needed. Real-key tests stay in integration suite.

### Embedding precedence rules
- Precedence order is locked: runtime/explicit EF > `configuration.embedding_function` > `schema.default_embedding_function`. This matches the Go client (`amikos-tech/chroma-go`) pattern.
- Explicit EF always wins — no DefaultEF fall-through special-casing (unlike Python client).
- When explicit EF name differs from persisted config EF name, log a warning (not error). Users stay in control.
- Precedence must be documented in Javadoc on Collection interface and `ChromaHttpCollection.requireEmbeddingFunction()`, and locked with contract tests.
- Auto-wired EF resolution logged at DEBUG level (e.g., "Auto-wired embedding function: openai from collection configuration").
- Unsupported EF descriptors do not block collection construction — lazy fail on embed only. Collection remains usable for operations that don't need embedding (get by ID, delete).

### Default embedding reliability
- `DefaultEmbeddingFunction` must have a configurable timeout for ONNX model download. If timeout is hit, throw `ChromaException` with actionable message.
- Must be thread-safe for concurrent embedding calls. Model initialization should be lazy and synchronized.
- Download failures classified as retryable (timeout, 5xx, connection reset) vs non-retryable (404, 403, corrupt file). Retry once on retryable failures, fail fast on non-retryable.
- No static warmup method — lazy download only. Document first-use download behavior in Javadoc and README.

### ID generator validation edges
- Duplicate IDs within a single add/upsert batch must fail fast with `ChromaException` listing the duplicate IDs, before sending to server.
- Custom `IdGenerator` output (including lambdas) must be validated at the boundary: check for null/blank after `generate()` call. Throw `ChromaException` with record index on failure.
- `Sha256IdGenerator`: support metadata fallback when document is null. Hash serialized metadata for content-addressable dedup on embeddings-only records.
- `Sha256IdGenerator`: throw `IllegalArgumentException` when both document AND metadata are null ("requires a non-null document or metadata").

### Claude's Discretion
- Exact metadata serialization format for SHA-256 fallback hashing (as long as it's deterministic and documented).
- Exact conformance test structure (abstract class vs parameterized tests) as long as all providers are covered.
- Mocking approach for provider conformance tests (WireMock vs simple stubs) based on existing test infrastructure.
- Exact timeout default value for ONNX model download.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirement contracts
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, and plan scaffolding.
- `.planning/REQUIREMENTS.md` — EMB-01/EMB-02/EMB-03/EMB-04 acceptance targets.
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8 compatibility, sync API posture).
- `.planning/STATE.md` — current execution position and known test-environment caveat.

### Project conventions and architecture
- `CLAUDE.md` — repository conventions, test commands, and architecture notes.

### Embedding function infrastructure
- `src/main/java/tech/amikos/chromadb/embeddings/EmbeddingFunction.java` — core embedding interface contract.
- `src/main/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunction.java` — ONNX Runtime local embedding implementation.
- `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionResolver.java` — descriptor-to-runtime resolution logic.
- `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionSpec.java` — serializable embedding descriptor model.
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` — dual-source precedence logic (explicitEmbeddingFunction vs embeddingFunctionSpec).

### Provider implementations
- `src/main/java/tech/amikos/chromadb/embeddings/openai/` — OpenAI embedding provider.
- `src/main/java/tech/amikos/chromadb/embeddings/cohere/` — Cohere embedding provider.
- `src/main/java/tech/amikos/chromadb/embeddings/hf/` — HuggingFace embedding provider.
- `src/main/java/tech/amikos/chromadb/embeddings/ollama/` — Ollama embedding provider.

### ID generation infrastructure
- `src/main/java/tech/amikos/chromadb/v2/IdGenerator.java` — FunctionalInterface for ID generation.
- `src/main/java/tech/amikos/chromadb/v2/UuidIdGenerator.java` — Random UUID generator.
- `src/main/java/tech/amikos/chromadb/v2/UlidIdGenerator.java` — ULID generator with timestamp + random.
- `src/main/java/tech/amikos/chromadb/v2/Sha256IdGenerator.java` — Content-addressable SHA-256 generator.
- `src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java` — Existing ID generator test coverage.

### Cross-client parity reference
- `amikos-tech/chroma-go` — Go client embedding precedence pattern (runtime EF > config > schema, no DefaultEF fall-through).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EmbeddingFunctionResolver` already handles descriptor-to-runtime resolution for all 5 providers with error wrapping.
- `EmbeddingFunctionSpec` provides immutable descriptor with security-conscious `toString()` (redacts sensitive keys).
- `ChromaHttpCollection.requireEmbeddingFunction()` already implements lazy resolution with synchronized access.
- Three `IdGenerator` singletons (UUID, ULID, SHA-256) with thread-safe implementations.
- `WithParam` parameter abstraction for provider configuration.

### Established Patterns
- Fail-fast validation at setter/factory time (Phase 1 auth pattern).
- Lazy initialization with synchronized blocks (`requireEmbeddingFunction()`).
- Singleton instances for thread-safe stateless generators.
- WireMock-based contract testing for HTTP request/response assertions.

### Integration Points
- Provider error normalization wraps at `EmbeddingFunctionResolver.resolve()` and `ChromaHttpCollection.embedQueryTexts()`.
- Precedence documentation goes on `Collection` interface and `ChromaHttpCollection.requireEmbeddingFunction()`.
- Duplicate ID detection integrates in `AddBuilderImpl`/`UpsertBuilderImpl` before request construction.
- Custom ID output validation integrates at the ID generation call site in builder implementations.
- SHA-256 metadata fallback modifies `Sha256IdGenerator.generate()`.

</code_context>

<specifics>
## Specific Ideas

- Match Go client (`amikos-tech/chroma-go`) embedding precedence pattern — runtime EF always wins, no Python-style DefaultEF special-casing.
- Provider error messages should include both provider name AND model for multi-provider debugging.
- ONNX model download timeout + retry classification (retryable vs non-retryable) was a specific user request based on known CI stall issue.
- SHA-256 metadata fallback enables content-addressable dedup for embeddings-only records (no document text).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 3 scope.

</deferred>

---

*Phase: 03-embeddings-id-extensibility*
*Context gathered: 2026-03-19*
