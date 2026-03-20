---
phase: 07-readme-embedding-examples
plan: 01
subsystem: documentation
tags: [readme, openai, cohere, huggingface, embeddings, WithParam]

# Dependency graph
requires:
  - phase: 06-tech-debt-cleanup
    provides: "WithParam-based HuggingFace constructor fix and v1 legacy example pattern"
provides:
  - "Fixed OpenAI embedding constructor (v2 + v1 appendix) using WithParam.apiKey + WithParam.model"
  - "Fixed Cohere embedding constructor (v2 + v1 appendix) using WithParam.apiKey [+ WithParam.model for v2]"
  - "WithParam import added to all 8 README embedding code blocks (v2: OpenAI, Cohere, HF, HFEI; v1: OpenAI, Cohere, HF, HFEI)"
  - "Phase07ReadmeEmbeddingValidationTest (6 Nyquist assertions preventing constructor regression)"
affects: [release, documentation, EMB-01, QLTY-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Nyquist validation test pattern: string-match README assertions to prevent documentation rot"
    - "WithParam factory methods as the only public API for embedding function constructor args"

key-files:
  created:
    - src/test/java/tech/amikos/chromadb/Phase07ReadmeEmbeddingValidationTest.java
  modified:
    - README.md

key-decisions:
  - "v1 Cohere stays minimal — apiKey only (WithParam.apiKey(apiKey)), no model param; consistent with locked Phase 07 context decision"
  - "8 WithParam import threshold (4 v2 + 4 v1) asserted in test to prevent future import drift"

patterns-established:
  - "All embedding function constructors in README use WithParam factory methods — no bare String args"
  - "Every code block that uses WithParam must also import tech.amikos.chromadb.embeddings.WithParam"

requirements-completed: [EMB-01, QLTY-03]

# Metrics
duration: 3min
completed: 2026-03-20
---

# Phase 07 Plan 01: README Embedding Examples Summary

**Fixed 4 broken embedding constructors (OpenAI v2/v1 + Cohere v2/v1) and added 8 WithParam imports across all README embedding code blocks, closing EMB-01 and QLTY-03**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T16:36:21Z
- **Completed:** 2026-03-20T16:39:30Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Created Phase07ReadmeEmbeddingValidationTest with 6 Nyquist test methods (RED then GREEN)
- Fixed v2 OpenAI constructor: `OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small")` -> `OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))`
- Fixed v2 Cohere constructor: `CohereEmbeddingFunction(apiKey)` -> `CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"))`
- Fixed v1 appendix OpenAI constructor to match v2 pattern
- Fixed v1 appendix Cohere constructor to WithParam.apiKey (apiKey only per locked decision)
- Added `import tech.amikos.chromadb.embeddings.WithParam;` to all 8 embedding code blocks (4 v2 + 4 v1)

## Task Commits

Each task was committed atomically:

1. **Task 1: Phase07ReadmeEmbeddingValidationTest scaffold** - `4562c0e` (test)
2. **Task 2: Fix README embedding constructors and imports** - `28adcee` (fix)

## Files Created/Modified

- `src/test/java/tech/amikos/chromadb/Phase07ReadmeEmbeddingValidationTest.java` - 6-method Nyquist validation for README embedding examples (constructor correctness + import completeness)
- `README.md` - Fixed 4 constructor calls and added 8 WithParam import lines across v2 and v1 appendix embedding examples

## Decisions Made

- v1 Cohere uses `WithParam.apiKey(apiKey)` only (no model param) — consistent with locked Phase 07 context decision; the v2 pattern includes WithParam.model but v1 stays minimal
- Test threshold for import count is 8 (4 v2 + 4 v1) so any future removal of an import will fail the test

## Deviations from Plan

None - plan executed exactly as written. All 12 edits applied to README as specified (edits 7-12 map to plan edits 7-12 one-to-one). Tests confirmed RED then GREEN.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All README embedding examples are copy-paste correct with proper constructors and imports
- EMB-01 and QLTY-03 requirements are closed
- v1.0 milestone audit items are fully resolved
- Project is release-ready: Phase 07 was the final gap-closure phase

---
*Phase: 07-readme-embedding-examples*
*Completed: 2026-03-20*
