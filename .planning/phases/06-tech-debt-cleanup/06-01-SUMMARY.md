---
phase: 06-tech-debt-cleanup
plan: 01
subsystem: testing
tags: [junit4, readme, hf-embedding, sha256-id-generator, integration-tests]

# Dependency graph
requires:
  - phase: 05-documentation-release-readiness
    provides: README.md rewrite, release.yml workflow
  - phase: 03-embeddings-id-extensibility
    provides: Sha256IdGenerator metadata fallback behavior, HuggingFaceEmbeddingFunction WithParam constructors
  - phase: 04-compatibility-test-matrix
    provides: AbstractChromaIntegrationTest with assumeMinVersion() helper

provides:
  - Corrected README HuggingFace examples (v2 and v1 legacy) using WithParam.apiKey(apiKey)
  - Corrected README Sha256IdGenerator description with metadata fallback clause
  - assumeMinVersion() wired to a concrete caller (ClientLifecycleIntegrationTest)
  - Phase06TechDebtValidationTest with six audit item validations

affects:
  - phase-06-plan-02 (INFRA-1, INFRA-2, ND4J-BUMP fixes build on this scaffold)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Nyquist validation tests in tech.amikos.chromadb package (non-v2 subpackage) for project-level static checks"
    - "Files.newDirectoryStream for walking integration test directory to find assumeMinVersion callers"

key-files:
  created:
    - src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java
  modified:
    - README.md
    - src/test/java/tech/amikos/chromadb/v2/ClientLifecycleIntegrationTest.java

key-decisions:
  - "v1 legacy HuggingFace example updated to WithParam.apiKey() because the bare-String constructor no longer exists — even legacy examples must reference the available constructor signature"
  - "testAssumeMinVersionSmokeTest uses assumeMinVersion(1.0.0) which always passes on all three matrix versions, making it a safe wiring proof"

patterns-established:
  - "Phase06TechDebtValidationTest: @BeforeClass loads README, release.yml, pom.xml; uses findProjectRoot() walk to locate files"

requirements-completed: [DOC-BUG-1, DOC-BUG-2, ASSUME-WIRE]

# Metrics
duration: 5min
completed: 2026-03-20
---

# Phase 06 Plan 01: Tech Debt Cleanup — Docs and assumeMinVersion Wiring Summary

**Three README documentation bugs fixed and assumeMinVersion() wired to a concrete caller via Phase06TechDebtValidationTest scaffold with six audit item validations**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-03-20T14:55:06Z
- **Completed:** 2026-03-20T15:00:00Z
- **Tasks:** 2
- **Files modified:** 3 (README.md, ClientLifecycleIntegrationTest.java, Phase06TechDebtValidationTest.java)

## Accomplishments

- Created Phase06TechDebtValidationTest with six Nyquist validation methods covering all Phase 6 audit items
- Fixed v2 README HuggingFace example to use `WithParam.apiKey(apiKey)` instead of nonexistent bare-String constructor
- Fixed v1 legacy README HuggingFace example to use `WithParam.apiKey(apiKey)` — the only available constructor
- Updated Sha256IdGenerator description to reflect Phase 3 metadata fallback behavior (both document and metadata null check)
- Wired `assumeMinVersion()` to a concrete caller in `ClientLifecycleIntegrationTest.testAssumeMinVersionSmokeTest`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Phase06TechDebtValidationTest scaffold** - `45a9d2d` (test)
2. **Task 2: Fix README doc bugs and wire assumeMinVersion** - `d87263a` (fix)

**Plan metadata:** TBD (docs: complete plan)

## Files Created/Modified

- `src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java` - Six Nyquist validation tests for Phase 6 audit items
- `README.md` - Fixed HF constructor in v2 section (line 378) and v1 section (line 639); updated Sha256IdGenerator description (line 308)
- `src/test/java/tech/amikos/chromadb/v2/ClientLifecycleIntegrationTest.java` - Added testAssumeMinVersionSmokeTest wiring proof

## Decisions Made

- v1 legacy HuggingFace example updated to `WithParam.apiKey()` because the bare-String constructor no longer exists — even legacy examples must reference the available constructor signature
- `testAssumeMinVersionSmokeTest` uses `assumeMinVersion("1.0.0")` which always passes on all three matrix versions (1.0.0, 1.3.7, 1.5.5), making it a safe wiring proof with no false skips

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 6 Plan 1 complete; three DOC/ASSUME audit items resolved and validated GREEN
- Plan 02 (INFRA-1, INFRA-2, ND4J-BUMP) can now use Phase06TechDebtValidationTest scaffold for its verifications
- Tests 4-6 in Phase06TechDebtValidationTest (release.yml and nd4j) will fail RED until Plan 02 applies its fixes

---
*Phase: 06-tech-debt-cleanup*
*Completed: 2026-03-20*

## Self-Check: PASSED

- FOUND: src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java
- FOUND: .planning/phases/06-tech-debt-cleanup/06-01-SUMMARY.md
- FOUND: commit 45a9d2d (test scaffold)
- FOUND: commit d87263a (README fixes + assumeMinVersion wiring)
- FOUND: commit 10e8419 (plan metadata)
