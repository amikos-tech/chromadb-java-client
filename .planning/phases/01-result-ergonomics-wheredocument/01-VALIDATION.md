---
phase: 1
slug: result-ergonomics-wheredocument
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-20
audited: 2026-03-21
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + TestContainers |
| **Config file** | `pom.xml` (surefire plugin) |
| **Quick run command** | `mvn test -Dtest=WhereDocumentTest,ResultRowTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~2 seconds (unit only) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=WhereDocumentTest,ResultRowTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 2 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| T1 | 01 | 1 | ERGO-01 | unit | `mvn test -Dtest=ResultRowTest` | ✅ | ✅ green |
| T1 | 02 | 2 | ERGO-01 | unit | `mvn test -Dtest=ResultRowTest` | ✅ | ✅ green |
| T2 | 02 | 2 | ERGO-01 | integration | `mvn test -Dtest=RecordOperationsIntegrationTest#testRowAccessOnGetResult+testRowAccessOnQueryResult` | ✅ | ✅ green |
| T1 | 03 | 1 | ERGO-02 | unit | `mvn test -Dtest=WhereDocumentTest` | ✅ | ✅ green |
| T2 | 03 | 1 | ERGO-02 | integration | `mvn test -Dtest=RecordOperationsIntegrationTest#testWhereDocumentContainsFilterOnGet+testWhereDocumentNotContainsFilterOnGet+testWhereDocumentOnQuery` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Test Coverage Summary

| Test File | Count | Coverage |
|-----------|-------|----------|
| `ResultRowTest.java` | 43 | ResultRow/QueryResultRow/ResultGroup types, wiring into GetResult/QueryResult, caching, equals/hashCode/toString |
| `WhereDocumentTest.java` | 26 | All 6 operators (contains, notContains, regex, notRegex, and, or), serialization, validation, edge cases |
| `RecordOperationsIntegrationTest.java` | 7 | Row access on get/query results (2), WhereDocument filters on get/query (5) |

**Total automated tests: 76** (69 unit + 7 integration)

---

## Wave 0 Requirements

- [x] `src/test/java/tech/amikos/chromadb/v2/ResultRowTest.java` — 43 unit tests for ResultRow/ResultGroup types and wiring
- [x] `src/test/java/tech/amikos/chromadb/v2/WhereDocumentTest.java` — 26 unit tests for WhereDocument operators

*Note: ResultGroup tests are included in ResultRowTest.java (same package for package-private access). No separate ResultGroupTest.java needed.*

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s (measured: ~2s for unit tests)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** complete

---

## Validation Audit 2026-03-21

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Requirements covered | 2/2 (ERGO-01, ERGO-02) |
| Unit tests verified | 69 (43 + 26) |
| Integration tests verified | 7 |
