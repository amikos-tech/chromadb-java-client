---
phase: 5
slug: cloud-integration-testing
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (existing) |
| **Config file** | `pom.xml` — surefire plugin with `integration` profile |
| **Quick run command** | `mvn test -Dtest=SearchApiCloudIntegrationTest` |
| **Full suite command** | `mvn test -Dtest="*CloudIntegrationTest,*CloudTest"` |
| **Estimated runtime** | ~60 seconds (cloud latency dependent) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=SearchApiCloudIntegrationTest`
- **After every plan wave:** Run `mvn test -Dtest="*CloudIntegrationTest,*CloudTest"`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-01 | 01 | 1 | CLOUD-02/03 | skeleton | `mvn test-compile` | ❌ W0 | ⬜ pending |
| 5-01-02 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudDistanceSpace*` | ❌ W0 | ⬜ pending |
| 5-01-03 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudHnswConfig*` | ❌ W0 | ⬜ pending |
| 5-01-04 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudSchemaRoundTrip*` | ❌ W0 | ⬜ pending |
| 5-01-05 | 01 | 1 | CLOUD-03 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudStringArray*` | ❌ W0 | ⬜ pending |
| 5-01-06 | 01 | 1 | CLOUD-03 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudArrayContains*` | ❌ W0 | ⬜ pending |
| 5-01-07 | 01 | 1 | CLOUD-03 | unit | `mvn test -Dtest=MetadataValidationTest` | ❌ W0 | ⬜ pending |
| 5-01-08 | 01 | 1 | CLOUD-03 | unit+wiring | `mvn test -Dtest=MetadataValidationTest#testAddExecute*` | ❌ W0 | ⬜ pending |
| 5-02-01 | 02 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudKnnSearch*` | ❌ W0 | ⬜ pending |
| 5-02-02 | 02 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudRrfSearch*` | ❌ W0 | ⬜ pending |
| 5-02-03 | 02 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudGroupBy*` | ❌ W0 | ⬜ pending |
| 5-02-04 | 02 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudBatchSearch*` | ❌ W0 | ⬜ pending |
| 5-02-05 | 02 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testCloudSearchFilter*` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Plan-to-Requirement Mapping

| Plan | Requirements | Description |
|------|-------------|-------------|
| 01 | CLOUD-02, CLOUD-03 | Schema/index parity tests, array metadata tests, mixed-type validation |
| 02 | CLOUD-01 | Search parity cloud tests (BLOCKED on Phase 3) |

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiCloudIntegrationTest.java` — test class skeleton with credential loading, seed data setup, cleanup
- [ ] Verify `.env` credential loading works with existing `Utils.loadEnvFile(".env")` pattern

*Existing JUnit 4 + surefire infrastructure covers all framework needs.*

---

## Phase 3 Dependency Gate (Plan 02)

Plan 02 (CLOUD-01) is **BLOCKED** pending Phase 3 (Search API) completion. Phase 3 has 0 plans executed. The Search API types (`Search`, `Knn`, `Rrf`, `GroupBy`, `ReadLevel`, `SearchResult`) do not exist yet.

**Pre-execution check for Plan 02:**
```bash
grep -r "class Search\|interface Search\|SearchResult\|SearchBuilder\|ReadLevel\|class Knn\|class Rrf\|class GroupBy" src/main/java/tech/amikos/chromadb/v2/
```
If no results: STOP. Do not execute Plan 02.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CI secrets propagation | CLOUD-01/02/03 | Requires GitHub Actions secrets config | Verify `CHROMA_API_KEY`, `CHROMA_TENANT`, `CHROMA_DATABASE` are set in CI environment |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
