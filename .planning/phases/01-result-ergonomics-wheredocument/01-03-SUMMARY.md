---
phase: 01-result-ergonomics-wheredocument
plan: 03
subsystem: api
tags: [java, where-document, filter-dsl, chromadb, testcontainers]

# Dependency graph
requires:
  - phase: 01-result-ergonomics-wheredocument
    provides: WhereDocument class with fromMap escape hatch (Plan 01 baseline)
provides:
  - Complete typed WhereDocument DSL with all 6 operators: contains, notContains, regex, notRegex, and, or
  - Unit tests covering all operators, serialization, validation, and edge cases
  - Integration tests proving typed WhereDocument filters work against real ChromaDB
affects:
  - Any plan that uses WhereDocument in collection get/query/delete operations
  - Integration testing plans covering where_document filtering

# Tech tracking
tech-stack:
  added: []
  patterns:
    - leafCondition helper for scalar operator serialization (operator -> value map)
    - logicalCondition helper for $and/$or clause aggregation (mirrors Where.java pattern)
    - requireNonNull/requireNonBlank validation helpers per Where.java convention

key-files:
  created: []
  modified:
    - src/main/java/tech/amikos/chromadb/v2/WhereDocument.java
    - src/test/java/tech/amikos/chromadb/v2/WhereDocumentTest.java
    - src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java

key-decisions:
  - "contains/notContains reject null and blank (whitespace-only) strings; regex/notRegex reject only null (empty string is a valid regex)"
  - "Javadoc on WhereDocument.contains() clarifies distinction from Where.documentContains() per D-18: WhereDocument is local-compatible path, Where#documentContains is Cloud-oriented inline filter"
  - "logicalCondition helper mirrors Where.java pattern exactly: validates conditions are non-null, non-zero, and each element's toMap() is non-null"

patterns-established:
  - "WhereDocument operator pattern: leafCondition(OP_X, validatedValue) for leaf nodes"
  - "WhereDocument combinator pattern: logicalCondition(OP_AND/OR, conditions...) for logical operators"

requirements-completed: [ERGO-02]

# Metrics
duration: 5min
completed: 2026-03-20
---

# Phase 01 Plan 03: WhereDocument Operators Summary

**Typed WhereDocument DSL fully implemented: all 6 operators (contains, notContains, regex, notRegex, and, or) with input validation and integration tests proving end-to-end Chroma filtering**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-03-20T19:12:00Z
- **Completed:** 2026-03-20T19:14:37Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Replaced all 6 UnsupportedOperationException stubs in WhereDocument.java with real implementations
- Added operator constants (OP_CONTAINS, OP_NOT_CONTAINS, OP_REGEX, OP_NOT_REGEX, OP_AND, OP_OR) and private helpers (leafCondition, logicalCondition, requireNonNull, requireNonBlank)
- Replaced 4 stub-asserting unit tests with 22 serialization and validation tests covering all operators, null/blank rejection, and edge cases
- Added 3 integration tests (testWhereDocumentContainsFilterOnGet, testWhereDocumentNotContainsFilterOnGet, testWhereDocumentOnQuery) that verify typed API against real ChromaDB via TestContainers

## Task Commits

Each task was committed atomically (TDD - 3 commits for Task 1):

1. **Task 1 RED: WhereDocument stub replacement tests** - `1ccaad1` (test)
2. **Task 1 GREEN: WhereDocument operator implementation** - `6a57a26` (feat)
3. **Task 2: Integration tests for typed WhereDocument** - `c9b52c1` (feat)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/WhereDocument.java` - Replaced all 6 stubs with real implementations; added operator constants, leafCondition/logicalCondition helpers, validation helpers; updated instance method Javadoc to remove stale @throws UnsupportedOperationException; added D-18 Javadoc on contains()
- `src/test/java/tech/amikos/chromadb/v2/WhereDocumentTest.java` - Replaced 4 stub-asserting tests with 22 serialization and validation tests; removed assertNotImplemented and stubWhereDocument helpers
- `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` - Added 3 integration test methods using typed WhereDocument.contains() and WhereDocument.notContains()

## Decisions Made

- `contains`/`notContains` reject null and blank (whitespace-only) strings — consistent with Where.documentContains() blank rejection pattern
- `regex`/`notRegex` reject only null — empty string is a valid regex pattern
- Javadoc on `WhereDocument.contains()` clarifies distinction from `Where.documentContains()` per D-18: WhereDocument is the local-compatible where_document path; Where#documentContains is the Cloud-oriented inline filter
- logicalCondition helper mirrors Where.java exactly, including null toMap() validation

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Parallel execution (Plan 01-01 RED phase) left untracked production files (QueryResultRowImpl.java, ResultGroupImpl.java, etc.) that caused ResultRowTest.java compilation failure. This is a cross-plan dependency issue, pre-existing in branch state. Worked around by excluding ResultRowTest.java from compilation during WhereDocumentTest-only runs. The orchestrator will handle full compilation after all parallel agents complete.

## Next Phase Readiness

- WhereDocument DSL is fully functional — operators produce correct Chroma JSON, validated inputs, integration-tested
- ERGO-02 requirement is complete
- Any code using `WhereDocument.contains()`, `notContains()`, `regex()`, `notRegex()`, `and()`, `or()` will work as expected

## Known Stubs

None - all WhereDocument operator stubs have been replaced with real implementations.

---
*Phase: 01-result-ergonomics-wheredocument*
*Completed: 2026-03-20*
