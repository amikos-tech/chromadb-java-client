---
phase: 04-compatibility-test-matrix
plan: 01
subsystem: testing
tags: [junit4, testcontainers, github-actions, makefile, ci, version-matrix]

# Dependency graph
requires: []
provides:
  - assumeMinVersion() helper for version-conditional test skips
  - compareVersions() utility for dotted-version string comparison
  - Fail-fast container startup (AssertionError instead of silent skip)
  - CHROMA_MATRIX_VERSIONS Makefile variable with 3 pinned versions
  - make test-matrix target for local reproducible multi-version testing
  - GitHub Actions 5-cell integration test matrix (JDK 8 x 3 Chroma + JDK 11/17 x 1.5.5)
affects: [04-02-compatibility-guardrails]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - JUnit 4 Assume.assumeTrue for version-conditional skips (skip vs fail distinction)
    - GitHub Actions matrix include pattern for sparse JDK x Chroma cross-product
    - Makefile for-loop with set -e and || exit for sequential version testing with fail-fast

key-files:
  created: []
  modified:
    - src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java
    - Makefile
    - .github/workflows/integration-test.yml

key-decisions:
  - "Container startup failure throws AssertionError (fail-fast) not Assume.assumeTrue (silent skip) to make bad images visible"
  - "CHROMA_MATRIX_VERSIONS maintained in Makefile; CI workflow maintains its own matrix definition (both in sync at 1.0.0 1.3.7 1.5.5)"
  - "JDK 11 and 17 test only latest Chroma version (1.5.5) via GitHub Actions matrix include; JDK 8 covers all 3 versions"
  - "Unit tests run as separate CI job (no Docker) across all 3 JDKs; integration tests are the matrix job"
  - "Upgraded CI actions: checkout v3->v4, setup-java v3->v4, adopt->temurin"

patterns-established:
  - "Version-conditional skip pattern: call assumeMinVersion() in @Before or at test start; test is marked skipped not failed"
  - "compareVersions() is package-private static (not private) to allow direct unit testing"

requirements-completed: [QLTY-01]

# Metrics
duration: 15min
completed: 2026-03-20
---

# Phase 4 Plan 01: Version Matrix and CI Infrastructure Summary

**Multi-version test matrix via assumeMinVersion() skip helper, Makefile test-matrix target (3 Chroma versions), and 5-cell GitHub Actions integration test matrix with temurin/v4 actions**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-03-20T08:15:00Z
- **Completed:** 2026-03-20T08:30:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Added `assumeMinVersion()` and `compareVersions()` to `AbstractChromaIntegrationTest` enabling version-conditional test skips without test duplication
- Converted container startup failures from silent `Assume.assumeTrue` skip to `AssertionError` fail-fast so bad Docker images cause visible failures
- Added `CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5` and `make test-matrix` target for local reproducible multi-version testing
- Replaced single monolithic CI job with 3-job matrix workflow: unit-tests (3 JDKs), integration-tests (5 cells), cloud parity

## Task Commits

Each task was committed atomically:

1. **Task 1: Version-skip infrastructure and fail-fast container startup** - `ff10b15` (feat)
2. **Task 2: CHROMA_MATRIX_VERSIONS variable and test-matrix target** - `62c9f1c` (feat)
3. **Task 3: Replace CI workflow with matrix-parameterized jobs** - `83de447` (feat)

## Files Created/Modified
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` - Added assumeMinVersion(), compareVersions(), fail-fast startup; updated DEFAULT_CHROMA_VERSION to 1.5.5
- `Makefile` - Replaced empty CHROMA_VERSIONS with CHROMA_MATRIX_VERSIONS; added test-matrix target with sequential loop and fail-fast
- `.github/workflows/integration-test.yml` - Split into 3 jobs: unit-tests (JDK 8/11/17), integration-tests (5-cell matrix), cloud parity; upgraded to temurin/v4 actions

## Decisions Made
- Container startup failures throw `AssertionError` instead of using `Assume.assumeTrue` — bad image tags must be visible failures, not silent skips
- GitHub Actions `include` pattern (not exclude) for JDK 11/17 cells — produces exactly 5 cells with no risk of accidentally running excluded combinations
- `fail-fast: false` on both matrix jobs so all cells run to completion even if one fails (full picture of breakage)
- Upgraded `actions/checkout` and `actions/setup-java` from v3 to v4 as part of the rewrite
- `distribution: temurin` replaces deprecated `adopt`

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Version skip infrastructure is in place; individual tests can call `assumeMinVersion("1.3.0")` to skip against older Chroma versions
- CI matrix gates all PRs across 5 cells — no cells are advisory-only
- Plan 04-02 (API compatibility guardrails: animal-sniffer + PublicInterfaceCompatibilityTest) can proceed immediately

---
*Phase: 04-compatibility-test-matrix*
*Completed: 2026-03-20*
