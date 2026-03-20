# Phase 03: Embeddings & ID Extensibility - Research

**Researched:** 2026-03-19
**Domain:** Embedding provider normalization, embedding resolution precedence, DefaultEmbeddingFunction reliability, ID generator validation
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Provider error normalization**
- All provider errors must be wrapped in `ChromaException` at the v2 boundary with provider name + model in the message (e.g., "OpenAI embedding failed (model: text-embedding-3-small): 429 rate limit exceeded").
- Providers must validate inputs eagerly: reject null/empty text lists before calling remote APIs. Consistent with Phase 1 fail-fast pattern.
- Partial results (returned embedding count != input count) must fail fast with a count-mismatch `ChromaException`.
- A shared conformance test suite (abstract base class) must assert consistent behavior across all providers: null rejection, empty-list rejection, count-match, ChromaException wrapping.
- Conformance tests run with mocked HTTP responses — no real API keys needed. Real-key tests stay in integration suite.

**Embedding precedence rules**
- Precedence order is locked: runtime/explicit EF > `configuration.embedding_function` > `schema.default_embedding_function`. This matches the Go client (`amikos-tech/chroma-go`) pattern.
- Explicit EF always wins — no DefaultEF fall-through special-casing (unlike Python client).
- When explicit EF name differs from persisted config EF name, log a warning (not error). Users stay in control.
- Precedence must be documented in Javadoc on Collection interface and `ChromaHttpCollection.requireEmbeddingFunction()`, and locked with contract tests.
- Auto-wired EF resolution logged at DEBUG level (e.g., "Auto-wired embedding function: openai from collection configuration").
- Unsupported EF descriptors do not block collection construction — lazy fail on embed only. Collection remains usable for operations that don't need embedding (get by ID, delete).

**Default embedding reliability**
- `DefaultEmbeddingFunction` must have a configurable timeout for ONNX model download. If timeout is hit, throw `ChromaException` with actionable message.
- Must be thread-safe for concurrent embedding calls. Model initialization should be lazy and synchronized.
- Download failures classified as retryable (timeout, 5xx, connection reset) vs non-retryable (404, 403, corrupt file). Retry once on retryable failures, fail fast on non-retryable.
- No static warmup method — lazy download only. Document first-use download behavior in Javadoc and README.

**ID generator validation edges**
- Duplicate IDs within a single add/upsert batch must fail fast with `ChromaException` listing the duplicate IDs, before sending to server.
- Custom `IdGenerator` output (including lambdas) must be validated at the boundary: check for null/blank after `generate()` call. Throw `ChromaException` with record index on failure.
- `Sha256IdGenerator`: support metadata fallback when document is null. Hash serialized metadata for content-addressable dedup on embeddings-only records.
- `Sha256IdGenerator`: throw `IllegalArgumentException` when both document AND metadata are null ("requires a non-null document or metadata").

### Claude's Discretion
- Exact metadata serialization format for SHA-256 fallback hashing (as long as it's deterministic and documented).
- Exact conformance test structure (abstract class vs parameterized tests) as long as all providers are covered.
- Mocking approach for provider conformance tests (WireMock vs simple stubs) based on existing test infrastructure.
- Exact timeout default value for ONNX model download.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within Phase 3 scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| EMB-01 | User can use OpenAI, Cohere, HuggingFace, and Ollama embedding functions through one consistent embedding contract. | Provider conformance normalization pattern: shared abstract test, eager null/empty validation, count-mismatch guard, ChromaException wrapping with provider+model in message. |
| EMB-02 | User can use the default local embedding function without external model API keys. | DefaultEmbeddingFunction download reliability: configurable timeout, retry classification, thread-safe lazy init, actionable exception messages. |
| EMB-03 | User can provide a custom embedding function, and runtime/descriptor precedence is deterministic and documented. | Precedence chain lock: explicit > config > schema. Javadoc on Collection + requireEmbeddingFunction(). Warning log on EF name mismatch. Contract tests. |
| EMB-04 | User can generate deterministic or random IDs (UUID, ULID, SHA-256) for add/upsert flows with client-side validation. | Duplicate-ID detection in generateIds(), null/blank guard at generation site, Sha256IdGenerator metadata fallback, both-null guard. |
</phase_requirements>

---

## Summary

Phase 3 is a hardening phase: all core infrastructure already exists. The three ID generators (`UuidIdGenerator`, `UlidIdGenerator`, `Sha256IdGenerator`) are implemented. Five embedding providers exist. `EmbeddingFunctionResolver` wraps initialization errors. `ChromaHttpCollection.requireEmbeddingFunction()` implements the precedence chain. The work is to close the gaps: normalize provider input validation and error messages across all four remote providers; document and contract-test the precedence chain; harden `DefaultEmbeddingFunction` with a configurable download timeout and retry logic; and extend `Sha256IdGenerator` with metadata fallback.

The codebase uses JUnit 4, WireMock (`wiremock-jre8` 2.35.2), and TestContainers. No Mockito. The conformance test suite for providers should use WireMock stub servers (the same pattern already in `ChromaHttpCollectionTest`, `ChromaApiClientTest`, and `ChromaClientImplTest`). Abstract base class with `@Rule WireMockRule` is the correct structure — providers use OkHttp clients, so WireMock intercepts real HTTP requests.

The biggest implementation risk is `DefaultEmbeddingFunction`: the current download path uses `URL.openStream()` with no timeout, no retry, no thread safety on initialization, and throws `EFException` (old hierarchy) rather than `ChromaException`. This must be refactored carefully to avoid breaking the existing `TestDefaultEmbeddings` ground-truth test. Thread safety on model init must use a double-checked locking pattern with `volatile` since the class is Java 8 compatible.

**Primary recommendation:** Implement in three coordinated plans: (1) provider normalization + conformance tests, (2) precedence documentation + contract tests, (3) ID generator edges + Sha256 metadata fallback. Keep the three plans independent enough to execute sequentially without circular dependencies.

---

## Standard Stack

### Core (already in pom.xml — no new dependencies needed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 | 4.13.2 | Unit and contract testing | Project-standard; all existing tests use it |
| WireMock (`wiremock-jre8`) | 2.35.2 | HTTP stub server for provider conformance tests | Already used in `ChromaHttpCollectionTest`, `ChromaApiClientTest`, `ChromaClientImplTest` |
| OkHttp | Existing (via main deps) | HTTP client used by all four remote providers | Already used by providers |
| java.security.MessageDigest | JDK built-in | SHA-256 hashing in Sha256IdGenerator | Already used; no external dep needed |
| java.util.logging | JDK built-in | DEBUG-level EF resolution logging | Already used in `ChromaHttpCollection` (LOG field) |

### No New Dependencies
All required capabilities are already available:
- WireMock for HTTP stubbing (provider conformance tests)
- `java.util.concurrent` for thread-safe download initialization (double-checked locking)
- `java.net.HttpURLConnection` or OkHttp timeout configuration for ONNX download
- `java.util.TreeMap`/`java.util.LinkedHashMap` for deterministic metadata serialization in SHA-256 fallback

---

## Architecture Patterns

### Recommended Project Structure (no changes to directory structure)

Existing package layout is correct:
```
src/main/java/tech/amikos/chromadb/
├── embeddings/
│   ├── EmbeddingFunction.java        (interface — no changes needed)
│   ├── DefaultEmbeddingFunction.java (MODIFY: timeout + retry + thread safety)
│   ├── openai/OpenAIEmbeddingFunction.java   (MODIFY: null/empty guard + error message)
│   ├── cohere/CohereEmbeddingFunction.java   (MODIFY: null/empty guard + error message)
│   ├── hf/HuggingFaceEmbeddingFunction.java  (MODIFY: null/empty guard + error message)
│   └── ollama/OllamaEmbeddingFunction.java   (MODIFY: null/empty guard + error message)
├── v2/
│   ├── Collection.java                        (MODIFY: add Javadoc on EF precedence)
│   ├── ChromaHttpCollection.java              (MODIFY: Javadoc + warn on EF name mismatch + log auto-wire)
│   ├── EmbeddingFunctionResolver.java         (MODIFY: include model name in error messages)
│   ├── Sha256IdGenerator.java                 (MODIFY: metadata fallback + both-null guard)
│   └── IdGenerator.java                       (no changes needed)

src/test/java/tech/amikos/chromadb/
├── embeddings/
│   ├── AbstractEmbeddingFunctionConformanceTest.java  (NEW: abstract conformance base)
│   ├── OpenAIEmbeddingFunctionConformanceTest.java    (NEW: extends abstract, WireMock)
│   ├── CohereEmbeddingFunctionConformanceTest.java    (NEW: extends abstract, WireMock)
│   ├── HuggingFaceEmbeddingFunctionConformanceTest.java (NEW: extends abstract, WireMock)
│   └── OllamaEmbeddingFunctionConformanceTest.java    (NEW: extends abstract, WireMock)
├── v2/
│   ├── EmbeddingPrecedenceTest.java                   (NEW: contract tests for EF precedence)
│   └── IdGeneratorTest.java                           (MODIFY: add metadata fallback + both-null cases)
```

### Pattern 1: Provider Conformance Abstract Base (WireMock)

**What:** An abstract JUnit 4 test class that defines conformance behavior all providers must satisfy. Each provider gets a concrete subclass that wires up a WireMock stub for that provider's HTTP endpoint.

**When to use:** Whenever behavior must be identical across all remote embedding providers.

**Example structure:**
```java
// src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java
public abstract class AbstractEmbeddingFunctionConformanceTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    // Subclass creates the EF pointed at the WireMock port
    protected abstract EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException;

    // Subclass stubs a successful response for N inputs returning N embeddings
    protected abstract void stubSuccess(int inputCount);

    // Subclass stubs a failed response (e.g., 429)
    protected abstract void stubFailure(int httpStatus, String body);

    @Test(expected = ChromaException.class)
    public void testRejectsNullTextList() throws EFException {
        EmbeddingFunction ef = createEmbeddingFunction(wireMock.baseUrl());
        ef.embedDocuments((List<String>) null);
    }

    @Test(expected = ChromaException.class)
    public void testRejectsEmptyTextList() throws EFException {
        EmbeddingFunction ef = createEmbeddingFunction(wireMock.baseUrl());
        ef.embedDocuments(Collections.emptyList());
    }

    @Test
    public void testCountMismatchThrowsChromaException() throws EFException {
        // stub returns 1 embedding for 2 inputs
        stubMismatch(2, 1);
        EmbeddingFunction ef = createEmbeddingFunction(wireMock.baseUrl());
        try {
            ef.embedDocuments(Arrays.asList("a", "b"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("2"));
            assertTrue(e.getMessage().contains("1"));
        }
    }

    @Test
    public void testProviderErrorWrappedAsChromaExceptionWithProviderAndModel() throws EFException {
        stubFailure(429, "{\"error\": \"rate limit\"}");
        EmbeddingFunction ef = createEmbeddingFunction(wireMock.baseUrl());
        try {
            ef.embedDocuments(Collections.singletonList("test"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue("Message must include provider name", e.getMessage().contains(providerName()));
            assertTrue("Message must include model name", e.getMessage().contains(modelName()));
        }
    }

    protected abstract String providerName();  // "openai", "cohere", etc.
    protected abstract String modelName();     // default model name for that provider
}
```

**Why this structure:** WireMock is already in the test classpath (wiremock-jre8 2.35.2). Each provider makes real HTTP calls via OkHttp — WireMock intercepts them without needing real API keys. The `@Rule WireMockRule` with dynamic port avoids port conflicts in parallel test runs.

### Pattern 2: Eager Input Validation in Providers

**What:** Add null/empty list guards at the TOP of `embedDocuments(List<String>)` and `embedQueries(List<String>)` in each provider, before any HTTP call.

**Example (OpenAI as template):**
```java
@Override
public List<Embedding> embedDocuments(List<String> documents) throws EFException {
    if (documents == null) {
        throw new ChromaException(
            "OpenAI embedding failed (model: " + modelName + "): documents must not be null"
        );
    }
    if (documents.isEmpty()) {
        throw new ChromaException(
            "OpenAI embedding failed (model: " + modelName + "): documents must not be empty"
        );
    }
    // ... existing HTTP call
}
```

**Important:** The `EmbeddingFunction` interface uses `throws EFException`. Providers can throw `ChromaException` (which extends `RuntimeException`, not `EFException`) — it is an unchecked exception and does not violate the interface contract. `ChromaHttpCollection.embedQueryTexts()` already catches `ChromaException` separately from `EFException`.

### Pattern 3: Error Message Standardization in Providers

The locked format is: `"{ProviderName} embedding failed (model: {model}): {http-status or cause description}"`

```java
// In createEmbedding() error handling:
if (!response.isSuccessful()) {
    String providerInfo = "OpenAI embedding failed (model: " + configParams.get(EF_PARAMS_MODEL) + ")";
    throw new ChromaException(providerInfo + ": " + response.code() + " " + response.message());
}
```

Each provider already has `configParams.get(Constants.EF_PARAMS_MODEL)` available at the point of the HTTP call.

### Pattern 4: Count-Mismatch Guard After API Call

Add after deserializing the response in each provider:
```java
List<Embedding> result = parseResponse(responseData);
if (result.size() != documents.size()) {
    throw new ChromaException(
        "OpenAI embedding failed (model: " + modelName + "): "
        + "expected " + documents.size() + " embeddings, got " + result.size()
    );
}
return result;
```

### Pattern 5: DefaultEmbeddingFunction — Timeout + Thread-Safe Init

**Current problem:** `downloadAndSetupModel()` uses `new URL(MODEL_DOWNLOAD_URL).openStream()` — no timeout, no retry, blocks indefinitely in CI. Constructor throws `EFException` but the v2 boundary should see `ChromaException`.

**Solution: OkHttp for download (already available as project dependency)**

Use OkHttp (already on the classpath via providers) for the model download — it has built-in timeout configuration. This avoids adding any new dependency.

```java
// Configurable timeout — default 5 minutes (generous for model download in CI)
public static final int DEFAULT_DOWNLOAD_TIMEOUT_SECONDS = 300;

// Lazy, thread-safe initialization guard
private static volatile boolean modelInitialized = false;
private static final Object MODEL_INIT_LOCK = new Object();

// Constructor becomes:
public DefaultEmbeddingFunction() throws EFException {
    this(DEFAULT_DOWNLOAD_TIMEOUT_SECONDS);
}

public DefaultEmbeddingFunction(int downloadTimeoutSeconds) throws EFException {
    ensureModelDownloaded(downloadTimeoutSeconds);
    // ... tokenizer and session init unchanged
}

private static void ensureModelDownloaded(int timeoutSeconds) throws EFException {
    if (modelInitialized) return;
    synchronized (MODEL_INIT_LOCK) {
        if (modelInitialized) return;  // double-checked locking
        downloadAndSetupModel(timeoutSeconds);
        modelInitialized = true;
    }
}
```

**Retry classification (locked decision):**
- Retryable: `SocketTimeoutException`, `ConnectException`, HTTP 5xx responses
- Non-retryable: HTTP 404 (model not found at URL), HTTP 403 (access denied), checksum mismatch (corrupt file)
- Retry policy: retry exactly once on retryable failures, fail fast on non-retryable

```java
private static void downloadAndSetupModel(int timeoutSeconds) throws EFException {
    OkHttpClient downloadClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build();
    // First attempt
    try {
        attemptDownload(downloadClient);
        return;
    } catch (RetryableDownloadException e) {
        // retry once
    }
    // Second attempt (no further retry)
    try {
        attemptDownload(downloadClient);
    } catch (RetryableDownloadException e) {
        throw new EFException(new ChromaException(
            "DefaultEmbeddingFunction: model download timed out after 2 attempts. "
            + "Check network connectivity. Download URL: " + MODEL_DOWNLOAD_URL
        ));
    }
}
```

**Note:** `DefaultEmbeddingFunction` constructor declares `throws EFException` (part of the public API). The constructor wraps the download failure into `EFException`. `EmbeddingFunctionResolver.resolve()` already wraps `EFException` into `ChromaException` when it instantiates `DefaultEmbeddingFunction`.

**Thread safety note:** The `modelInitialized` volatile + synchronized block is Java 5+ and fully Java 8 compatible. The `OrtSession session` and `HuggingFaceTokenizer tokenizer` fields are initialized once inside the synchronized block and read-only thereafter — safe for concurrent embedding calls.

### Pattern 6: SHA-256 Metadata Fallback

**Current behavior:** `Sha256IdGenerator.generate()` throws `IllegalArgumentException` when `document == null`.

**New behavior:** When `document == null`, fall back to hashing serialized metadata. When both are null, throw `IllegalArgumentException`.

**Metadata serialization format (Claude's discretion):** Use sorted key iteration (`TreeMap`) + JSON-like `key=value;` pairs. This is deterministic, documented, and requires no external dependency.

```java
@Override
public String generate(String document, Map<String, Object> metadata) {
    if (document == null && metadata == null) {
        throw new IllegalArgumentException(
            "Sha256IdGenerator requires a non-null document or metadata"
        );
    }
    String content = document != null ? document : serializeMetadata(metadata);
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return hexEncode(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 not available", e);
    }
}

/**
 * Deterministic metadata serialization for SHA-256 hashing.
 * Format: sorted key=value pairs joined by semicolons.
 * Keys are sorted lexicographically. Values use Object.toString().
 * This format is stable and documented; do not change without a version migration plan.
 */
static String serializeMetadata(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
        return "";
    }
    Map<String, Object> sorted = new TreeMap<String, Object>(metadata);
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        if (sb.length() > 0) sb.append(';');
        sb.append(entry.getKey()).append('=');
        sb.append(entry.getValue() == null ? "null" : entry.getValue().toString());
    }
    return sb.toString();
}
```

**Package-private `serializeMetadata()`** so it can be tested directly in `IdGeneratorTest`.

### Pattern 7: EF Precedence Warning Log

When explicit EF is set at runtime AND the collection has a persisted config EF with a different name, log a warning:

```java
// In requireEmbeddingFunction():
private synchronized EmbeddingFunction requireEmbeddingFunction() {
    if (explicitEmbeddingFunction != null) {
        // Warn if name differs from persisted spec
        if (embeddingFunctionSpec != null) {
            String specName = embeddingFunctionSpec.getName();
            // We don't have the runtime EF's "name" easily, so we log unconditionally
            // when both exist — this is safe and matches Go client behavior
            LOG.warning("Runtime embedding function overrides persisted collection EF '"
                + specName + "'. Explicit EF takes precedence.");
        }
        embeddingFunction = explicitEmbeddingFunction;
        return explicitEmbeddingFunction;
    }
    if (embeddingFunction != null) {
        return embeddingFunction;
    }
    if (embeddingFunctionSpec == null) {
        throw new ChromaException("...actionable message...");
    }
    LOG.fine("Auto-wired embedding function: " + embeddingFunctionSpec.getName()
        + " from collection configuration");
    embeddingFunction = EmbeddingFunctionResolver.resolve(embeddingFunctionSpec);
    return embeddingFunction;
}
```

**Precision note:** The warning should only fire when a conflict actually exists (explicit EF + spec both present). The current implementation already has `explicitEmbeddingFunction` and `embeddingFunctionSpec` as fields, so both checks are straightforward.

### Pattern 8: Duplicate IDs in Explicit ID Lists

The locked decision says "Duplicate IDs within a single add/upsert batch must fail fast with `ChromaException`". Currently, `generateIds()` in `ChromaHttpCollection` already does duplicate detection for generator-produced IDs. The gap is for **explicit `ids(...)` lists**.

Add a duplicate check in `AddBuilderImpl.execute()` and `UpsertBuilderImpl.execute()` for the explicit IDs path:

```java
// After resolvedIds is obtained (both explicit and generator paths):
checkForDuplicateIds(resolvedIds);
```

```java
private static void checkForDuplicateIds(List<String> ids) {
    Map<String, List<Integer>> indexesById = new LinkedHashMap<String, List<Integer>>();
    boolean hasDuplicate = false;
    for (int i = 0; i < ids.size(); i++) {
        String id = ids.get(i);
        List<Integer> indexes = indexesById.get(id);
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            indexesById.put(id, indexes);
        } else {
            hasDuplicate = true;
        }
        indexes.add(Integer.valueOf(i));
    }
    if (hasDuplicate) {
        throw new ChromaException(buildDuplicateIdsMessage(indexesById));
    }
}
```

This can reuse the existing `buildDuplicateIdsMessage()` helper. The exception type should be `ChromaException` (not `IllegalArgumentException`) per the locked decision.

### Anti-Patterns to Avoid

- **Don't add a static warmup method to DefaultEmbeddingFunction.** Locked as lazy-download-only. No `warmup()`, no `preload()`.
- **Don't add Mockito as a test dependency.** The project only uses JUnit 4 + WireMock for HTTP stubbing. Conformance tests must use WireMock, not Mockito.
- **Don't throw `EFException` from the conformance validation guards.** Use `ChromaException` (unchecked). `ChromaHttpCollection` already catches `ChromaException` before `EFException` in `embedQueryTexts()`.
- **Don't change the `EmbeddingFunction` interface signature.** It is part of the public API with Java 8 compatibility constraints.
- **Don't use `synchronized` on embedding method calls.** Thread safety in `DefaultEmbeddingFunction` only applies to the one-time initialization; the ONNX session itself (`OrtSession`) is documented as thread-safe after creation. Adding `synchronized` to every embed call would be a correctness over-read.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP stub server for provider tests | Custom mock EF implementations that don't make HTTP calls | WireMock (`WireMockRule` with dynamic port) | Providers use real OkHttp clients; WireMock intercepts at the socket level, testing the full request/response path including deserialization |
| Timeout for ONNX download | Custom InputStream wrapper with timeout | OkHttp client (already on classpath) with `.readTimeout()` | OkHttp is already a project dependency via providers; consistent with the rest of the codebase |
| Deterministic Map serialization for SHA-256 | Custom serialization library | `java.util.TreeMap` + string builder | Zero external dependency, deterministic key order, Java 8 compatible |
| Thread-safe singleton initialization | External concurrency library | `volatile` + `synchronized` double-checked locking | Standard Java 5+ pattern, no additional dependencies, correct for single initialization |

---

## Common Pitfalls

### Pitfall 1: EFException vs ChromaException at Provider Boundaries

**What goes wrong:** Provider methods declare `throws EFException`. Adding ChromaException throws inside those methods seems like a type mismatch.

**Why it happens:** Developers see `throws EFException` and assume all exceptions from providers must be EFException.

**How to avoid:** `ChromaException` extends `RuntimeException` (unchecked). It does NOT need to be declared in the `throws` clause. You can throw `ChromaException` from any method regardless of its `throws` declaration. Verify: `ChromaException.java` confirms `extends RuntimeException`.

**Warning signs:** Compilation error "unreported exception" — this would appear only for checked exceptions. ChromaException is unchecked, so no such error.

### Pitfall 2: Breaking TestDefaultEmbeddings Ground-Truth Test

**What goes wrong:** Refactoring `DefaultEmbeddingFunction` for timeout/thread-safety breaks the static field initialization or the constructor, causing `TestDefaultEmbeddings.testDefaultEmbeddingFunction()` to fail.

**Why it happens:** The current implementation has static fields (`MODEL_CACHE_DIR`, `modelPath`, `modelFile`) and instance fields (`tokenizer`, `env`, `session`). Moving to lazy init changes when these are set.

**How to avoid:** Keep `MODEL_CACHE_DIR`, `modelPath`, `modelFile` as static constants (they are pure path computations, not download-dependent). Move only the download trigger and session initialization into the lazy-init block. The `tokenizer` and `session` fields can remain instance fields initialized inside the constructor after the download check passes.

**Warning signs:** `NullPointerException` on `session.run()` or `tokenizer.batchEncode()` — indicates the lazy init didn't complete before embedding calls.

### Pitfall 3: modelInitialized Static Flag Not Reset Between Tests

**What goes wrong:** Making `modelInitialized` a static volatile field means it persists across test instances in the same JVM. If `TestDownloadModel` deletes the model dir and then creates a new `DefaultEmbeddingFunction`, the static flag says "initialized" but the model files are gone.

**Why it happens:** Static state in unit-tested classes is a known JUnit 4 trap (no per-test class reloading).

**How to avoid:** The `modelInitialized` flag should be set based on `validateModel()` returning true, not just "constructor was called". On each `ensureModelDownloaded()` call, first check `validateModel()`. If the flag is true but `validateModel()` returns false (files deleted), re-download.

Alternatively: don't use a static boolean flag. Instead, use the existing `validateModel()` call at the start of `ensureModelDownloaded()` as the gate — if the model file is present, skip download. This is stateless and naturally handles the "deleted between tests" case.

### Pitfall 4: WireMock Port Conflicts in Conformance Test Suite

**What goes wrong:** Multiple `WireMockRule` instances with fixed ports fail when tests run in parallel.

**Why it happens:** Each conformance test subclass has its own `@Rule WireMockRule`.

**How to avoid:** Always use `wireMockConfig().dynamicPort()`. All existing WireMock tests in the codebase already do this.

### Pitfall 5: SHA-256 Metadata Serialization Non-Determinism

**What goes wrong:** Using `HashMap` (or any unordered map) for metadata serialization produces different SHA-256 hashes for the same metadata depending on JVM iteration order.

**Why it happens:** `HashMap` does not guarantee insertion order. Java's Map serialization through `toString()` is also non-deterministic.

**How to avoid:** Always copy to `TreeMap` (natural string sort order) before iterating. Document the format in Javadoc so callers understand the stability guarantee.

### Pitfall 6: Sha256IdGenerator INSTANCE Singleton Is Not Thread-Safe After Metadata Fallback

**What goes wrong:** If `serializeMetadata()` modifies any shared state, concurrent calls would corrupt each other.

**Why it happens:** Singleton pattern is only safe for stateless operations.

**How to avoid:** `serializeMetadata()` must be a pure function using only local variables (`TreeMap` copy, `StringBuilder`). No instance state. Verified: the current `generate()` method creates a new `MessageDigest` per call (SHA-256 MessageDigest is not thread-safe; `MessageDigest.getInstance()` creates a new instance). This pattern must be preserved.

### Pitfall 7: Explicit ID Duplicate Detection Performance

**What goes wrong:** Naive O(n^2) duplicate check using `List.contains()` causes noticeable slowdown for large batches.

**Why it happens:** Easy to reach for `contains()` in a loop.

**How to avoid:** Use `LinkedHashMap<String, List<Integer>>` (same as `generateIds()` already does) for O(n) detection while preserving insertion order for the error message.

---

## Code Examples

### Example 1: WireMock Provider Stub (OpenAI pattern)

```java
// Source: existing ChromaHttpCollectionTest.java + WireMock 2.35 docs
// In OpenAIEmbeddingFunctionConformanceTest:
@Before
public void stubSuccessResponse() {
    // Stub a 200 response with 1 embedding for 1 input
    wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"data\":[{\"embedding\":[0.1,0.2,0.3],\"index\":0}],"
                + "\"model\":\"text-embedding-ada-002\"}")));
}

@Before
public void stubRateLimitResponse() {
    wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
        .withRequestBody(containing("rate_limit_trigger"))
        .willReturn(aResponse()
            .withStatus(429)
            .withBody("{\"error\":{\"message\":\"rate limit exceeded\"}}")));
}

@Override
protected EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException {
    return new OpenAIEmbeddingFunction(
        WithParam.baseAPI(baseUrl + "/v1/embeddings"),
        WithParam.apiKey("test-key")
    );
}
```

### Example 2: requireEmbeddingFunction() with Precedence Javadoc

```java
// In ChromaHttpCollection.java
/**
 * Resolves the embedding function for text embedding operations.
 *
 * <p><strong>Precedence (highest to lowest):</strong></p>
 * <ol>
 *   <li>Runtime/explicit EF — set via {@code CreateCollectionOptions.embeddingFunction(...)}
 *       or {@code client.getCollection(name, embeddingFunction)}. Always wins.</li>
 *   <li>{@code configuration.embedding_function} — persisted in collection configuration.</li>
 *   <li>{@code schema.default_embedding_function} — persisted in collection schema.</li>
 * </ol>
 *
 * <p>When an explicit EF is provided and a persisted EF descriptor also exists,
 * a WARNING is logged. The explicit EF is used; no error is thrown.</p>
 *
 * <p>Unsupported EF descriptors (unknown provider name) do not block collection
 * construction. They fail lazily at the first embed operation.</p>
 */
private synchronized EmbeddingFunction requireEmbeddingFunction() { ... }
```

### Example 3: OkHttp-Based Download with Timeout

```java
// In DefaultEmbeddingFunction.java
private static void attemptDownload(OkHttpClient client, Path archivePath) throws IOException {
    Request request = new Request.Builder().url(MODEL_DOWNLOAD_URL).get().build();
    try (Response response = client.newCall(request).execute()) {
        if (response.code() == 404 || response.code() == 403) {
            throw new NonRetryableDownloadException(
                "DefaultEmbeddingFunction: model download failed with HTTP "
                + response.code() + " at " + MODEL_DOWNLOAD_URL
                + ". This is a non-retryable error."
            );
        }
        if (!response.isSuccessful()) {
            throw new RetryableDownloadException(
                "DefaultEmbeddingFunction: model download failed with HTTP "
                + response.code() + ". Will retry once."
            );
        }
        try (InputStream in = response.body().byteStream()) {
            Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
```

### Example 4: Duplicate ID Check for Explicit Lists

```java
// In ChromaHttpCollection (after resolveIds() returns):
private static void checkForDuplicateIds(List<String> ids) {
    // Only worth checking when there are enough IDs to potentially duplicate
    if (ids == null || ids.size() < 2) return;
    Map<String, List<Integer>> seen = new LinkedHashMap<String, List<Integer>>();
    boolean hasDuplicate = false;
    for (int i = 0; i < ids.size(); i++) {
        String id = ids.get(i);
        List<Integer> indexes = seen.get(id);
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            seen.put(id, indexes);
        } else {
            hasDuplicate = true;
        }
        indexes.add(Integer.valueOf(i));
    }
    if (hasDuplicate) {
        throw new ChromaException(buildDuplicateIdsMessage(seen));
    }
}
```

---

## State of the Art (What Already Works)

| Existing Capability | Status | Notes |
|--------------------|--------|-------|
| `EmbeddingFunctionResolver.resolve()` | Working | Wraps `EFException` and `RuntimeException` into `ChromaException` at resolver level. Does NOT include model name in message. |
| `ChromaHttpCollection.requireEmbeddingFunction()` | Working | Precedence chain implemented (explicit > config > schema). No warning log when both explicit + spec exist. No DEBUG log on auto-wire. |
| `ChromaHttpCollection.embedQueryTexts()` | Working | Count-mismatch guard present. Error message is generic ("Failed to embed queryTexts"). Does NOT include provider name or model. |
| `generateIds()` in `ChromaHttpCollection` | Working | Duplicate detection for generator-produced IDs. NOT applied to explicit `ids(...)` lists. Custom generator output validated (null/blank check with index). |
| `Sha256IdGenerator` | Working | Hashes document. Throws `IllegalArgumentException` when `document == null`. Does NOT support metadata fallback. |
| `UuidIdGenerator` | Working | Complete. No changes needed. |
| `UlidIdGenerator` | Working | Complete. No changes needed. |
| `DefaultEmbeddingFunction` | Partially working | Download works but no timeout, no retry, no thread safety on init, blocks indefinitely on slow networks. |
| All 4 remote providers | Partially working | No null/empty input guard. Error messages are `IOException("Unexpected code " + response)` — not `ChromaException`, not provider+model format. |

| Old Approach | New Approach | When | Impact |
|--------------|-------------|------|--------|
| `new URL(url).openStream()` (no timeout) | OkHttp client with configurable `readTimeout` | Phase 3 | Prevents CI stalls on model download |
| `throw new IOException("Unexpected code " + response)` in providers | `throw new ChromaException("Provider embedding failed (model: X): ...")` | Phase 3 | User-facing error messages are actionable |
| No null/empty guard in providers before HTTP call | Eager validation, `ChromaException` before HTTP call | Phase 3 | Faster failure, no wasted API credits |
| No duplicate check on explicit ID lists | `checkForDuplicateIds()` in execute() before HTTP call | Phase 3 | Catches data bugs before server-side error |
| Sha256 requires document, ignores metadata | Sha256 falls back to deterministic metadata hash | Phase 3 | Enables content-addressable dedup for embeddings-only records |

---

## Open Questions

1. **DefaultEmbeddingFunction: use OkHttp or HttpURLConnection for download?**
   - What we know: OkHttp is already on the classpath (used by all four providers). `HttpURLConnection` is JDK built-in and has `setReadTimeout()`.
   - What's unclear: whether the OkHttp version on the classpath exposes the needed timeout API without conflict.
   - Recommendation: Use OkHttp. It is already tested in the CI environment for provider HTTP calls. Consistent with the rest of the codebase. The `connectTimeout` + `readTimeout` builder API is stable.

2. **Conformance test: abstract class or parameterized test?**
   - What we know: JUnit 4 parameterized tests (`@RunWith(Parameterized.class)`) require a common factory pattern. WireMock `@Rule` injection is harder with parameterized runners since `WireMockRule` uses JUnit 4 rules which depend on the runner.
   - What's unclear: whether `WireMockRule` works with `@RunWith(Parameterized.class)`.
   - Recommendation: Use abstract base class. WireMock `@Rule` is inherited and works cleanly in concrete subclasses. This is the pattern already established in `AbstractChromaIntegrationTest.java`.

3. **modelInitialized static flag: how to handle test isolation?**
   - What we know: `TestDefaultEmbeddings` calls `FileUtils.deleteDirectory()` on the model cache in `testDownloadModel()`, then constructs a new `DefaultEmbeddingFunction`.
   - What's unclear: if a static boolean is used, the second construction would skip download (flag already true) but files are deleted.
   - Recommendation: Don't use a static boolean. Use `validateModel()` as the gate inside `ensureModelDownloaded()`. If `validateModel()` returns false, download even if we've been through init before. This is stateless and handles re-download correctly.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) |
| Config file | none — Maven Surefire discovers `*Test.java` |
| Quick run command | `mvn test -Dtest=IdGeneratorTest,EmbeddingFunctionResolverTest` |
| Full suite (unit only) command | `mvn test -Dgroups=""` (or exclude integration via naming) |
| Full suite command | `mvn test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EMB-01 | OpenAI null input rejected with ChromaException | unit | `mvn test -Dtest=OpenAIEmbeddingFunctionConformanceTest` | Wave 0 |
| EMB-01 | OpenAI empty list rejected with ChromaException | unit | `mvn test -Dtest=OpenAIEmbeddingFunctionConformanceTest` | Wave 0 |
| EMB-01 | OpenAI count mismatch throws ChromaException | unit | `mvn test -Dtest=OpenAIEmbeddingFunctionConformanceTest` | Wave 0 |
| EMB-01 | OpenAI provider error wraps to ChromaException with provider+model | unit | `mvn test -Dtest=OpenAIEmbeddingFunctionConformanceTest` | Wave 0 |
| EMB-01 | Cohere/HF/Ollama — same conformance behaviors | unit | `mvn test -Dtest=CohereEmbeddingFunctionConformanceTest,HuggingFaceEmbeddingFunctionConformanceTest,OllamaEmbeddingFunctionConformanceTest` | Wave 0 |
| EMB-02 | DefaultEF throws ChromaException on download timeout | unit | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | Wave 0 |
| EMB-02 | DefaultEF thread-safe: concurrent embed calls succeed | unit | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | Wave 0 |
| EMB-02 | DefaultEF non-retryable 404 fails fast | unit | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | Wave 0 |
| EMB-03 | Explicit EF wins over config spec | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | Wave 0 |
| EMB-03 | Config EF wins over schema EF | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | Wave 0 |
| EMB-03 | Warning logged when explicit EF + spec both present | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | Wave 0 |
| EMB-03 | DEBUG log on auto-wire | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | Wave 0 |
| EMB-04 | Duplicate explicit IDs fail with ChromaException | unit | `mvn test -Dtest=ChromaHttpCollectionTest` | Extend existing |
| EMB-04 | Sha256 metadata fallback when document is null | unit | `mvn test -Dtest=IdGeneratorTest` | Extend existing |
| EMB-04 | Sha256 both-null throws IllegalArgumentException | unit | `mvn test -Dtest=IdGeneratorTest` | Extend existing |
| EMB-04 | Lambda generator null output fails with ChromaException at index | unit | `mvn test -Dtest=ChromaHttpCollectionTest` | Extend existing |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=IdGeneratorTest,EmbeddingFunctionResolverTest,ChromaHttpCollectionTest -pl . -am`
- **Per wave merge:** `mvn test` (unit suite, no integration containers)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java` — covers EMB-01 null/empty/count-mismatch/error-format across providers
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/OpenAIEmbeddingFunctionConformanceTest.java` — WireMock stubs for OpenAI
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/CohereEmbeddingFunctionConformanceTest.java` — WireMock stubs for Cohere
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceEmbeddingFunctionConformanceTest.java` — WireMock stubs for HuggingFace
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/OllamaEmbeddingFunctionConformanceTest.java` — WireMock stubs for Ollama
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java` — covers EMB-02 timeout/retry/thread-safety (separate from the existing `TestDefaultEmbeddings` ground-truth test which requires actual model download)
- [ ] `src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java` — covers EMB-03 precedence chain contract

Existing files to extend:
- `src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java` — add SHA-256 metadata fallback and both-null cases
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` — add duplicate explicit-ID detection tests

---

## Sources

### Primary (HIGH confidence)
- Direct code reading of all referenced source files — `EmbeddingFunction.java`, `DefaultEmbeddingFunction.java`, `EmbeddingFunctionResolver.java`, `ChromaHttpCollection.java`, `Sha256IdGenerator.java`, `UlidIdGenerator.java`, `UuidIdGenerator.java`, `IdGenerator.java`, `EmbeddingFunctionSpec.java`, `ChromaException.java`
- Direct code reading of all provider implementations — `OpenAIEmbeddingFunction.java`, `CohereEmbeddingFunction.java`, `HuggingFaceEmbeddingFunction.java`, `OllamaEmbeddingFunction.java`
- Direct code reading of existing tests — `IdGeneratorTest.java`, `EmbeddingFunctionResolverTest.java`, `EmbeddingFunctionCompatibilityTest.java`, `ChromaHttpCollectionTest.java`, `TestDefaultEmbeddings.java`
- `pom.xml` — confirmed JUnit 4.13.2, WireMock wiremock-jre8 2.35.2, TestContainers BOM 1.21.4, no Mockito
- `03-CONTEXT.md` — all locked decisions are implementation-ready with sufficient detail

### Secondary (MEDIUM confidence)
- `STATE.md` blocker note: "Full `mvn test` run was interrupted after stalling on first-time ONNX model download" — confirms the CI stall bug motivating DefaultEmbeddingFunction timeout work

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified in pom.xml; no new dependencies needed
- Architecture: HIGH — based on direct reading of all relevant source files; patterns match existing codebase conventions
- Pitfalls: HIGH — derived from actual code reading (e.g., EFException vs ChromaException gap, static modelInitialized flag risk, WireMock dynamic port requirement)
- Validation: HIGH — test infrastructure fully understood; gaps identified based on what doesn't exist yet

**Research date:** 2026-03-19
**Valid until:** 2026-04-19 (stable codebase; no external API dependencies in scope)
