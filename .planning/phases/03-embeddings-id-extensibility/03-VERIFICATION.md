---
phase: 03-embeddings-id-extensibility
verified: 2026-03-19T12:30:00Z
status: passed
score: 13/13 must-haves verified
re_verification: false
---

# Phase 3: Embeddings & ID Extensibility Verification Report

**Phase Goal:** Ensure embedding and ID workflows are deterministic, extensible, and safe for production ingestion/query paths.
**Verified:** 2026-03-19T12:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | All four remote providers reject null text lists with ChromaException before making HTTP calls | VERIFIED | OpenAI, Cohere, HuggingFace, Ollama all have null guards at top of `embedDocuments(List<String>)` throwing `ChromaException` with provider+model message |
| 2  | All four remote providers reject empty text lists with ChromaException before making HTTP calls | VERIFIED | `documents must not be empty` guards confirmed in all four providers |
| 3  | All four remote providers throw ChromaException with provider name and model name in the message on API failure | VERIFIED | Pattern `"{Provider} embedding failed (model: ..." + response.code()` confirmed in OpenAI, Cohere, HuggingFace, Ollama |
| 4  | All four remote providers throw ChromaException when returned embedding count does not match input count | VERIFIED | `result.size() != documents.size()` count-mismatch guards present in all four providers |
| 5  | Explicit runtime EF always takes precedence over config/schema EF descriptor | VERIFIED | `requireEmbeddingFunction()` returns `explicitEmbeddingFunction` first; `EmbeddingPrecedenceTest.testExplicitEFWinsOverConfigSpec()` contract-tests this |
| 6  | Warning is logged when explicit EF overrides a persisted EF spec | VERIFIED | `LOG.warning("Runtime embedding function overrides persisted collection EF '..."` at line 1142 of `ChromaHttpCollection.java`; `testWarningLoggedWhenExplicitOverridesSpec` passes |
| 7  | Auto-wired EF resolution is logged at FINE/DEBUG level | VERIFIED | `LOG.fine("Auto-wired embedding function: " + embeddingFunctionSpec.getName() + ...)` at line 1161; `testAutoWireLoggedAtFineLevel` passes |
| 8  | Precedence chain is documented in Javadoc on Collection interface and requireEmbeddingFunction() | VERIFIED | `Collection.java` has `<h3>Embedding Function Precedence</h3>` section; `ChromaHttpCollection.java` has `Precedence (highest to lowest)` Javadoc on `requireEmbeddingFunction()` |
| 9  | DefaultEmbeddingFunction downloads the ONNX model with a configurable timeout instead of blocking indefinitely | VERIFIED | `OkHttpClient.Builder().readTimeout(timeoutSeconds, ...)` in `DefaultEmbeddingFunction.java`; `DEFAULT_DOWNLOAD_TIMEOUT_SECONDS = 300`; overloaded constructor `DefaultEmbeddingFunction(int)` present |
| 10 | Retryable download failures are retried exactly once before failing | VERIFIED | Two-attempt structure (`RetryableDownloadException` caught after first attempt, rethrows after second); `testRetryableServerErrorRetriesThenThrowsChromaException` verifies 2 HTTP requests |
| 11 | Non-retryable download failures fail fast on first attempt | VERIFIED | `NonRetryableDownloadException` thrown immediately for HTTP 404/403; `testNonRetryable404ThrowsChromaException` verifies exactly 1 request |
| 12 | Sha256IdGenerator hashes metadata when document is null, producing a deterministic ID | VERIFIED | `document != null ? document : serializeMetadata(metadata)` in `Sha256IdGenerator.generate()`; `serializeMetadata()` uses `TreeMap` for sorted key=value serialization |
| 13 | Duplicate IDs in explicit ID lists fail fast with ChromaException listing the duplicates | VERIFIED | `checkForDuplicateIds(resolvedIds)` called in both `AddBuilderImpl.execute()` and `UpsertBuilderImpl.execute()` when `hasExplicitIds(ids)` is true |

**Score:** 13/13 truths verified

### Required Artifacts

| Artifact | Min Lines | Actual Lines | Status | Details |
|----------|-----------|--------------|--------|---------|
| `src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java` | 60 | 146 | VERIFIED | Contains `testRejectsNullDocumentList`, `testCountMismatchThrowsChromaException`, 5 behavioral tests |
| `src/test/java/tech/amikos/chromadb/embeddings/OpenAIConformanceTest.java` | 40 | 67 | VERIFIED | Extends abstract base, WireMock stubs for `/v1/embeddings` |
| `src/test/java/tech/amikos/chromadb/embeddings/CohereConformanceTest.java` | 40 | 69 | VERIFIED | Extends abstract base, WireMock stubs for `/v1/embed` |
| `src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceConformanceTest.java` | 40 | 69 | VERIFIED | Extends abstract base, WireMock stubs for model path |
| `src/test/java/tech/amikos/chromadb/embeddings/OllamaConformanceTest.java` | 40 | 66 | VERIFIED | Extends abstract base, WireMock stubs for `/api/embed` |
| `src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java` | 50 | 299 | VERIFIED | 5 contract tests: explicit wins, config spec used, warning logged, fine log, no-EF error |
| `src/main/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunction.java` | — | 356 | VERIFIED | OkHttp download, `DEFAULT_DOWNLOAD_TIMEOUT_SECONDS=300`, `MODEL_INIT_LOCK`, `modelDownloadUrl` package-private, `RetryableDownloadException`, `NonRetryableDownloadException`, no `URL.openStream()` |
| `src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java` | 100 | 194 | VERIFIED | 7 tests: timeout constant, cache dir stability, custom timeout constructor, concurrent thread safety, 404 non-retryable, 503 retryable, timeout ChromaException |
| `src/main/java/tech/amikos/chromadb/v2/Sha256IdGenerator.java` | — | 87 | VERIFIED | `serializeMetadata()` package-private, `TreeMap` sorted keys, `document != null ? document : serializeMetadata(metadata)`, both-null guard |
| `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` | — | 1227 | VERIFIED | `checkForDuplicateIds()` present at line 1070, called from `AddBuilderImpl.execute()` (line 493) and `UpsertBuilderImpl.execute()` (line 587); `generateIds()` throws `ChromaException` for null/blank output and generator exceptions |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `OpenAIEmbeddingFunction.embedDocuments()` | `ChromaException` | null/empty guard + error wrapping | WIRED | Lines 90-109: null guard, empty guard, API error wrapping, count-mismatch check all present |
| `ChromaHttpCollection.requireEmbeddingFunction()` | `LOG.warning` | EF name mismatch warning | WIRED | Line 1142: `LOG.warning("Runtime embedding function overrides persisted collection EF '...`)` |
| `ChromaHttpCollection.requireEmbeddingFunction()` | `LOG.fine` | Auto-wire debug log | WIRED | Line 1161: `LOG.fine("Auto-wired embedding function: " + embeddingFunctionSpec.getName() + ...)` |
| `DefaultEmbeddingFunction.ensureModelDownloaded()` | `OkHttpClient` | HTTP download with configurable readTimeout | WIRED | Line 223-225: `new OkHttpClient.Builder().readTimeout(timeoutSeconds, ...)` |
| `DefaultEmbeddingFunction.ensureModelDownloaded()` | `validateModel()` | Stateless gate check | WIRED | Lines 205, 209: `validateModel()` called twice in double-checked locking pattern |
| `DefaultEmbeddingFunction.downloadModel()` | `ChromaException` | Throws ChromaException on timeout and retry exhaustion | WIRED | Lines 244, 254: `new ChromaException("...model download failed after 2 attempts...")` |
| `Sha256IdGenerator.generate()` | `serializeMetadata()` | Fallback when document is null | WIRED | Line 41: `String content = document != null ? document : serializeMetadata(metadata)` |
| `ChromaHttpCollection.AddBuilderImpl.execute()` | `checkForDuplicateIds()` | Explicit ID duplicate detection before HTTP call | WIRED | Lines 492-494: `if (hasExplicitIds(ids)) { checkForDuplicateIds(resolvedIds); }` |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| EMB-01 | 03-01-PLAN.md | User can use OpenAI, Cohere, HuggingFace, and Ollama embedding functions through one consistent embedding contract | SATISFIED | All four providers: null/empty guards, ChromaException with provider+model, count-mismatch detection; conformance test suite (20 tests) passes |
| EMB-02 | 03-02-PLAN.md | User can use the default local embedding function without external model API keys | SATISFIED | `DefaultEmbeddingFunction` replaced `URL.openStream()` with OkHttp, 300s configurable timeout, retry-once for transient failures, fail-fast for 404/403, thread-safe double-checked locking |
| EMB-03 | 03-01-PLAN.md | User can provide a custom embedding function, and runtime/descriptor precedence is deterministic and documented | SATISFIED | Precedence Javadoc on `Collection` interface and `requireEmbeddingFunction()`; `EmbeddingPrecedenceTest` (5 tests) locks the chain; WARNING and FINE logs wired |
| EMB-04 | 03-03-PLAN.md | User can generate deterministic or random IDs (UUID, ULID, SHA-256) for add/upsert flows with client-side validation | SATISFIED | `Sha256IdGenerator` metadata fallback with TreeMap serialization; `checkForDuplicateIds()` for explicit ID lists; `generateIds()` throws `ChromaException` for null/blank/exception generator output |

No orphaned requirements — all four requirements (EMB-01 through EMB-04) were claimed by plans and verified in the codebase.

### Anti-Patterns Found

No anti-patterns found in key modified files. No TODO/FIXME/placeholder markers in production code. No stub implementations (return null, return empty collections). All handlers have substantive implementations.

### Human Verification Required

None. All behaviors are verifiable through code inspection and the WireMock-backed test suite. No UI, real-time, or external service behaviors requiring human verification.

### Gaps Summary

No gaps. All 13 observable truths verified, all artifacts are substantive and wired, all key links confirmed, and all four requirements are satisfied with test coverage.

---

## Commit Verification

All commits referenced in SUMMARYs are present in git history:
- `8732809` — feat(03-01): normalize provider error handling with ChromaException conformance tests
- `1734894` — feat(03-01): add EF precedence Javadoc, warning/debug logging, and contract tests
- `236572e` — feat(03-02): refactor DefaultEmbeddingFunction with OkHttp download, timeout, retry, and thread safety
- `7d474fc` — feat(03-02): add DefaultEmbeddingFunctionTest with WireMock-based download reliability tests
- `fcca865` — feat(03-03): extend Sha256IdGenerator with metadata fallback and both-null guard
- `c79d1c7` — feat(03-03): add duplicate ID detection for explicit lists and ChromaException for generator failures

---

_Verified: 2026-03-19T12:30:00Z_
_Verifier: Claude (gsd-verifier)_
