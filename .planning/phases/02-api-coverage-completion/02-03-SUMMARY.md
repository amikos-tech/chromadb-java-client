---
phase: 02-api-coverage-completion
plan: 03
subsystem: api
tags: [chroma, query, parity, integration-tests, makefile]
requires:
  - phase: 02-01
    provides: create-only lifecycle fallback and response-authoritative mapping
  - phase: 02-02
    provides: schema/config passthrough and mapping parity groundwork
provides:
  - mixed query input parity (`queryTexts` + `queryEmbeddings`) with embeddings-authoritative execution
  - query include forwarding contract coverage for omitted vs explicit include behavior
  - reproducible phase 2 parity matrix command for Chroma 1.5.5 and 1.3.7
affects: [api-03, record-operations, parity-validation]
tech-stack:
  added: []
  patterns: [execute-time precedence for mixed query inputs, makefile parity matrix gate]
key-files:
  created: []
  modified:
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java
    - src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java
    - src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java
    - Makefile
key-decisions:
  - "Allow mixed queryTexts/queryEmbeddings setters in either order and resolve precedence only at execute-time."
  - "Keep include serialization server-authoritative by sending include only when explicitly set by caller."
patterns-established:
  - "Mixed input query pattern: explicit embeddings override text embedding without requiring runtime embedding function."
  - "Phase parity verification pattern: one Makefile target runs unit contract checks plus pinned version integration matrix."
requirements-completed: [API-03]
duration: 8min
completed: 2026-03-18
---

# Phase 2 Plan 3: API-03 Query/Record Parity Summary

**Mixed query input parity now matches Python/Go semantics with embeddings-authoritative execution and a pinned two-version Phase 2 parity gate.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-18T18:50:10Z
- **Completed:** 2026-03-18T18:58:21Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Replaced mixed-input rejection tests with parity acceptance coverage for both setter orders and embeddings-authoritative results.
- Added WireMock request-contract tests proving omitted `include` does not force client defaults and explicit `include` is forwarded exactly.
- Added `test-phase-02-parity` target to run unit parity checks and integration matrix against Chroma `1.5.5` and `1.3.7`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add contract/integration tests for mixed query inputs and include forwarding** - `306b2ca` (test)
2. **Task 2: Implement embeddings-authoritative mixed-input query behavior** - `98af43d` (feat)
3. **Task 3: Add reproducible Phase 2 parity matrix command** - `184fe1b` (chore)

**Plan metadata:** pending

## Files Created/Modified
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` - mixed-input parity tests and include omitted/exact query payload contract assertions.
- `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` - integration test locking embeddings-authoritative mixed query behavior.
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - QueryBuilder mixed-input setter acceptance with execute-time precedence retained.
- `Makefile` - new `test-phase-02-parity` target with unit + Chroma `1.5.5`/`1.3.7` integration matrix commands.

## Decisions Made
- Mixed query inputs are accepted in both setter orders; explicit `queryEmbeddings` is authoritative and query text embedding is fallback-only.
- Query `include` remains server-authoritative when omitted and caller-authoritative when explicitly set.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- API-03 parity behavior is test-locked with both unit contract checks and integration coverage.
- Maintainers can run `make test-phase-02-parity` to reproduce the full phase-2 parity gate locally or in CI.

---
*Phase: 02-api-coverage-completion*
*Completed: 2026-03-18*

## Self-Check: PASSED
- FOUND: `.planning/phases/02-api-coverage-completion/02-03-SUMMARY.md`
- FOUND: `306b2ca`
- FOUND: `98af43d`
- FOUND: `184fe1b`
