---
phase: 7
slug: readme-embedding-examples
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 |
| **Config file** | `pom.xml` (surefire plugin) |
| **Quick run command** | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~5 seconds (README string matching only) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest`
- **After every plan wave:** Run `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 0 | QLTY-03 | unit | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest` | ❌ W0 | ⬜ pending |
| 07-01-02 | 01 | 1 | EMB-01, QLTY-03 | unit | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/Phase07ReadmeEmbeddingValidationTest.java` — README content validation: verify OpenAI/Cohere constructors use WithParam pattern, verify WithParam imports present
- [ ] Pattern follows `Phase06TechDebtValidationTest` structure: `@BeforeClass` loads README via `findProjectRoot()`, `@Test` methods do `assertTrue`/`assertFalse` string matching

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
