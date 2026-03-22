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
| **Quick run command** | `mvn test -Dtest=SearchApiCloudIntegrationTest -pl .` |
| **Full suite command** | `mvn test -Dtest="*CloudIntegrationTest,*CloudTest" -pl .` |
| **Estimated runtime** | ~60 seconds (cloud latency dependent) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=SearchApiCloudIntegrationTest -pl .`
- **After every plan wave:** Run `mvn test -Dtest="*CloudIntegrationTest,*CloudTest" -pl .`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-01 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testDistanceSpace*` | ❌ W0 | ⬜ pending |
| 5-01-02 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testHnswConfig*` | ❌ W0 | ⬜ pending |
| 5-01-03 | 01 | 1 | CLOUD-02 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testInvalidConfig*` | ❌ W0 | ⬜ pending |
| 5-02-01 | 02 | 1 | CLOUD-03 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testArrayMetadata*` | ❌ W0 | ⬜ pending |
| 5-02-02 | 02 | 1 | CLOUD-03 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testContainsFilter*` | ❌ W0 | ⬜ pending |
| 5-02-03 | 02 | 1 | CLOUD-03 | unit | `mvn test -Dtest=SearchApiCloudIntegrationTest#testMixedTypeArray*` | ❌ W0 | ⬜ pending |
| 5-03-01 | 03 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testKnnSearch*` | ❌ W0 | ⬜ pending |
| 5-03-02 | 03 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testRrfSearch*` | ❌ W0 | ⬜ pending |
| 5-03-03 | 03 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testGroupBy*` | ❌ W0 | ⬜ pending |
| 5-03-04 | 03 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testBatchSearch*` | ❌ W0 | ⬜ pending |
| 5-03-05 | 03 | 2 | CLOUD-01 | integration | `mvn test -Dtest=SearchApiCloudIntegrationTest#testFilter*` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/v2/SearchApiCloudIntegrationTest.java` — test class skeleton with credential loading, seed data setup, cleanup
- [ ] Verify `.env` credential loading works with existing `Utils.loadEnvFile(".env")` pattern

*Existing JUnit 4 + surefire infrastructure covers all framework needs.*

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
