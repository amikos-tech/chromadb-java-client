---
phase: 03
slug: embeddings-id-extensibility
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-19
validated: 2026-03-20
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) |
| **Config file** | none -- Maven Surefire discovers `*Test.java` |
| **Quick run command** | `mvn test -Dtest=IdGeneratorTest,EmbeddingPrecedenceTest,DefaultEmbeddingFunctionTest` |
| **Full suite command** | `mvn test -Dtest=OpenAIConformanceTest,CohereConformanceTest,HuggingFaceConformanceTest,OllamaConformanceTest,EmbeddingPrecedenceTest,DefaultEmbeddingFunctionTest,EmbeddingFunctionCompatibilityTest,IdGeneratorTest,"ChromaHttpCollectionTest#testAddWithDuplicateExplicitIdsFails+testUpsertWithDuplicateExplicitIdsFails+testAddWithNullGeneratorOutputFailsWithChromaException+testAddWithGeneratorExceptionFailsWithChromaException+testAddIdGeneratorRejectsDuplicateGeneratedIds+testAddIdGeneratorWrapsGeneratorExceptionWithRecordIndex+testAddIdGeneratorRejectsNullGeneratedId+testAddIdGeneratorRejectsEmptyGeneratedId"` |
| **Estimated runtime** | ~15 seconds (unit only, no container) |

---

## Sampling Rate

- **After every task commit:** Run quick run command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | EMB-01 | unit | `mvn test -Dtest=OpenAIConformanceTest,CohereConformanceTest,HuggingFaceConformanceTest,OllamaConformanceTest` | yes | green |
| 03-01-02 | 01 | 1 | EMB-03 | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | yes | green |
| 03-02-01 | 02 | 2 | EMB-02 | unit | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | yes | green |
| 03-02-02 | 02 | 2 | EMB-02 | unit | `mvn test -Dtest=EmbeddingFunctionCompatibilityTest` | yes | green |
| 03-03-01 | 03 | 2 | EMB-04 | unit | `mvn test -Dtest=IdGeneratorTest` | yes | green |
| 03-03-02 | 03 | 2 | EMB-04 | unit | `mvn test -Dtest="ChromaHttpCollectionTest#testAddWithDuplicateExplicitIdsFails+testUpsertWithDuplicateExplicitIdsFails+testAddWithNullGeneratorOutputFailsWithChromaException+testAddWithGeneratorExceptionFailsWithChromaException"` | yes | green |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [x] `src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java` -- abstract conformance base for EMB-01 (6 behavioral tests)
- [x] `src/test/java/tech/amikos/chromadb/embeddings/OpenAIConformanceTest.java` -- WireMock stubs for OpenAI (6 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/embeddings/CohereConformanceTest.java` -- WireMock stubs for Cohere (6 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceConformanceTest.java` -- WireMock stubs for HuggingFace (6 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/embeddings/OllamaConformanceTest.java` -- WireMock stubs for Ollama (6 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java` -- EMB-03 precedence chain contract (5 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java` -- EMB-02 download reliability (7 tests, all green)

Existing files extended (no Wave 0 needed):
- [x] `src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java` -- SHA-256 metadata fallback + both-null + serialization (34 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` -- duplicate explicit ID detection + generator failures (4 new + 7 updated tests, all green)

---

## Requirement Coverage Summary

### EMB-01: Consistent Embedding Contract (24 tests)

| Behavior | Test Method | Provider Coverage |
|----------|------------|-------------------|
| Null document list rejected with ChromaException | `testRejectsNullDocumentList` | OpenAI, Cohere, HuggingFace, Ollama |
| Empty document list rejected with ChromaException | `testRejectsEmptyDocumentList` | OpenAI, Cohere, HuggingFace, Ollama |
| Null query rejected with ChromaException | `testRejectsNullQuery` | OpenAI, Cohere, HuggingFace, Ollama |
| Count mismatch throws ChromaException | `testCountMismatchThrowsChromaException` | OpenAI, Cohere, HuggingFace, Ollama |
| API error wrapped as ChromaException with provider+model | `testProviderErrorWrappedAsChromaException` | OpenAI, Cohere, HuggingFace, Ollama |
| Successful embedding returns correct count | `testSuccessfulEmbedding` | OpenAI, Cohere, HuggingFace, Ollama |

### EMB-02: Default Local Embedding Reliability (9 tests)

| Behavior | Test Method | File |
|----------|------------|------|
| Default timeout is 300 seconds | `testDefaultTimeoutConstant` | DefaultEmbeddingFunctionTest |
| Model cache dir is stable | `testModelCacheDirIsStable` | DefaultEmbeddingFunctionTest |
| Custom timeout constructor accepted | `testConstructorAcceptsCustomTimeout` | DefaultEmbeddingFunctionTest |
| Concurrent construction is thread-safe | `testConcurrentConstructionThreadSafety` | DefaultEmbeddingFunctionTest |
| Non-retryable 404 fails fast (1 request) | `testNonRetryable404ThrowsChromaException` | DefaultEmbeddingFunctionTest |
| Retryable 503 retries once then fails (2 requests) | `testRetryableServerErrorRetriesThenThrowsChromaException` | DefaultEmbeddingFunctionTest |
| Timeout throws ChromaException with actionable message | `testTimeoutThrowsChromaExceptionWithActionableMessage` | DefaultEmbeddingFunctionTest |
| EmbeddingFunction interface compatibility | 2 tests | EmbeddingFunctionCompatibilityTest |

### EMB-03: Deterministic EF Precedence (5 tests)

| Behavior | Test Method | File |
|----------|------------|------|
| Explicit EF wins over config spec | `testExplicitEFWinsOverConfigSpec` | EmbeddingPrecedenceTest |
| Config spec used when no explicit EF | `testConfigSpecUsedWhenNoExplicitEF` | EmbeddingPrecedenceTest |
| WARNING logged when explicit overrides spec | `testWarningLoggedWhenExplicitOverridesSpec` | EmbeddingPrecedenceTest |
| FINE log when auto-wiring from spec | `testAutoWireLoggedAtFineLevel` | EmbeddingPrecedenceTest |
| No EF throws ChromaException with guidance | `testNoEFThrowsChromaException` | EmbeddingPrecedenceTest |

### EMB-04: ID Generator Correctness (38 tests)

| Behavior | Test Method(s) | File |
|----------|---------------|------|
| SHA-256 known hash determinism | `testSha256KnownHash`, `testSha256Deterministic` | IdGeneratorTest |
| SHA-256 different docs produce different IDs | `testSha256DifferentDocsDifferentIds` | IdGeneratorTest |
| SHA-256 hex format (64 chars) | `testSha256HexLength` | IdGeneratorTest |
| SHA-256 both-null throws IllegalArgumentException | `testSha256ThrowsOnNullDocument`, `testSha256BothNullThrowsIllegalArgument` | IdGeneratorTest |
| SHA-256 metadata fallback when doc is null | `testSha256MetadataFallbackWhenDocumentNull` | IdGeneratorTest |
| SHA-256 metadata fallback is deterministic | `testSha256MetadataFallbackDeterministic` | IdGeneratorTest |
| SHA-256 different metadata produces different IDs | `testSha256MetadataFallbackDifferentMetaDifferentIds` | IdGeneratorTest |
| SHA-256 metadata sort order independence | `testSha256MetadataFallbackSortOrder` | IdGeneratorTest |
| SHA-256 document takes precedence over metadata | `testSha256DocumentTakesPrecedenceOverMetadata` | IdGeneratorTest |
| SHA-256 empty metadata fallback valid | `testSha256EmptyMetadataFallback` | IdGeneratorTest |
| serializeMetadata deterministic sorted output | `testSerializeMetadataDeterministic` | IdGeneratorTest |
| serializeMetadata null values | `testSerializeMetadataNullValues` | IdGeneratorTest |
| serializeMetadata empty/null maps | `testSerializeMetadataEmpty`, `testSerializeMetadataNull` | IdGeneratorTest |
| UUID format and uniqueness | `testUuidGeneratesValidFormat`, `testUuidUniqueness` | IdGeneratorTest |
| ULID format, length, and sort order | `testUlidLength`, `testUlidValidCrockfordCharacters`, `testUlidLexicographicSortOrder` | IdGeneratorTest |
| Duplicate explicit IDs in add fail fast | `testAddWithDuplicateExplicitIdsFails` | ChromaHttpCollectionTest |
| Duplicate explicit IDs in upsert fail fast | `testUpsertWithDuplicateExplicitIdsFails` | ChromaHttpCollectionTest |
| Null generator output throws ChromaException | `testAddWithNullGeneratorOutputFailsWithChromaException` | ChromaHttpCollectionTest |
| Generator exception wrapped in ChromaException | `testAddWithGeneratorExceptionFailsWithChromaException` | ChromaHttpCollectionTest |

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated (2026-03-20)

---

## Validation Run Evidence

Validated 2026-03-20 by Nyquist auditor. All commands executed, all tests green.

| Task ID | Command | Tests Run | Failures | Errors | Skipped | Result |
|---------|---------|-----------|----------|--------|---------|--------|
| 03-01-01 | `mvn test -Dtest=OpenAIConformanceTest,CohereConformanceTest,HuggingFaceConformanceTest,OllamaConformanceTest` | 24 | 0 | 0 | 0 | PASS |
| 03-01-02 | `mvn test -Dtest=EmbeddingPrecedenceTest` | 5 | 0 | 0 | 0 | PASS |
| 03-02-01 | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | 7 | 0 | 0 | 0 | PASS |
| 03-02-02 | `mvn test -Dtest=EmbeddingFunctionCompatibilityTest` | 2 | 0 | 0 | 0 | PASS |
| 03-03-01 | `mvn test -Dtest=IdGeneratorTest` | 34 | 0 | 0 | 0 | PASS |
| 03-03-02 | `mvn test -Dtest="ChromaHttpCollectionTest#testAddWithDuplicateExplicitIdsFails+testUpsertWithDuplicateExplicitIdsFails+testAddWithNullGeneratorOutputFailsWithChromaException+testAddWithGeneratorExceptionFailsWithChromaException"` | 4 | 0 | 0 | 0 | PASS |
| **Total** | | **76** | **0** | **0** | **0** | **PASS** |
