---
phase: 03
slug: embeddings-id-extensibility
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-19
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) |
| **Config file** | none — Maven Surefire discovers `*Test.java` |
| **Quick run command** | `mvn test -Dtest=IdGeneratorTest,ChromaHttpCollectionTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~30 seconds (unit only) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=IdGeneratorTest,ChromaHttpCollectionTest`
- **After every plan wave:** Run `mvn test` (unit suite)
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | EMB-01 | unit | `mvn test -Dtest=OpenAIConformanceTest,CohereConformanceTest,HuggingFaceConformanceTest,OllamaConformanceTest` | W0 | pending |
| 03-01-02 | 01 | 1 | EMB-03 | unit | `mvn test -Dtest=EmbeddingPrecedenceTest` | W0 | pending |
| 03-02-01 | 02 | 2 | EMB-02 | unit | `mvn test -Dtest=DefaultEmbeddingFunctionTest` | W0 | pending |
| 03-02-02 | 02 | 2 | EMB-02 | unit | `mvn test -Dtest=EmbeddingFunctionCompatibilityTest` | exists | pending |
| 03-03-01 | 03 | 2 | EMB-04 | unit | `mvn test -Dtest=IdGeneratorTest` | extend | pending |
| 03-03-02 | 03 | 2 | EMB-04 | unit | `mvn test -Dtest=ChromaHttpCollectionTest` | extend | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/embeddings/AbstractEmbeddingFunctionConformanceTest.java` — abstract conformance base for EMB-01
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/OpenAIConformanceTest.java` — WireMock stubs for OpenAI
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/CohereConformanceTest.java` — WireMock stubs for Cohere
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/HuggingFaceConformanceTest.java` — WireMock stubs for HuggingFace
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/OllamaConformanceTest.java` — WireMock stubs for Ollama
- [ ] `src/test/java/tech/amikos/chromadb/v2/EmbeddingPrecedenceTest.java` — EMB-03 precedence chain contract
- [ ] `src/test/java/tech/amikos/chromadb/embeddings/DefaultEmbeddingFunctionTest.java` — EMB-02 download reliability (WireMock)

Existing files to extend (no Wave 0 needed):
- `src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java` — SHA-256 metadata fallback + both-null
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` — duplicate explicit ID detection

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
