---
phase: 4
slug: embedding-ecosystem
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-01
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Maven Surefire |
| **Config file** | `pom.xml` (surefire plugin already configured) |
| **Quick run command** | `mvn test -Dtest=TestClassName` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~120 seconds (with TestContainers) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=<relevant test class>`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | 01 | 1 | EMB-05, EMB-06 | unit | `mvn test -Dtest=TestSparseEmbeddingFunction,TestContentEmbeddingFunction` | ❌ W0 | ⬜ pending |
| TBD | 02 | 1 | RERANK-01 | unit | `mvn test -Dtest=TestCohereRerankingFunction,TestJinaRerankingFunction` | ❌ W0 | ⬜ pending |
| TBD | 03 | 1 | EMB-07 | unit | `mvn test -Dtest=TestGeminiEmbeddingFunction,TestBedrockEmbeddingFunction,TestVoyageEmbeddingFunction` | ❌ W0 | ⬜ pending |
| TBD | 04 | 2 | EMB-05 | unit | `mvn test -Dtest=TestBM25EmbeddingFunction,TestChromaCloudSpladeEmbeddingFunction` | ❌ W0 | ⬜ pending |
| TBD | 05 | 2 | EMB-08 | unit | `mvn test -Dtest=TestEmbeddingFunctionRegistry` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Test stubs for sparse/content interfaces — EMB-05, EMB-06
- [ ] Test stubs for new dense providers — EMB-07
- [ ] Test stubs for reranking — RERANK-01
- [ ] Test stubs for registry — EMB-08

*Existing test infrastructure (JUnit 5, Surefire, TestContainers) covers framework needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Gemini API integration | EMB-07 | Requires GOOGLE_API_KEY | Set env var, run `mvn test -Dtest=TestGeminiEmbeddingFunction` |
| Bedrock API integration | EMB-07 | Requires AWS credentials | Set env vars, run `mvn test -Dtest=TestBedrockEmbeddingFunction` |
| Voyage API integration | EMB-07 | Requires VOYAGE_API_KEY | Set env var, run `mvn test -Dtest=TestVoyageEmbeddingFunction` |
| Cohere Rerank integration | RERANK-01 | Requires COHERE_API_KEY | Set env var, run `mvn test -Dtest=TestCohereRerankFunction` |
| Jina Rerank integration | RERANK-01 | Requires JINA_API_KEY | Set env var, run `mvn test -Dtest=TestJinaRerankFunction` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
