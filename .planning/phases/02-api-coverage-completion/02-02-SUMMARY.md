---
phase: 02-api-coverage-completion
plan: 02
subsystem: api
tags: [dto-mapping, schema, cmek, serialization, junit]
requires: []
provides:
  - CollectionConfiguration/Schema passthrough APIs for unknown key retention
  - Non-lossy parse->serialize handling for unknown config/schema/CMEK keys
  - Contract tests for typed-authoritative merge and top-level schema precedence
affects: [api-coverage-completion, collection-mapping, dto-contracts]
tech-stack:
  added: []
  patterns:
    - Strict known-field parsing plus unknown-key passthrough preservation
    - Typed-authoritative serialization where typed keys override passthrough conflicts
key-files:
  created:
    - .planning/phases/02-api-coverage-completion/deferred-items.md
  modified:
    - src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java
    - src/main/java/tech/amikos/chromadb/v2/Schema.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java
    - src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java
    - src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java
key-decisions:
  - "Unknown config/schema payload is stored as immutable passthrough maps on value objects."
  - "Unsupported CMEK providers are preserved in schema passthrough and merged back during serialization."
  - "Top-level schema precedence remains canonical for collection mapping."
patterns-established:
  - "Parse known fields strictly, preserve unknown siblings in passthrough."
  - "Serialize typed fields first, then merge passthrough entries only for non-conflicting keys."
requirements-completed: [API-02]
duration: 10min
completed: 2026-03-18
---

# Phase 2 Plan 2: API-02 Schema/CMEK Parity Summary

**Passthrough-aware schema/config DTO mapping now preserves unknown keys and CMEK providers while keeping top-level schema canonical.**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-18T18:25:31Z
- **Completed:** 2026-03-18T18:34:34Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added API-02 contract tests covering unknown-key preservation, typed-authoritative conflict handling, and schema precedence.
- Implemented immutable passthrough support on `CollectionConfiguration` and `Schema`.
- Updated DTO parse/serialize logic to retain unknown config/schema/CMEK data without regressing known-field validation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add contract tests for unknown-key preservation and schema precedence** - `96339f8` (test)
2. **Task 2: Implement passthrough-aware schema/config models and DTO mapping** - `2d73093` (feat)

**Plan metadata:** pending (captured in docs commit for SUMMARY/STATE/ROADMAP updates)

## Files Created/Modified

- `src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java` - API-02 contract tests for `PreservesUnknown` and `TypedAuthoritative`.
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` - `PrefersTopLevelSchema` contract test with canonical-source passthrough assertions.
- `src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java` - added immutable configuration passthrough map + builder API.
- `src/main/java/tech/amikos/chromadb/v2/Schema.java` - added immutable schema passthrough map + builder API.
- `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` - passthrough extraction/merge logic and CMEK unknown-provider preservation.

## Decisions Made

- Unknown payload keys are modeled as first-class immutable passthrough maps to avoid lossy DTO round-trips.
- Serialization is deterministic and typed-authoritative: typed-known fields always win key conflicts.
- CMEK parsing remains strict for supported providers (`gcp`) while unsupported providers are preserved when safely serializable.

## Deviations from Plan

### Execution Variance

**1. Contract-first red->green sequencing across tasks**
- **Found during:** Task 1 and Task 2
- **Issue:** Task 1 verify command ended red by design after adding contract tests for unimplemented API-02 behavior.
- **Fix:** Task 2 implemented passthrough-aware mapping and re-ran verification to green.
- **Verification:** `mvn -q -Dtest=ChromaDtosContractTest,ChromaHttpCollectionTest test` and integration suite both exit 0.
- **Committed in:** `96339f8` and `2d73093`

---

**Total deviations:** 0 auto-fixed (execution sequencing variance only)
**Impact on plan:** No scope creep; behavior landed exactly within API-02 contract scope.

## Issues Encountered

- Unrelated local modifications appeared in `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java` during execution. Item was logged in `.planning/phases/02-api-coverage-completion/deferred-items.md` and left untouched.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- API-02 parity contract is enforced and verified by targeted unit + integration suites.
- Ready for remaining Phase 2 plans; unresolved unrelated workspace edits in `ChromaClient.java` remain outside this plan scope.

## Self-Check: PASSED

- Verified summary and deferred-item files exist.
- Verified task commits `96339f8` and `2d73093` exist in git history.

---
*Phase: 02-api-coverage-completion*
*Completed: 2026-03-18*
