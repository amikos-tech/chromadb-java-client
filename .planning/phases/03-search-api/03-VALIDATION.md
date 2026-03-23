---
phase: 3
slug: search-api
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (already in project) |
| **Config file** | none — Maven Surefire picks up `**/*Test.java` |
| **Quick run command** | `mvn test -Dtest=SearchApiUnitTest,SelectTest,SparseVectorTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=SearchApiUnitTest,SelectTest,SparseVectorTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | SEARCH-01 | unit | `mvn test -Dtest=SearchApiUnitTest#testKnnQueryText` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | SEARCH-01 | unit | `mvn test -Dtest=SearchApiUnitTest#testKnnQueryEmbedding` | ❌ W0 | ⬜ pending |
| 03-01-03 | 01 | 1 | SEARCH-01 | unit | `mvn test -Dtest=SparseVectorTest` | ❌ W0 | ⬜ pending |
| 03-01-04 | 01 | 1 | SEARCH-01 | integration | `mvn test -Dtest=SearchApiIntegrationTest#testKnnSearch` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | SEARCH-02 | unit | `mvn test -Dtest=SearchApiUnitTest#testRrfDtoStructure` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | SEARCH-02 | integration | `mvn test -Dtest=SearchApiIntegrationTest#testRrfSearch` | ❌ W0 | ⬜ pending |
| 03-03-01 | 03 | 2 | SEARCH-03 | unit | `mvn test -Dtest=SelectTest` | ❌ W0 | ⬜ pending |
| 03-03-02 | 03 | 2 | SEARCH-03 | integration | `mvn test -Dtest=SearchApiIntegrationTest#testSelectProjection` | ❌ W0 | ⬜ pending |
| 03-04-01 | 04 | 3 | SEARCH-04 | unit | `mvn test -Dtest=SearchApiUnitTest#testGroupByDto` | ❌ W0 | ⬜ pending |
| 03-04-02 | 04 | 3 | SEARCH-04 | unit | `mvn test -Dtest=SearchApiUnitTest#testReadLevelWireValues` | ❌ W0 | ⬜ pending |
| 03-04-03 | 04 | 3 | SEARCH-04 | integration | `mvn test -Dtest=SearchApiIntegrationTest#testReadLevel` | ❌ W0 | ⬜ pending |
| 03-04-04 | 04 | 3 | SEARCH-04 | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | ✅ (needs update) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiUnitTest.java` — covers SEARCH-01 through SEARCH-04 unit behaviors (DTO structure, wire format, serialization)
- [ ] `src/test/java/tech/amikos/chromadb/v2/SelectTest.java` — covers Select constants and key() projection
- [ ] `src/test/java/tech/amikos/chromadb/v2/SparseVectorTest.java` — covers SparseVector immutability and validation
- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiIntegrationTest.java` — integration tests against Chroma >= 1.5 via TestContainers

*Existing infrastructure covers framework and test config. Wave 0 creates test files only.*

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
