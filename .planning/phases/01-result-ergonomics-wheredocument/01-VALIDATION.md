---
phase: 1
slug: result-ergonomics-wheredocument
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + TestContainers |
| **Config file** | `pom.xml` (surefire plugin) |
| **Quick run command** | `mvn test -Dtest=WhereDocumentTest,ResultRowTest,ResultGroupTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=WhereDocumentTest,ResultRowTest,ResultGroupTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | ERGO-01 | unit | `mvn test -Dtest=ResultRowTest,ResultGroupTest` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | ERGO-02 | unit | `mvn test -Dtest=WhereDocumentTest` | ✅ | ⬜ pending |
| TBD | TBD | TBD | ERGO-01 | integration | `mvn test -Dtest=RecordOperationsIntegrationTest` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/v2/ResultRowTest.java` — stubs for ResultRow/ResultGroup unit tests
- [ ] `src/test/java/tech/amikos/chromadb/v2/ResultGroupTest.java` — stubs for ResultGroup unit tests

*Existing WhereDocumentTest.java and RecordOperationsIntegrationTest.java already exist.*

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
