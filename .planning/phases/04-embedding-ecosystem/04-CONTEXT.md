# Phase 4: Embedding Ecosystem - Context

**Gathered:** 2026-04-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Expand the embedding ecosystem with sparse embedding functions, content-based multimodal embedding functions, a reranking interface, three new dense embedding providers, and a public embedding function registry with auto-wiring. This phase does not change existing client/collection behavior or existing embedding provider implementations.

</domain>

<decisions>
## Implementation Decisions

### Sparse Embedding Interface
- **D-01:** `SparseEmbeddingFunction` is a **separate interface** from `EmbeddingFunction` — not an extension, not a generic base. Returns `List<SparseVector>` (reusing existing v2 `SparseVector` type).
- **D-02:** Two sparse providers: **Chroma Cloud Splade** (remote) and **BM25** (local).
- **D-03:** BM25 implementation **mirrors the Go client's approach**: custom tokenizer pipeline (lowercase → regex split → stopword filter → Snowball stemmer → Murmur3 hashing). Ensures cross-client index compatibility.
- **D-04:** BM25 needs two new dependencies: a Java Snowball stemmer library and a Murmur3 hashing library (or minimal bundled Murmur3 impl). Both should be small.

### Content/Multimodal Embedding Interface
- **D-05:** **2-tier design** — skip `MultimodalEmbeddingFunction` (already legacy in Go). Only `ContentEmbeddingFunction` is added alongside existing `EmbeddingFunction`.
- **D-06:** `ContentEmbeddingFunction` interface: `embedContents(List<Content>) → List<Embedding>` with default `embedContent(Content) → Embedding`.
- **D-07:** Content/Part/BinarySource types follow **static factory + builder pattern** matching codebase conventions:
  - `Content.text("...")` for simple case, `Content.builder()` for complex (like `CollectionConfiguration.builder()`)
  - `Part.text(str)`, `Part.image(source)`, `Part.audio(source)` etc. — static factories (like `SparseVector.of()`)
  - `BinarySource.fromUrl(url)`, `.fromFile(path)`, `.fromBase64(data)`, `.fromBytes(data)` — static factories
- **D-08:** `Modality` and `Intent` are **Java enums** with `getValue()` and `fromValue(String)` — same pattern as `DistanceFunction` and `Include`.
- **D-09:** All multimodal types live in **`tech.amikos.chromadb.embeddings.content`** sub-package. Existing `EmbeddingFunction` stays in `tech.amikos.chromadb.embeddings`.
- **D-10:** **Adapter pattern**: `TextEmbeddingAdapter` wraps `EmbeddingFunction` → `ContentEmbeddingFunction`. `ContentToTextAdapter` wraps the reverse. Public classes + `ContentEmbeddingFunction.fromTextOnly(ef)` convenience factory.
- **D-11:** `CapabilityMetadata` **deferred** — not needed for v1, can be added later as non-breaking addition.

### New Dense Providers
- **D-12:** Three new dense providers: **Gemini**, **Bedrock**, **Voyage** (prioritized per requirements).
- **D-13:** Dependency strategy: **Vendor SDKs for major labs** (Google AI SDK for Gemini, AWS SDK for Bedrock), **OkHttp for smaller providers** (Voyage). First-party SDKs are mature, secure, and handle auth natively.
- **D-14:** Vendor SDK dependencies use **optional/provided Maven scope** — not pulled transitively into user projects.
- **D-15:** All providers use the established `WithParam` configuration pattern and register with the same provider names as the Go client for cross-client compatibility.

### Embedding Function Registry
- **D-16:** Public `EmbeddingFunctionRegistry` wraps the existing package-private `EmbeddingFunctionResolver`. Resolver becomes an internal detail pre-registering built-in providers.
- **D-17:** **3 separate factory maps** (dense, sparse, content) — aligned with Go client's pattern. No multimodal map (skipped per D-05).
- **D-18:** **Content fallback chain** on resolve: tries content factory first → falls back to dense + adapter wrapping. Any registered dense provider automatically works with content API.
- **D-19:** **Singleton + instance API**: `EmbeddingFunctionRegistry.getDefault()` returns shared singleton with built-in providers pre-registered. Users can also create custom instances (useful for testing).
- **D-20:** Registration API: `registry.registerDense(name, factory)`, `registerSparse(name, factory)`, `registerContent(name, factory)`. Thread-safe with synchronized access.
- **D-21:** Provider names match Go client: `"openai"`, `"cohere"`, `"google_genai"`, `"amazon_bedrock"`, `"voyageai"`, `"chroma_bm25"`, `"bm25"` (alias), `"chromacloud_splade"`, etc.

### Reranking
- **D-22:** `RerankingFunction` interface: `rerank(query, List<String> documents) → List<RerankResult>` where `RerankResult` has score + index.
- **D-23:** **Two providers**: Cohere Rerank and Jina Reranker. Both are simple REST APIs using OkHttp.

### Claude's Discretion
- Exact Snowball stemmer and Murmur3 library choices (as long as they're lightweight and Java 8 compatible)
- Exact Google AI SDK and AWS SDK artifact coordinates and versions
- Internal class organization within each provider package
- Exact method signatures for registry factory functional interfaces
- Whether to add `Closeable` support to registry-resolved instances (Go has it)
- Stop word list for BM25 (should match Go/Python client defaults)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirement contracts
- `.planning/ROADMAP.md` — Phase 4 goal, success criteria (EMB-05, EMB-06, EMB-07, EMB-08, RERANK-01)
- `.planning/REQUIREMENTS.md` — EMB-05 through EMB-08, RERANK-01 acceptance targets
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8, sync API)

### Project conventions
- `CLAUDE.md` — repository conventions, test commands, architecture notes

### Go client reference implementations
- `https://github.com/amikos-tech/chroma-go/tree/main/pkg/embeddings` — Go embedding interfaces, registry, content types
- `https://github.com/amikos-tech/chroma-go/tree/main/pkg/embeddings/bm25` — Go BM25 implementation (tokenizer, Murmur3, stop words)
- Go registry pattern: global singleton, 4 maps (dense/sparse/multimodal/content), `Register*()` + `Build*()`, content fallback chain

### Existing embedding infrastructure
- `src/main/java/tech/amikos/chromadb/embeddings/EmbeddingFunction.java` — current text-only interface (stays unchanged)
- `src/main/java/tech/amikos/chromadb/embeddings/WithParam.java` — configuration parameter pattern (reuse for new providers)
- `src/main/java/tech/amikos/chromadb/embeddings/openai/OpenAIEmbeddingFunction.java` — reference provider implementation pattern
- `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionResolver.java` — existing package-private auto-wirer (to be wrapped by public registry)
- `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionSpec.java` — descriptor with type/name/config (registry resolves from this)

### Existing sparse vector types
- `src/main/java/tech/amikos/chromadb/v2/SparseVector.java` — immutable sparse vector (int[] indices + float[] values), reuse in SparseEmbeddingFunction
- `src/main/java/tech/amikos/chromadb/Embedding.java` — dense float[] wrapper (stays unchanged)

### Build configuration
- `pom.xml` — Maven build config: add new dependencies here (Snowball, Murmur3, Google AI SDK, AWS SDK as optional/provided)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EmbeddingFunction` interface — text-only contract, stays as-is. New interfaces alongside it.
- `WithParam` pattern — reuse for all new provider constructors (apiKey, model, baseAPI, etc.)
- `SparseVector` — already in v2 package, reuse as return type for `SparseEmbeddingFunction`.
- `EmbeddingFunctionResolver` — wrappable by public registry. Already handles 5 known providers.
- `EmbeddingFunctionSpec` — descriptor format matches Go's `EmbeddingFunctionConfig` concept.
- OkHttp + Gson — all existing providers use this HTTP/JSON stack. Reuse for Voyage, Cohere Rerank, Jina Reranker.

### Established Patterns
- Provider package structure: `embeddings/{provider}/` with `{Provider}EmbeddingFunction.java`, `CreateEmbeddingRequest.java`, `CreateEmbeddingResponse.java`.
- Defaults applied first, then user `WithParam` overrides (see `OpenAIEmbeddingFunction` constructor).
- `ChromaException` thrown for HTTP errors; `EFException` for provider-specific failures.
- Java 8 compatible: no records, no sealed classes, default methods on interfaces OK.

### Integration Points
- New `SparseEmbeddingFunction` integrates with `SparseVector` (v2 package) for return types.
- New `ContentEmbeddingFunction` integrates with existing `Embedding` class for return types.
- `EmbeddingFunctionRegistry` wraps and replaces `EmbeddingFunctionResolver` as the public API entry point.
- New provider packages follow `embeddings/{provider}/` convention.
- Content types live in new `embeddings/content/` sub-package.
- Maven dependencies: Snowball + Murmur3 as compile deps, Google AI SDK + AWS SDK as optional/provided.

</code_context>

<specifics>
## Specific Ideas

- Mirror Go client's BM25 tokenizer pipeline exactly for cross-client sparse vector index compatibility (same stop words, same Murmur3 hashing, same stemmer behavior).
- Use same provider registration names as Go client for cross-client config compatibility (`"google_genai"`, `"amazon_bedrock"`, `"voyageai"`, `"chroma_bm25"`, etc.).
- Content fallback chain (content → dense+adapter) ensures any dense provider works with the content API without explicit content registration — this is a key usability win.
- First-party vendor SDKs for major cloud providers (Google, AWS) — mature auth handling, security, and maintenance. OkHttp for indie providers.

</specifics>

<deferred>
## Deferred Ideas

- **CapabilityMetadata** — provider capability declaration (supported modalities, intents, batching). Can be added as non-breaking enhancement after v1.
- **Closeable support** in registry — Go has `BuildDenseCloseable()` for cleanup. Can add later.
- **Additional providers** beyond Gemini/Bedrock/Voyage — Jina embeddings, Mistral, Together, Nomic, etc. can be added incrementally.
- **MultimodalEmbeddingFunction** middle-tier interface — intentionally skipped (legacy in Go). If needed, can be added later as adapter target.

</deferred>

---

*Phase: 04-embedding-ecosystem*
*Context gathered: 2026-04-01*
