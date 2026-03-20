---
phase: 03-embeddings-id-extensibility
plan: 03
subsystem: api
tags: [sha256, id-generator, validation, chromaexception, java8]

# Dependency graph
requires:
  - phase: 03-embeddings-id-extensibility
    provides: IdGenerator interface, Sha256IdGenerator, ChromaHttpCollection add/upsert builders

provides:
  - Sha256IdGenerator metadata fallback: hashes sorted metadata when document is null
  - Sha256IdGenerator both-null guard with clear error message
  - serializeMetadata() static package-private method for deterministic key=value;key=value serialization via TreeMap
  - checkForDuplicateIds() for client-side duplicate detection of explicit ID lists before HTTP call
  - ChromaException (not IllegalArgumentException) for generateIds() null/blank output and generator exceptions
  - Duplicate ID detection in AddBuilderImpl.execute() and UpsertBuilderImpl.execute()

affects: [03-embeddings-id-extensibility, future plans using IdGenerator or add/upsert builders]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TreeMap used for deterministic metadata serialization: sorted key=value pairs joined by semicolons"
    - "Client-side duplicate ID detection using LinkedHashMap to preserve insertion order for error messages"
    - "ChromaException as boundary exception for all ID generator failures (null, blank, runtime exceptions)"

key-files:
  created: []
  modified:
    - src/main/java/tech/amikos/chromadb/v2/Sha256IdGenerator.java
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java
    - src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java
    - src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java

key-decisions:
  - "Sha256IdGenerator throws IllegalArgumentException when BOTH document AND metadata are null (not when just one is null)"
  - "Empty metadata map is valid for Sha256IdGenerator: serializes to empty string, hashes that"
  - "ChromaException is thrown (not IllegalArgumentException) for all IdGenerator boundary failures per EMB-04 plan"
  - "checkForDuplicateIds() is only called for explicit ID lists, not generator-produced IDs (generators have their own duplicate detection)"
  - "serializeMetadata() is package-private (not private) to enable direct testability"

patterns-established:
  - "serializeMetadata: sorted TreeMap keys, key=value;key=value format, null values as literal 'null' string"
  - "Boundary exception upgrade: IllegalArgumentException -> ChromaException at public API boundaries for IdGenerator failures"

requirements-completed: [EMB-04]

# Metrics
duration: 28min
completed: 2026-03-19
---

# Phase 03 Plan 03: ID Generator Validation Edges Summary

**Sha256IdGenerator metadata fallback with deterministic TreeMap serialization, plus ChromaException for generator failures and client-side duplicate ID detection in add/upsert builders**

## Performance

- **Duration:** ~28 min
- **Started:** 2026-03-19T11:40:00Z
- **Completed:** 2026-03-19T12:08:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Sha256IdGenerator now supports metadata-only records (embeddings without documents) by hashing serialized metadata when document is null
- serializeMetadata() uses TreeMap for deterministic key-sorted serialization (key=value;key=value format) enabling content-addressable deduplication on metadata
- Explicit ID lists in add/upsert are checked for duplicates client-side via checkForDuplicateIds() before any HTTP call
- All IdGenerator failures (null/blank output, runtime exceptions, duplicate generator output) now throw ChromaException with record index instead of IllegalArgumentException

## Task Commits

1. **Task 1: Extend Sha256IdGenerator with metadata fallback and both-null guard** - `fcca865` (feat, TDD)
2. **Task 2: Add duplicate ID detection for explicit lists and ChromaException for generator failures** - `c79d1c7` (feat, TDD)

## Files Created/Modified

- `src/main/java/tech/amikos/chromadb/v2/Sha256IdGenerator.java` - Added serializeMetadata() and metadata fallback in generate()
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - Added checkForDuplicateIds(), updated generateIds() to throw ChromaException, added duplicate check calls in AddBuilderImpl and UpsertBuilderImpl
- `src/test/java/tech/amikos/chromadb/v2/IdGeneratorTest.java` - Added 11 new tests for metadata fallback, sort order, serializeMetadata, and both-null guard
- `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` - Added 4 new tests for duplicate IDs and generator failures; updated 5 existing tests from IllegalArgumentException to ChromaException

## Decisions Made

- Sha256IdGenerator throws when BOTH document AND metadata are null; empty metadata map is valid (serializes to empty string)
- ChromaException is the boundary exception for all IdGenerator failures, consistent with EMB-04 requirement for typed exceptions
- checkForDuplicateIds() only guards explicit ID lists; generateIds() handles duplicate detection for generator-produced IDs separately
- serializeMetadata() is package-private for direct testability without reflection

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated 5 existing tests that expected IllegalArgumentException from generateIds()**
- **Found during:** Task 2 (AddBuilderImpl/UpsertBuilderImpl duplicate detection and ChromaException upgrade)
- **Issue:** Existing tests `testAddIdGeneratorRejectsDuplicateGeneratedIds`, `testAddIdGeneratorWrapsGeneratorExceptionWithRecordIndex`, `testAddIdGeneratorRejectsNullGeneratedId`, `testAddIdGeneratorRejectsEmptyGeneratedId`, and `testAddWithSha256IdGeneratorEmbeddingsOnlyFailsWithDocumentRequirement` expected `IllegalArgumentException` but the plan requires `ChromaException` to be thrown from generateIds(). Also the Sha256 test message needed updating from "requires a non-null document" to "requires a non-null document or metadata".
- **Fix:** Updated all 5 tests to catch `ChromaException` and adjusted message assertions to match new behavior
- **Files modified:** src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java
- **Verification:** All 159 tests pass in ChromaHttpCollectionTest + IdGeneratorTest combined run
- **Committed in:** c79d1c7 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Bug: pre-existing tests had wrong expected exception type)
**Impact on plan:** Fix was necessary for correctness and consistency with the plan's ChromaException decision. No scope creep.

## Issues Encountered

None - implementation proceeded as specified in the plan.

## Next Phase Readiness

- EMB-04 is fully satisfied: Sha256IdGenerator handles metadata-only records, explicit duplicate IDs fail fast, generator failures throw ChromaException with record index
- All 159 ChromaHttpCollectionTest + 34 IdGeneratorTest pass (193 total)
- Ready for any downstream plans that depend on robust client-side ID validation

---
*Phase: 03-embeddings-id-extensibility*
*Completed: 2026-03-19*
