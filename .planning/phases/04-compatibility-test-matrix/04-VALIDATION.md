---
phase: 4
slug: compatibility-test-matrix
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-20
validated: 2026-03-20
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 |
| **Config file** | pom.xml (Maven Surefire convention-based) |
| **Quick run command** | `mvn test -Dtest="CompareVersionsTest,Phase04ValidationTest,PublicInterfaceCompatibilityTest"` |
| **Full suite command** | `mvn test -Dtest="CompareVersionsTest,Phase04ValidationTest,PublicInterfaceCompatibilityTest"` |
| **Estimated runtime** | ~4 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest="CompareVersionsTest,Phase04ValidationTest,PublicInterfaceCompatibilityTest"`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 4 seconds (unit tests only, no Docker required)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File | Status |
|---------|------|------|-------------|-----------|-------------------|------|--------|
| 04-01-01 | 01 | 1 | QLTY-01 | unit | `mvn test -Dtest=CompareVersionsTest` | CompareVersionsTest.java | green |
| 04-01-02 | 01 | 1 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testMakefileHasChromaMatrixVersionsWithThreePinnedVersions` | Phase04ValidationTest.java | green |
| 04-01-03 | 01 | 1 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testMakefileTestMatrixTargetUsesFailFastLoop` | Phase04ValidationTest.java | green |
| 04-01-04 | 01 | 2 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testCiWorkflowHasThreeJobs` | Phase04ValidationTest.java | green |
| 04-01-05 | 01 | 2 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testCiWorkflowIntegrationMatrixHasThreeChromaVersions` | Phase04ValidationTest.java | green |
| 04-01-06 | 01 | 2 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testCiWorkflowHasNoFailFast` | Phase04ValidationTest.java | green |
| 04-01-07 | 01 | 2 | QLTY-01 | unit | `mvn test -Dtest=Phase04ValidationTest#testContainerStartupFailureThrowsAssertionErrorNotAssume` | Phase04ValidationTest.java | green |
| 04-02-01 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=Phase04ValidationTest#testAnimalSnifferPluginConfiguredInPom` | Phase04ValidationTest.java | green |
| 04-02-02 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=Phase04ValidationTest#testAnimalSnifferBoundToCompilePhase` | Phase04ValidationTest.java | green |
| 04-02-03 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | PublicInterfaceCompatibilityTest.java | green |
| 04-02-04 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=Phase04ValidationTest#testPublicInterfaceCompatTestCoversAllTenTypes` | Phase04ValidationTest.java | green |
| 04-02-05 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=Phase04ValidationTest#testClientInterfaceHasExpectedPublicMethods` | Phase04ValidationTest.java | green |
| 04-02-06 | 02 | 1 | QLTY-02 | unit | `mvn test -Dtest=Phase04ValidationTest#testCollectionInterfaceHasExpectedOperationBuilders` | Phase04ValidationTest.java | green |

*Status: green (all 80 tests pass: 51 + 19 + 10)*

---

## Wave 0 Requirements

- [x] `pom.xml` -- `animal-sniffer-maven-plugin` 1.27 configured with `<phase>compile</phase>`
- [x] `Makefile` -- `CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5` and `test-matrix` target present
- [x] `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` -- `assumeMinVersion()`, `compareVersions()`, fail-fast `AssertionError` all present
- [x] `.github/workflows/integration-test.yml` -- 3-job matrix structure with temurin/v4 actions
- [x] `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` -- 51 tests with count assertions for all 10 types

*All Wave 0 dependencies satisfied.*

---

## Nyquist Validation Tests Created

| # | File | Test Count | Type | Command | Requirement |
|---|------|-----------|------|---------|-------------|
| 1 | `src/test/java/tech/amikos/chromadb/v2/CompareVersionsTest.java` | 10 | unit | `mvn test -Dtest=CompareVersionsTest` | QLTY-01 |
| 2 | `src/test/java/tech/amikos/chromadb/v2/Phase04ValidationTest.java` | 19 | unit | `mvn test -Dtest=Phase04ValidationTest` | QLTY-01, QLTY-02 |
| 3 | `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` (existing) | 51 | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | QLTY-02 |

**Combined command:** `mvn test -Dtest="CompareVersionsTest,Phase04ValidationTest,PublicInterfaceCompatibilityTest"`
**Result:** 80 tests, 0 failures, 0 errors, 0 skipped (3.275s)

---

## Requirement Coverage Summary

| Requirement | Tests | Key Behaviors Validated |
|-------------|-------|------------------------|
| QLTY-01 | CompareVersionsTest (10) + Phase04ValidationTest (12) | compareVersions correctness, matrix version ordering, Makefile structure (CHROMA_MATRIX_VERSIONS, fail-fast loop, CHROMA_VERSION passthrough), CI workflow (3 jobs, 3 Chroma versions, fail-fast:false, temurin, v4 actions, JDK 11/17 include), container startup AssertionError, assumeMinVersion helper, DEFAULT_CHROMA_VERSION=1.5.5 |
| QLTY-02 | Phase04ValidationTest (7) + PublicInterfaceCompatibilityTest (51) | animal-sniffer in pom.xml (plugin, java18 signature, compile phase binding), 10 method-count constants present, getDeclaredMethods used, 10 original tests preserved, Client interface methods verified, Collection builder return types verified |

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `make test-matrix` end-to-end execution | QLTY-01 | Requires Docker + significant wall-clock time | Run `make test-matrix` with Docker available; expect 3 sequential Chroma container startups |
| GitHub Actions matrix runs 5 cells | QLTY-01 | Requires push to GitHub | Push PR branch, verify Actions tab shows 5 integration-test matrix cells |
| CI blocks merge on red cell | QLTY-01 | Requires branch protection | Verify PR merge button is blocked when any matrix cell fails |
| animal-sniffer rejects JDK 9+ API in `mvn compile` | QLTY-02 | Requires modifying source temporarily | Add `List.of("x")` to any main source method, run `mvn compile`, expect build failure |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s (measured: ~4s)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated (2026-03-20)
