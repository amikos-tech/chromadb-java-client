# Phase 4: Embedding Ecosystem - Research

**Researched:** 2026-04-01
**Domain:** Java embedding functions — sparse/content interfaces, BM25, new dense providers (Gemini, Bedrock, Voyage), reranking (Cohere, Jina), EmbeddingFunctionRegistry
**Confidence:** HIGH (core patterns from existing codebase + Go reference client; API wire formats verified from official docs)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `SparseEmbeddingFunction` is a **separate interface** from `EmbeddingFunction` — not an extension, not a generic base. Returns `List<SparseVector>`.
- **D-02:** Two sparse providers: **Chroma Cloud Splade** (remote) and **BM25** (local).
- **D-03:** BM25 tokenizer pipeline: lowercase → regex split → stopword filter → Snowball stemmer → Murmur3 hashing. Must match Go client for index compatibility.
- **D-04:** BM25 needs a Java Snowball stemmer library and a Murmur3 hashing library (or minimal bundled impl). Both must be small.
- **D-05:** **2-tier design only** — skip `MultimodalEmbeddingFunction`. Only `ContentEmbeddingFunction` is added.
- **D-06:** `ContentEmbeddingFunction` interface: `embedContents(List<Content>) → List<Embedding>` with default `embedContent(Content) → Embedding`.
- **D-07:** Content/Part/BinarySource types use static factory + builder pattern: `Content.text("...")`, `Content.builder()`, `Part.text(str)`, `Part.image(source)`, `BinarySource.fromUrl(url)`, `.fromFile(path)`, `.fromBase64(data)`, `.fromBytes(data)`.
- **D-08:** `Modality` and `Intent` are Java enums with `getValue()` and `fromValue(String)`.
- **D-09:** All multimodal types live in `tech.amikos.chromadb.embeddings.content` sub-package.
- **D-10:** Adapter pattern: `TextEmbeddingAdapter` wraps `EmbeddingFunction` → `ContentEmbeddingFunction`. `ContentToTextAdapter` wraps the reverse. Plus `ContentEmbeddingFunction.fromTextOnly(ef)` convenience factory.
- **D-11:** `CapabilityMetadata` deferred.
- **D-12:** Three new dense providers: **Gemini**, **Bedrock**, **Voyage**.
- **D-13:** Google AI SDK for Gemini, AWS SDK for Bedrock, OkHttp for Voyage.
- **D-14:** Vendor SDK dependencies use **optional/provided Maven scope**.
- **D-15:** All providers use `WithParam` configuration pattern; provider names match Go client.
- **D-16:** `EmbeddingFunctionRegistry` wraps existing package-private `EmbeddingFunctionResolver`. Resolver becomes internal.
- **D-17:** 3 separate factory maps (dense, sparse, content). No multimodal map.
- **D-18:** Content fallback chain: content factory first → dense + adapter wrapping.
- **D-19:** Singleton (`getDefault()`) + instance API. Singleton pre-registers built-in providers.
- **D-20:** `registry.registerDense(name, factory)`, `registerSparse(name, factory)`, `registerContent(name, factory)`. Thread-safe via synchronized access.
- **D-21:** Provider names: `"openai"`, `"cohere"`, `"google_genai"`, `"amazon_bedrock"`, `"voyageai"`, `"chroma_bm25"`, `"bm25"` (alias), `"chromacloud_splade"`.
- **D-22:** `RerankingFunction` interface: `rerank(query, List<String> documents) → List<RerankResult>` where `RerankResult` has score + index.
- **D-23:** Two reranking providers: Cohere Rerank and Jina Reranker. Both use OkHttp.

### Claude's Discretion

- Exact Snowball stemmer and Murmur3 library choices (lightweight, Java 8 compatible)
- Exact Google AI SDK and AWS SDK artifact coordinates and versions
- Internal class organization within each provider package
- Exact method signatures for registry factory functional interfaces
- Whether to add `Closeable` support to registry-resolved instances
- Stop word list for BM25 (should match Go/Python client defaults)

### Deferred Ideas (OUT OF SCOPE)

- `CapabilityMetadata` — provider capability declaration
- `Closeable` support in registry
- Additional providers beyond Gemini/Bedrock/Voyage
- `MultimodalEmbeddingFunction` middle-tier interface
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EMB-05 | User can use sparse embedding functions (BM25, Chroma Cloud Splade) through a `SparseEmbeddingFunction` interface | BM25 tokenizer pipeline verified from Go client; Splade API endpoint verified; `SparseVector` type exists in codebase |
| EMB-06 | User can use multimodal embedding functions (image+text) through a `ContentEmbeddingFunction` interface | Note: CONTEXT.md D-05 clarifies requirement as `ContentEmbeddingFunction` (not `MultimodalEmbeddingFunction`); Go reference types documented |
| EMB-07 | User can use at least 3 additional dense embedding providers (Gemini, Bedrock, Voyage) | Wire formats, endpoints, auth headers, and SDK artifacts verified for all three |
| EMB-08 | User can rely on an `EmbeddingFunctionRegistry` to auto-wire embedding functions from server-side collection configuration | Go registry pattern documented; existing `EmbeddingFunctionResolver` identified as internal to wrap |
| RERANK-01 | User can rerank query results using a `RerankingFunction` interface with at least one provider (Cohere or Jina) | Both Cohere Rerank v2 and Jina Reranker API documented; Go `RerankingFunction` interface inspected |
</phase_requirements>

---

## Summary

Phase 4 expands the embedding ecosystem into five distinct areas: sparse embeddings (BM25 + Chroma Cloud Splade), a content embedding interface for multimodal inputs, three new dense providers (Gemini, Bedrock, Voyage), a public `EmbeddingFunctionRegistry`, and a reranking interface with two providers.

All patterns are well-established. The existing codebase provides strong scaffolding: `EmbeddingFunction`, `WithParam`, `SparseVector`, `EmbeddingFunctionResolver`, `EmbeddingFunctionSpec`, `AbstractEmbeddingFunctionConformanceTest`, and `DistanceFunction` (enum pattern for `Modality`/`Intent`). New code follows these patterns without exception.

The most complex new component is BM25: the tokenizer pipeline must match the Go client exactly (same stop words, same Murmur3 seed/behavior, same Snowball English stemmer) for cross-client sparse vector index compatibility. The BM25 scoring formula, hash-based index mapping, and sparse vector construction are all documented from the Go reference. Everything else (new dense providers, reranking, content types, registry) follows straightforward patterns already established in the codebase.

**Primary recommendation:** Implement in this order: (1) interfaces first (`SparseEmbeddingFunction`, `ContentEmbeddingFunction`, `RerankingFunction` + value types), (2) `EmbeddingFunctionRegistry` wrapping existing resolver, (3) BM25 (most complex), (4) Chroma Cloud Splade, (5) Gemini/Bedrock/Voyage dense providers, (6) Cohere Rerank + Jina Reranker.

---

## Standard Stack

### Core (existing — reuse as-is)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| OkHttp | 4.12.0 | HTTP client for all remote providers (Voyage, Splade, Cohere Rerank, Jina) | Already in pom.xml as main dep |
| Gson | 2.10.1 | JSON serialization for all provider request/response DTOs | Already in pom.xml as main dep |
| SparseVector | (in-repo) | Return type for `SparseEmbeddingFunction` | Already in `tech.amikos.chromadb.v2` |
| Embedding | (in-repo) | Return type for `ContentEmbeddingFunction` | Already in `tech.amikos.chromadb` |

### New Dependencies
| Library | Version | Purpose | Scope |
|---------|---------|---------|-------|
| `com.github.rholder:snowball-stemmer` | 1.3.0.581.1 | Snowball English stemmer for BM25 tokenizer | compile |
| Murmur3 (inline impl) | n/a | Hash tokens to int indices — ~50 lines, no external dep needed | n/a |
| `com.google.genai:google-genai` | 1.2.0 | Google Gemini embeddings SDK | optional |
| `software.amazon.awssdk:bedrockruntime` | 2.34.0 | AWS Bedrock embedding invocation | optional |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Inline Murmur3 | `com.google.guava:guava` (has `Hashing.murmur3_32()`) | Guava is large (2.9 MB); adding it as `compile` scope would inflate transitive deps for users who don't use BM25. Inline ~50-line Murmur3 is cleaner and explicitly matches Python `mmh3` x86 variant. |
| Inline Murmur3 | `com.github.eprst:murmur3` | Another small option but poorly maintained. Inline is simpler. |
| `com.google.genai:google-genai` | Raw OkHttp for Gemini | Go client uses first-party SDK; SDK handles auth rotation, retries, and future API changes. Optional scope avoids transitive exposure. |
| `software.amazon.awssdk:bedrockruntime` | Raw OkHttp for Bedrock | AWS uses SigV4 auth — implementing it manually is error-prone. SDK is the right choice. |

**Installation:**
```xml
<!-- BM25: Snowball stemmer -->
<dependency>
    <groupId>com.github.rholder</groupId>
    <artifactId>snowball-stemmer</artifactId>
    <version>1.3.0.581.1</version>
</dependency>

<!-- Gemini embeddings (optional: user must add to their project) -->
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>1.2.0</version>
    <optional>true</optional>
</dependency>

<!-- AWS Bedrock embeddings (optional: user must add to their project) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bedrockruntime</artifactId>
    <version>2.34.0</version>
    <optional>true</optional>
</dependency>
```

**Version notes (verified 2026-04-01):**
- `snowball-stemmer:1.3.0.581.1` — last released 2014, stable, Java 8 compatible (pure Java)
- `google-genai:1.2.0` — latest on Maven Central 2026-05-30; Java 8 compatible (confirmed `maven.compiler.source=1.8` in google-genai pom)
- `bedrockruntime:2.34.0` — latest; AWS SDK v2 requires Java 8+
- Guava is already a **test-scope transitive** dep; do NOT promote to compile scope

---

## Architecture Patterns

### Recommended Package Structure
```
src/main/java/tech/amikos/chromadb/
├── embeddings/
│   ├── EmbeddingFunction.java              (existing — unchanged)
│   ├── WithParam.java                      (existing — unchanged)
│   ├── DefaultEmbeddingFunction.java       (existing — unchanged)
│   ├── SparseEmbeddingFunction.java        (NEW interface)
│   ├── ContentEmbeddingFunction.java       (NEW interface + adapters)
│   ├── EmbeddingFunctionRegistry.java      (NEW public class)
│   ├── content/                            (NEW sub-package)
│   │   ├── Content.java
│   │   ├── Part.java
│   │   ├── BinarySource.java
│   │   ├── Modality.java
│   │   └── Intent.java
│   ├── bm25/                               (NEW)
│   │   ├── BM25EmbeddingFunction.java
│   │   ├── BM25Tokenizer.java
│   │   └── BM25StopWords.java
│   ├── chromacloudsplade/                  (NEW)
│   │   ├── ChromaCloudSpladeEmbeddingFunction.java
│   │   ├── CreateSparseEmbeddingRequest.java
│   │   └── CreateSparseEmbeddingResponse.java
│   ├── gemini/                             (NEW)
│   │   ├── GeminiEmbeddingFunction.java
│   │   └── (no request/response DTOs — uses google-genai SDK types)
│   ├── bedrock/                            (NEW)
│   │   ├── BedrockEmbeddingFunction.java
│   │   └── (uses AWS SDK types directly)
│   └── voyage/                             (NEW)
│       ├── VoyageEmbeddingFunction.java
│       ├── CreateEmbeddingRequest.java
│       └── CreateEmbeddingResponse.java
└── reranking/                              (NEW top-level sub-package)
    ├── RerankingFunction.java              (interface)
    ├── RerankResult.java                   (value type)
    ├── cohere/
    │   ├── CohereRerankingFunction.java
    │   ├── RerankRequest.java
    │   └── RerankResponse.java
    └── jina/
        ├── JinaRerankingFunction.java
        ├── RerankRequest.java
        └── RerankResponse.java
```

### Pattern 1: SparseEmbeddingFunction Interface
**What:** Parallel to `EmbeddingFunction` but returns `List<SparseVector>` (not `List<Embedding>`).
**When to use:** Any provider that produces sparse token-weighted vectors (BM25, Splade).

```java
// Source: derived from Go SparseEmbeddingFunction interface + existing EmbeddingFunction pattern
package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.v2.SparseVector;
import java.util.List;

public interface SparseEmbeddingFunction {
    SparseVector embedQuery(String query) throws EFException;
    List<SparseVector> embedDocuments(List<String> documents) throws EFException;
}
```

### Pattern 2: ContentEmbeddingFunction Interface + Adapter
**What:** Accepts `List<Content>` (multimodal) instead of `List<String>`.
**When to use:** Gemini multimodal, VoyageAI multimodal, or wrapping any dense EF.

```java
// Source: derived from Go ContentEmbeddingFunction + D-06 in CONTEXT.md
package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.Content;
import java.util.Collections;
import java.util.List;

public interface ContentEmbeddingFunction {
    List<Embedding> embedContents(List<Content> contents) throws EFException;

    default Embedding embedContent(Content content) throws EFException {
        return embedContents(Collections.singletonList(content)).get(0);
    }

    /** Convenience: wrap any text-only EmbeddingFunction as ContentEmbeddingFunction */
    static ContentEmbeddingFunction fromTextOnly(EmbeddingFunction ef) {
        return new TextEmbeddingAdapter(ef);
    }
}
```

### Pattern 3: Content Value Type (static factory + builder)
**What:** Mirrors Go's `Content` type — ordered list of `Part` objects with optional `Intent`.
**Pattern source:** `CollectionConfiguration.builder()` for complex case, `SparseVector.of()` for simple factories.

```java
// Source: Go multimodal.go + CONTEXT.md D-07
public final class Content {
    private final List<Part> parts;
    private final Intent intent;

    public static Content text(String text) { ... }   // simple case
    public static Builder builder() { ... }           // complex case

    public static final class Builder {
        public Builder part(Part part) { ... }
        public Builder intent(Intent intent) { ... }
        public Content build() { ... }
    }
}
```

### Pattern 4: BM25 Tokenizer Pipeline
**What:** Exact replica of Go client tokenizer for cross-client index compatibility.
**Source:** Verified from `chroma-go/pkg/embeddings/bm25/tokenizer.go` and `bm25.go`.

Pipeline steps (in order):
1. `text.toLowerCase(Locale.ROOT)`
2. Replace non-alphanumeric sequences with spaces: `text.replaceAll("[^a-zA-Z0-9]+", " ")`
3. `text.split("\\s+")` → token array
4. Filter tokens in `BM25StopWords.DEFAULT_STOP_WORDS` (set lookup)
5. Filter tokens where `token.length() > tokenMaxLength` (default: 100)
6. Stem each token with `EnglishStemmer` from `com.github.rholder:snowball-stemmer`
7. Hash each stemmed token to int index via Murmur3 x86 32-bit with seed 0

BM25 score formula:
```
score(term t, doc d) = tf * (K + 1) / (tf + K * (1 - B + B * docLen / avgDocLen))
```
where K=1.2, B=0.75 (BM25 standard parameters, matching Go defaults).

```java
// Murmur3 x86 32-bit — inline implementation matching Python mmh3 behavior
// Key: seed=0, unsigned int arithmetic, same output as Go github.com/spaolacci/murmur3
int tokenIndex = murmur3_32(stemmedToken.getBytes(StandardCharsets.UTF_8), 0);
```

### Pattern 5: EmbeddingFunctionRegistry
**What:** Public singleton wrapping the existing package-private `EmbeddingFunctionResolver`. Three factory maps (dense, sparse, content).

```java
// Source: Go registry.go pattern + CONTEXT.md D-16 through D-21
public final class EmbeddingFunctionRegistry {
    private static final EmbeddingFunctionRegistry DEFAULT = new EmbeddingFunctionRegistry(true);

    // Factory functional interfaces (Java 8 compatible — named interfaces, not lambdas in interface)
    public interface DenseFactory {
        EmbeddingFunction create(Map<String, Object> config) throws EFException;
    }
    public interface SparseFactory {
        SparseEmbeddingFunction create(Map<String, Object> config) throws EFException;
    }
    public interface ContentFactory {
        ContentEmbeddingFunction create(Map<String, Object> config) throws EFException;
    }

    public static EmbeddingFunctionRegistry getDefault() { return DEFAULT; }

    public synchronized void registerDense(String name, DenseFactory factory) { ... }
    public synchronized void registerSparse(String name, SparseFactory factory) { ... }
    public synchronized void registerContent(String name, ContentFactory factory) { ... }

    // Resolve from spec (replaces EmbeddingFunctionResolver.resolve())
    public EmbeddingFunction resolveDense(EmbeddingFunctionSpec spec) { ... }
    public SparseEmbeddingFunction resolveSparse(EmbeddingFunctionSpec spec) { ... }
    // Content fallback: content factory → dense factory + adapter
    public ContentEmbeddingFunction resolveContent(EmbeddingFunctionSpec spec) { ... }
}
```

### Pattern 6: RerankingFunction Interface
**What:** Simple interface matching Go's `rerankings.RerankingFunction` contract, adapted for Java (no context.Context needed for sync API).

```java
// Source: Go pkg/rerankings/reranking.go + CONTEXT.md D-22
package tech.amikos.chromadb.reranking;

import tech.amikos.chromadb.EFException;
import java.util.List;

public interface RerankingFunction {
    List<RerankResult> rerank(String query, List<String> documents) throws EFException;
}

public final class RerankResult {
    private final int index;      // position in original documents list
    private final double score;   // relevance score (0.0-1.0)
    // static factory + getters
    public static RerankResult of(int index, double score) { ... }
}
```

### Anti-Patterns to Avoid
- **Extending `EmbeddingFunction` for sparse:** `SparseEmbeddingFunction` must be a separate interface (D-01). Implementing both on one class is fine; inheritance is not.
- **Adding `MultimodalEmbeddingFunction`:** Explicitly skipped (D-05). Do not create it.
- **Making `EmbeddingFunctionRegistry` replace `EmbeddingFunctionResolver` directly:** The resolver stays as package-private internal; the registry wraps and delegates to it for backward compatibility with existing `ChromaHttpCollection` code.
- **Using `Optional` return types:** Existing codebase returns null for absent values (see `EmbeddingFunctionResolver.resolve(null) → null`). Match that pattern.
- **Using records or sealed classes:** Java 8 target. Use final classes with private constructors and static factories.
- **Registering vendor SDK classes at class load time (static initializer in registry):** Pre-registration of built-in providers happens in the `EmbeddingFunctionRegistry` constructor, guarded by a flag, not in static initializers which cause NoClassDefFoundError when optional SDK jars are absent.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| English stemming for BM25 | Custom stemmer | `com.github.rholder:snowball-stemmer` (Snowball English) | Stemmer correctness is hard; must match Go client's Snowball output exactly |
| AWS SigV4 auth for Bedrock | Custom request signing | `software.amazon.awssdk:bedrockruntime` | SigV4 signing involves HMAC-SHA256 with date/region/service scope; one mistake breaks all requests silently |
| Google Cloud auth for Gemini | Custom OAuth2 | `com.google.genai:google-genai` | SDK handles API key + Application Default Credentials, retries, and model deprecation |
| Murmur3 hashing | Different hash (MD5, SHA-1) | Inline Murmur3 x86 32-bit (see code example) | BM25 indices must match Go/Python for cross-client compatibility; wrong variant = silently incompatible sparse vectors |
| BM25 document frequency / IDF tables | Separate indexer | Stateless per-document BM25 (no corpus IDF) | Go client uses per-document BM25 (TF-only with length norm) — no corpus needed |

**Key insight:** For BM25, the Go client does NOT maintain an IDF table across a document corpus. It computes a per-document BM25 score based on term frequency and document length only. This makes the implementation stateless — no corpus ingestion step required. This is the critical design decision to match.

---

## Common Pitfalls

### Pitfall 1: Murmur3 Variant Mismatch
**What goes wrong:** Using Murmur3 128-bit instead of x86 32-bit produces completely different index values. Sparse vectors from Java and Python/Go clients would not share the same vocabulary space, silently breaking hybrid search.
**Why it happens:** Go uses `github.com/spaolacci/murmur3` (x86 32-bit, seed 0). Python uses `mmh3` (same variant). Many Java Murmur3 libraries default to 128-bit or use different seeds.
**How to avoid:** Inline the Murmur3 x86 32-bit algorithm explicitly. Verify with known test vectors (e.g., `murmur3_32("hello", 0) == 613716550`).
**Warning signs:** BM25 integration tests fail when comparing Java and Python client outputs for the same text.

### Pitfall 2: Guava Promoted to Compile Scope
**What goes wrong:** Adding `com.google.guava:guava` as a `compile` dependency to use `Hashing.murmur3_32()` pulls in a 2.9 MB jar and 6 transitive deps for every user, even those not using BM25.
**Why it happens:** Guava is already present as a test-scope transitive dep (via WireMock), making it tempting to use it in main code.
**How to avoid:** Use inline Murmur3 implementation (~50 lines). The existing `mvn dependency:tree` shows guava only in test scope — keep it there.
**Warning signs:** `mvn dependency:tree` shows `com.google.guava:guava:jar:33.x-jre:compile`.

### Pitfall 3: Optional SDK Registration Triggers NoClassDefFoundError
**What goes wrong:** If `EmbeddingFunctionRegistry`'s static initializer tries to load `GeminiEmbeddingFunction` and the `google-genai` jar is absent (user didn't add it), Java throws `NoClassDefFoundError` at class load time for the registry — crashing all users including those not using Gemini.
**Why it happens:** Static initializers run unconditionally at class load. If any referenced class is missing, the whole initializer fails.
**How to avoid:** Pre-register vendor providers lazily or in a `try-catch (NoClassDefFoundError)` block. Alternatively, register them using reflection-based class existence checks: `Class.forName("com.google.genai.Client", false, ...)`.
**Warning signs:** `java.lang.NoClassDefFoundError: com/google/genai/Client` when running any embedding operation with Gemini SDK absent.

### Pitfall 4: BM25 Not Matching Go Stop Words
**What goes wrong:** Sparse vectors produced by Java BM25 are incompatible with Go/Python because different stop words were filtered out.
**Why it happens:** Different sources have different default stop word lists.
**How to avoid:** Use exactly the 174-word list from Go client (`chroma-go/pkg/embeddings/bm25/stopwords.go`). This is the `DEFAULT_CHROMA_BM25_STOPWORDS` list. The list is well-known (NLTK English stop words base).
**Warning signs:** Cross-language integration test fails — same document produces different non-zero indices in Java vs Go.

### Pitfall 5: Registry Wrapping Does Not Update ChromaHttpCollection
**What goes wrong:** `EmbeddingFunctionResolver.resolve()` is still called directly from `ChromaHttpCollection` after introducing the registry, so registry-registered custom providers are never used during auto-wiring.
**Why it happens:** The existing code calls the package-private resolver directly. The new registry must replace or delegate from the collection's auto-wiring path.
**How to avoid:** Update the single call site in `ChromaHttpCollection` (or wherever the resolver is invoked) to use `EmbeddingFunctionRegistry.getDefault().resolveDense(spec)` instead.
**Warning signs:** Custom providers registered with the registry are never instantiated when creating collections from config.

### Pitfall 6: Cohere Rerank v1 vs v2 Endpoint
**What goes wrong:** Sending requests to `https://api.cohere.ai/v1/rerank` (old endpoint) instead of `https://api.cohere.com/v2/rerank` (current endpoint) gets a deprecation warning or eventual 404.
**Why it happens:** Existing `CohereEmbeddingFunction` uses the v1 base URL. A copy-paste error could carry this forward.
**How to avoid:** Use `https://api.cohere.com/v2/rerank` as the default base URL for `CohereRerankingFunction`. The v2 model is `rerank-v4.0-pro` (or `rerank-english-v3.0` for backwards compat).
**Warning signs:** HTTP 301 redirect responses, or deprecation headers in the response.

---

## Code Examples

### BM25 Scoring Core
```java
// Source: derived from chroma-go/pkg/embeddings/bm25/bm25.go
// Per-document BM25 (no corpus IDF — stateless)
private SparseVector embedSingle(String text) throws EFException {
    List<String> tokens = tokenizer.tokenize(text);
    int docLen = tokens.size();

    // Count term frequencies
    Map<String, Integer> tf = new LinkedHashMap<String, Integer>();
    for (String token : tokens) {
        Integer count = tf.get(token);
        tf.put(token, count == null ? 1 : count + 1);
    }

    // Compute BM25 scores and hash to indices
    // K=1.2, B=0.75, avgDocLen set at construction time (default: 256)
    Map<Integer, Float> indexScores = new LinkedHashMap<Integer, Float>();
    for (Map.Entry<String, Integer> entry : tf.entrySet()) {
        float tfVal = entry.getValue();
        float score = (tfVal * (K + 1)) / (tfVal + K * (1 - B + B * docLen / avgDocLen));
        int idx = Murmur3.hash32(entry.getKey().getBytes(StandardCharsets.UTF_8), 0);
        Float existing = indexScores.get(idx);
        indexScores.put(idx, existing == null ? score : existing + score); // collision: sum scores
    }

    // Sort by index, build arrays
    List<Integer> sortedIndices = new ArrayList<Integer>(indexScores.keySet());
    Collections.sort(sortedIndices);
    int[] indices = new int[sortedIndices.size()];
    float[] values = new float[sortedIndices.size()];
    for (int i = 0; i < sortedIndices.size(); i++) {
        indices[i] = sortedIndices.get(i);
        values[i] = indexScores.get(sortedIndices.get(i));
    }
    return SparseVector.of(indices, values);
}
```

### Voyage Embeddings (OkHttp)
```java
// Source: docs.voyageai.com/reference/embeddings-api (verified 2026-04-01)
// POST https://api.voyageai.com/v1/embeddings
// Auth: Authorization: Bearer $VOYAGE_API_KEY
// Body: { "input": [...], "model": "voyage-3.5", "input_type": "document" }
// Response: { "data": [{"embedding": [...], "index": 0}], "usage": {...} }

Request request = new Request.Builder()
    .url(baseAPI)
    .post(RequestBody.create(gson.toJson(reqBody), JSON))
    .addHeader("Authorization", "Bearer " + apiKey)
    .addHeader("Content-Type", "application/json")
    .build();
```

### Cohere Rerank v2 (OkHttp)
```java
// Source: docs.cohere.com/reference/rerank (verified 2026-04-01)
// POST https://api.cohere.com/v2/rerank
// Auth: Authorization: Bearer $COHERE_API_KEY
// Body: { "model": "rerank-v4.0-pro", "query": "...", "documents": [...] }
// Response: { "results": [{"index": 0, "relevance_score": 0.95}] }

Request request = new Request.Builder()
    .url("https://api.cohere.com/v2/rerank")
    .post(RequestBody.create(gson.toJson(reqBody), JSON))
    .addHeader("Authorization", "Bearer " + apiKey)
    .addHeader("Content-Type", "application/json")
    .build();
```

### Jina Reranker (OkHttp)
```java
// Source: jina.ai/reranker + verified via web search (2026-04-01)
// POST https://api.jina.ai/v1/rerank
// Auth: Authorization: Bearer $JINA_API_KEY
// Body: { "model": "jina-reranker-v2-base-multilingual", "query": "...", "documents": [...] }
// Response: { "results": [{"index": 0, "relevance_score": 0.9}] }
```

### Gemini Embeddings (Google GenAI SDK)
```java
// Source: google-genai SDK (com.google.genai:google-genai:1.2.0, Java 8 compatible)
// Provider name: "google_genai" (matches Go client)
// Default model: "gemini-embedding-2-preview" (matches Go client constant)
// Env var: GEMINI_API_KEY

// SDK usage (Java 8 style — no var keyword):
Client genaiClient = Client.builder().apiKey(apiKey).build();
EmbedContentResponse response = genaiClient.models().embedContent(
    modelName,
    Content.fromParts(Part.fromText(text)),
    null
);
float[] vector = toFloatArray(response.embedding().values());
```

### AWS Bedrock Embeddings (bedrockruntime SDK)
```java
// Source: AWS SDK v2 bedrockruntime (software.amazon.awssdk:bedrockruntime:2.34.0)
// Provider name: "amazon_bedrock" (matches Go client)
// Default model: "amazon.titan-embed-text-v1" (matches Go client default)
// Auth: AWS SDK credential chain (env vars, ~/.aws/credentials, EC2 role, etc.)

BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
    .region(Region.of(region))
    .build();

String body = "{\"inputText\":\"" + text + "\"}";
InvokeModelResponse response = bedrockClient.invokeModel(InvokeModelRequest.builder()
    .modelId(modelId)
    .body(SdkBytes.fromUtf8String(body))
    .build());
// Parse JSON response: response.body().asUtf8String() → { "embedding": [...] }
```

### Modality Enum (follows DistanceFunction pattern)
```java
// Source: existing DistanceFunction.java pattern in this codebase
public enum Modality {
    TEXT("text"),
    IMAGE("image"),
    AUDIO("audio"),
    VIDEO("video"),
    PDF("pdf");

    private final String value;
    Modality(String value) { this.value = value; }
    public String getValue() { return value; }

    public static Modality fromValue(String value) {
        if (value == null) throw new IllegalArgumentException("value must not be null");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Modality m : values()) {
            if (m.value.equals(normalized)) return m;
        }
        throw new IllegalArgumentException("Unknown modality: " + value);
    }
}
```

---

## BM25 Stop Words (verified from Go client)

174 words matching Go `DEFAULT_CHROMA_BM25_STOPWORDS` (NLTK English base):

```
a, about, above, after, again, against, ain, all, am, an, and, any, are, aren,
aren't, as, at, be, because, been, before, being, below, between, both, but, by,
can, couldn, couldn't, d, did, didn, didn't, do, does, doesn, doesn't, doing, don,
don't, down, during, each, few, for, from, further, had, hadn, hadn't, has, hasn,
hasn't, have, haven, haven't, having, he, her, here, hers, herself, him, himself,
his, how, i, if, in, into, is, isn, isn't, it, it's, its, itself, just, ll, m,
ma, me, mightn, mightn't, more, most, mustn, mustn't, my, myself, needn, needn't,
no, nor, not, now, o, of, off, on, once, only, or, other, our, ours, ourselves,
out, over, own, re, s, same, shan, shan't, she, she's, should, should've, shouldn,
shouldn't, so, some, such, t, than, that, that'll, the, their, theirs, them,
themselves, then, there, these, they, this, those, through, to, too, under, until,
up, ve, very, was, wasn, wasn't, we, were, weren, weren't, what, when, where,
which, while, who, whom, why, will, with, won, won't, wouldn, wouldn't, y, you,
you'd, you'll, you're, you've, your, yours, yourself, yourselves
```

---

## API Wire Formats Reference

### Chroma Cloud Splade
- **Endpoint:** `POST https://embed.trychroma.com/embed_sparse`
- **Auth:** `x-chroma-token: <token>` header
- **Response:** `{ "indices": [...], "values": [...] }` per document

### Voyage AI Embeddings
- **Endpoint:** `POST https://api.voyageai.com/v1/embeddings`
- **Auth:** `Authorization: Bearer $VOYAGE_API_KEY`
- **Request:** `{ "input": ["..."], "model": "voyage-3.5", "input_type": "document" }`
- **Response:** `{ "data": [{"embedding": [...], "index": 0}], "usage": {"total_tokens": N} }`
- **Default model (Java):** Use `"voyage-2"` (Go client default) for consistency; optionally expose `"voyage-3.5"` as updated default

### Cohere Rerank v2
- **Endpoint:** `POST https://api.cohere.com/v2/rerank`
- **Auth:** `Authorization: Bearer $COHERE_API_KEY`
- **Request:** `{ "model": "rerank-english-v3.0", "query": "...", "documents": ["..."] }`
- **Response:** `{ "results": [{"index": 0, "relevance_score": 0.95}] }`

### Jina Reranker
- **Endpoint:** `POST https://api.jina.ai/v1/rerank`
- **Auth:** `Authorization: Bearer $JINA_API_KEY`
- **Request:** `{ "model": "jina-reranker-v2-base-multilingual", "query": "...", "documents": ["..."] }`
- **Response:** `{ "results": [{"index": 0, "relevance_score": 0.9}] }`

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Cohere rerank v1 API (`api.cohere.ai/v1/rerank`) | v2 API (`api.cohere.com/v2/rerank`) | 2024 | Use new endpoint and model names (`rerank-v4.0-pro`) |
| Gemini embedding SDK via `com.google.ai.client.generativeai` | `com.google.genai:google-genai` (new GA SDK) | May 2025 | New artifact ID; old SDK is deprecated |
| AWS SDK v1 (`com.amazonaws:aws-java-sdk-bedrock`) | AWS SDK v2 (`software.amazon.awssdk:bedrockruntime`) | SDK v1 end-of-life 2025-12-31 | Use v2 only |

**Deprecated/outdated:**
- `com.google.ai.client.generativeai` (old Gemini SDK): replaced by `com.google.genai:google-genai`
- `com.amazonaws:aws-java-sdk-bedrock` (AWS SDK v1): end-of-life December 2025; use `software.amazon.awssdk:bedrockruntime`
- Cohere v1 API base URL `api.cohere.ai`: moved to `api.cohere.com` for v2

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 8+ | All | ✓ | Enforced by pom.xml `animal-sniffer` | — |
| Maven | Build | ✓ | (project standard) | — |
| OkHttp 4.12.0 | Voyage, Splade, Cohere Rerank, Jina | ✓ | Already in pom.xml | — |
| Gson 2.10.1 | All provider DTOs | ✓ | Already in pom.xml | — |
| WireMock (test) | Unit tests for new providers | ✓ | Already in test scope | — |
| `GEMINI_API_KEY` env var | Gemini integration tests | ✗ (likely) | — | Skip integration test with `Assume.assumeNotNull` |
| `VOYAGE_API_KEY` env var | Voyage integration tests | ✗ (likely) | — | Skip integration test with `Assume.assumeNotNull` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Bedrock integration tests | ✗ (likely) | — | Skip integration test with `Assume.assumeNotNull` |
| `COHERE_API_KEY` env var | Cohere Rerank integration tests | ✗ (likely) | — | Skip integration test with `Assume.assumeNotNull` |
| `JINA_API_KEY` env var | Jina Reranker integration tests | ✗ (likely) | — | Skip integration test with `Assume.assumeNotNull` |
| Google GenAI SDK (`com.google.genai:google-genai:1.2.0`) | Gemini provider compile | ✗ (not in pom) | — | Must add as `<optional>true</optional>` dep |
| AWS Bedrock SDK (`software.amazon.awssdk:bedrockruntime:2.34.0`) | Bedrock provider compile | ✗ (not in pom) | — | Must add as `<optional>true</optional>` dep |
| Snowball stemmer (`com.github.rholder:snowball-stemmer:1.3.0.581.1`) | BM25 tokenizer | ✗ (not in pom) | — | Must add as `compile` dep |

**Missing dependencies with no fallback:**
- `com.github.rholder:snowball-stemmer` — blocking for BM25; must be added to pom.xml as compile dep
- `com.google.genai:google-genai` — blocking for Gemini provider compilation; must be added as optional
- `software.amazon.awssdk:bedrockruntime` — blocking for Bedrock provider compilation; must be added as optional

**Missing dependencies with fallback:**
- All API key env vars — integration tests gated with `Assume.assumeNotNull(System.getenv("KEY_NAME"))` per existing conformance test pattern

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 |
| Config file | none (uses maven-surefire-plugin patterns in pom.xml) |
| Quick run command | `mvn test -Dtest=EmbeddingFunctionRegistryTest,BM25EmbeddingFunctionTest,SparseEmbeddingFunctionTest,ContentEmbeddingFunctionTest,RerankingFunctionTest` |
| Full suite command | `mvn test` |
| Integration tests | `mvn test -Pintegration -Dtest=GeminiConformanceIntegrationTest,VoyageConformanceIntegrationTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EMB-05 | BM25 tokenizes and produces SparseVector with correct indices/values | unit | `mvn test -Dtest=BM25EmbeddingFunctionTest` | ❌ Wave 0 |
| EMB-05 | BM25 stop words match Go client list | unit | `mvn test -Dtest=BM25TokenizerTest` | ❌ Wave 0 |
| EMB-05 | BM25 Murmur3 hashing matches cross-client test vectors | unit | `mvn test -Dtest=Murmur3Test` | ❌ Wave 0 |
| EMB-05 | SparseEmbeddingFunction interface: null/empty rejection | unit | `mvn test -Dtest=SparseEmbeddingConformanceTest` | ❌ Wave 0 |
| EMB-05 | Chroma Cloud Splade provider (WireMock) | unit | `mvn test -Dtest=ChromaCloudSpladeEmbeddingFunctionTest` | ❌ Wave 0 |
| EMB-06 | Content/Part/BinarySource value types: construction, equality | unit | `mvn test -Dtest=ContentTypesTest` | ❌ Wave 0 |
| EMB-06 | ContentEmbeddingFunction adapter wraps EmbeddingFunction correctly | unit | `mvn test -Dtest=ContentEmbeddingAdapterTest` | ❌ Wave 0 |
| EMB-07 | Voyage provider: null rejection, error wrapping, success (WireMock) | unit | `mvn test -Dtest=VoyageConformanceTest` | ❌ Wave 0 |
| EMB-07 | Gemini provider: compiles and constructs with optional SDK absent → clear error | unit | `mvn test -Dtest=GeminiEmbeddingFunctionTest` | ❌ Wave 0 |
| EMB-07 | Bedrock provider: compiles and constructs with optional SDK absent → clear error | unit | `mvn test -Dtest=BedrockEmbeddingFunctionTest` | ❌ Wave 0 |
| EMB-08 | Registry: register dense/sparse/content, resolve by name | unit | `mvn test -Dtest=EmbeddingFunctionRegistryTest` | ❌ Wave 0 |
| EMB-08 | Registry: content fallback chain (dense → adapter when no content factory) | unit | `mvn test -Dtest=EmbeddingFunctionRegistryTest#testContentFallbackChain` | ❌ Wave 0 |
| EMB-08 | Registry singleton pre-registers all built-in providers | unit | `mvn test -Dtest=EmbeddingFunctionRegistryTest#testDefaultRegistryHasBuiltins` | ❌ Wave 0 |
| EMB-08 | Registry: duplicate registration rejected | unit | `mvn test -Dtest=EmbeddingFunctionRegistryTest#testDuplicateRegistrationRejected` | ❌ Wave 0 |
| RERANK-01 | RerankingFunction interface + RerankResult value type | unit | `mvn test -Dtest=RerankResultTest` | ❌ Wave 0 |
| RERANK-01 | Cohere Rerank provider: success and error cases (WireMock) | unit | `mvn test -Dtest=CohereRerankingFunctionTest` | ❌ Wave 0 |
| RERANK-01 | Jina Reranker provider: success and error cases (WireMock) | unit | `mvn test -Dtest=JinaRerankingFunctionTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** Quick test run targeting the files changed in that task
- **Per wave merge:** `mvn test` (all unit tests)
- **Phase gate:** `mvn test` full suite green before `/gsd:verify-work`

### Wave 0 Gaps
All test files listed above are new and must be created in Wave 0 (or alongside implementation):
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/BM25EmbeddingFunctionTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/BM25TokenizerTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/Murmur3Test.java` (cross-client test vectors)
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/SparseEmbeddingConformanceTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/chromacloudsplade/ChromaCloudSpladeEmbeddingFunctionTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/content/ContentTypesTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/ContentEmbeddingAdapterTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/voyage/VoyageConformanceTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/gemini/GeminiEmbeddingFunctionTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/bedrock/BedrockEmbeddingFunctionTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/EmbeddingFunctionRegistryTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/reranking/RerankResultTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/reranking/cohere/CohereRerankingFunctionTest.java`
- [ ] `src/test/java/tech/amikos/chromadb/reranking/jina/JinaRerankingFunctionTest.java`

*(All WireMock conformance tests follow the existing `AbstractEmbeddingFunctionConformanceTest` pattern)*

---

## Open Questions

1. **Voyage default model name**
   - What we know: Go client uses `"voyage-2"` as default. Voyage AI has since released `voyage-3.5` (and `voyage-4-large`).
   - What's unclear: Whether to use Go-compatible default (`"voyage-2"`) or updated default (`"voyage-3.5"`).
   - Recommendation: Use `"voyage-2"` as the default for Go client compatibility (matches `WithParam.defaultModel()`). Document the newer models in Javadoc. Users can override with `WithParam.model("voyage-3.5")`.

2. **EmbeddingFunctionRegistry thread safety model**
   - What we know: D-20 says "thread-safe with synchronized access". Go uses `sync.RWMutex` (multiple readers, single writer).
   - What's unclear: Whether to use `synchronized` methods (simple) or `java.util.concurrent.ConcurrentHashMap` (better read concurrency).
   - Recommendation: Use `synchronized` on register methods + `Collections.unmodifiableMap` snapshot for reads. The registry is write-once-at-startup in practice; high read concurrency is not a real concern.

3. **Gemini model name change**
   - What we know: Go client uses `"gemini-embedding-2-preview"` as default. The model ID naming convention for Google changes frequently.
   - What's unclear: Whether `"gemini-embedding-2-preview"` is still valid in the `google-genai` 1.2.0 SDK as of 2026.
   - Recommendation: Use the same default as the Go client (`"gemini-embedding-2-preview"`) for now. If Gemini integration test fails, the error will clarify the current model name. Low risk since the model param is user-overridable.

---

## Sources

### Primary (HIGH confidence)
- Codebase: `src/main/java/tech/amikos/chromadb/embeddings/` — existing EF patterns, WithParam, OkHttp usage
- Codebase: `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionResolver.java` — existing auto-wiring logic
- Codebase: `src/main/java/tech/amikos/chromadb/v2/SparseVector.java` — reuse as return type
- Codebase: `src/main/java/tech/amikos/chromadb/v2/DistanceFunction.java` — enum pattern for Modality/Intent
- GitHub raw: `chroma-go/pkg/embeddings/bm25/tokenizer.go` — tokenizer pipeline (lowercase, regex, stopword filter, stemmer)
- GitHub raw: `chroma-go/pkg/embeddings/bm25/bm25.go` — BM25 scoring, Murmur3 hashing, SparseVector output
- GitHub raw: `chroma-go/pkg/embeddings/bm25/stopwords.go` — 174 English stop words
- GitHub raw: `chroma-go/pkg/embeddings/registry.go` — registry pattern (4 maps, singleton, content fallback chain)
- GitHub raw: `chroma-go/pkg/rerankings/reranking.go` — RerankingFunction interface, RankedResult type
- GitHub raw: `chroma-go/pkg/rerankings/cohere/cohere.go` — Cohere rerank implementation
- GitHub raw: `chroma-go/pkg/rerankings/jina/jina.go` — Jina reranker implementation
- Official docs: `docs.voyageai.com/reference/embeddings-api` — endpoint, request/response format, auth
- Official docs: `docs.cohere.com/reference/rerank` — v2 endpoint, request/response format, auth
- Maven Central: `com.google.genai:google-genai:1.2.0` — verified 2026-04-01
- Maven Central: `software.amazon.awssdk:bedrockruntime:2.34.0` — verified 2026-04-01
- Maven Central: `com.github.rholder:snowball-stemmer:1.3.0.581.1` — verified 2026-04-01
- `googleapis/java-genai` pom.xml — Java 8 compatibility confirmed (`maven.compiler.source=1.8`)

### Secondary (MEDIUM confidence)
- WebSearch verified: AWS SDK v2 requires Java 8+ (multiple official AWS docs confirm)
- WebSearch verified: Jina reranker endpoint `https://api.jina.ai/v1/rerank`, Bearer auth (jina.ai/reranker)
- Go client `gemini.go` Name() method: `"google_genai"`, default model `"gemini-embedding-2-preview"`
- Go client `voyage.go` Name() method: `"voyageai"`, env var: `"VOYAGE_API_KEY"`, default: `"voyage-2"`
- Go client `bedrock.go` Name() method: `"amazon_bedrock"`, default model: `"amazon.titan-embed-text-v1"`
- Go client `chromacloudsplade.go` Name() method: `"chroma-cloud-splade"`, endpoint: `"https://embed.trychroma.com/embed_sparse"`

### Tertiary (LOW confidence)
- None — all critical claims verified via primary or secondary sources.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions verified against Maven Central 2026-04-01
- Architecture patterns: HIGH — derived from existing codebase patterns + Go reference client
- BM25 pipeline: HIGH — verified directly from Go source files
- API wire formats: HIGH — verified from official provider documentation
- Vendor SDK Java 8 compat: HIGH — confirmed via pom.xml inspection (google-genai) and official docs (AWS SDK v2)
- Pitfalls: HIGH — derived from concrete codebase analysis and known library behaviors

**Research date:** 2026-04-01
**Valid until:** 2026-07-01 (stable domain; API endpoints and SDK versions may drift after 90 days)
