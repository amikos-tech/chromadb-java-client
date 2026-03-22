---
phase: 05-cloud-integration-testing
plan: 01
subsystem: testing
tags: [cloud, integration-test, array-metadata, schema, hnsw, spann, junit4, chromadb]

# Dependency graph
requires:
  - phase: 02-collection-api-extensions
    provides: indexingStatus(), fork(), forkCount() on Collection interface
  - phase: 02-collection-api-extensions
    provides: CollectionConfiguration, UpdateCollectionConfiguration, Schema, DistanceFunction
  - phase: 01-result-ergonomics-wheredocument
    provides: Where.contains/notContains DSL for array metadata filters
provides:
  - Cloud integration test class SearchApiCloudIntegrationTest with 12 test methods
  - CLOUD-02 schema/index parity tests (distance space, HNSW, SPANN, schema round-trip)
  - CLOUD-03 array metadata tests (string/number/bool arrays, edge cases, empty arrays)
  - D-22 mixed-type array client-side validation in ChromaHttpCollection
  - MetadataValidationTest with 18 unit tests including 3 behavioral wiring tests
affects:
  - 05-02 (any follow-on cloud integration plans that extend this test class)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - validateMetadataArrayTypes static package-private method for client-side metadata validation
    - Behavioral wiring tests using stub ChromaHttpCollection (package-private from() + dead endpoint)
    - Shared @BeforeClass seed collection for read-only cloud tests, per-test isolated collections for mutating tests
    - waitForIndexing() polling helper using IndexingStatus.getOpIndexingProgress() >= 1.0 - 1e-6

key-files:
  created:
    - src/test/java/tech/amikos/chromadb/v2/SearchApiCloudIntegrationTest.java
    - src/test/java/tech/amikos/chromadb/v2/MetadataValidationTest.java
  modified:
    - src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java

key-decisions:
  - "validateMetadataArrayTypes uses ChromaBadRequestException with errorCode MIXED_TYPE_ARRAY or NULL_ARRAY_ELEMENT — consistent with existing exception hierarchy"
  - "Integer/Long/Short/Byte normalized to Integer group; Float/Double normalized to Float group for homogeneity checking — mixed int+long or float+double are valid"
  - "Behavioral wiring tests use ChromaHttpCollection.from() with a stub ChromaApiClient pointing at localhost:1 — validation fires before any network call so the dead endpoint is never reached"
  - "testCloudMixedTypeArrayRejected() calls validateMetadataArrayTypes directly (not through col.add()) — no cloud credential gate needed since it tests client-side only"
  - "Empty arrays pass validation — only non-empty arrays with mixed types are rejected"

patterns-established:
  - "validateMetadataArrayTypes is called at the VERY START of execute() in Add/Upsert/UpdateBuilderImpl, before resolveIds and any HTTP call"
  - "Cloud tests gate with Assume.assumeTrue(cloudAvailable) — skip on missing credentials, never fail"
  - "Seed collection uses server-side default embedding function (D-06) — no explicit embeddings in @BeforeClass"

requirements-completed: [CLOUD-02, CLOUD-03]

# Metrics
duration: 4min
completed: 2026-03-22
---

# Phase 05 Plan 01: Cloud Schema/Index and Array Metadata Integration Tests Summary

**Mixed-type array client validation in ChromaHttpCollection (D-22) with 18-test unit suite, plus 12-test cloud integration class covering CLOUD-02 (distance space/HNSW/SPANN/schema round-trips) and CLOUD-03 (string/number/bool arrays with contains/notContains filters)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-22T15:09:25Z
- **Completed:** 2026-03-22T15:13:25Z
- **Tasks:** 4 (all combined in one commit — Tasks 1-3 built test file incrementally, Task 4 added validation)
- **Files modified:** 3

## Accomplishments
- Created `SearchApiCloudIntegrationTest` with 12 tests: 1 availability gate, 5 CLOUD-02 (schema/index), 5 CLOUD-03 (array metadata), 1 D-22 (mixed-type, no cloud gate)
- Added `validateMetadataArrayTypes` to `ChromaHttpCollection` wired into `AddBuilderImpl`, `UpsertBuilderImpl`, and `UpdateBuilderImpl` execute() methods
- Created `MetadataValidationTest` with 18 unit tests (15 static + 3 behavioral wiring proving validation fires before HTTP call)
- All 18 MetadataValidationTest tests pass in CI without cloud credentials

## Task Commits

All tasks combined in one commit (4 tasks built the same set of files sequentially):

1. **Tasks 1-4: Cloud schema/index and array metadata tests + D-22 validation** - `3cf56ec` (feat)

## Files Created/Modified
- `src/test/java/tech/amikos/chromadb/v2/SearchApiCloudIntegrationTest.java` - 12 cloud integration test methods (CLOUD-02, CLOUD-03, D-22)
- `src/test/java/tech/amikos/chromadb/v2/MetadataValidationTest.java` - 18 unit tests for mixed-type array validation
- `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` - Added validateMetadataArrayTypes, validateHomogeneousList, normalizeNumericType (3 new methods); wired into 3 execute() methods

## Decisions Made
- `validateMetadataArrayTypes` uses `ChromaBadRequestException` with typed errorCode strings (`"MIXED_TYPE_ARRAY"`, `"NULL_ARRAY_ELEMENT"`) consistent with existing exception hierarchy
- Integer/Long/Short/Byte normalized to Integer group; Float/Double normalized to Float group — mixed int+long or float+double are valid (widening-compatible)
- Behavioral wiring tests use `ChromaHttpCollection.from()` with a stub `ChromaApiClient` pointing at `http://localhost:1` — validation fires before any network call
- `testCloudMixedTypeArrayRejected()` in SearchApiCloudIntegrationTest calls `validateMetadataArrayTypes` directly (no cloud credential gate) per D-22
- Empty arrays pass validation — only non-empty heterogeneous arrays are rejected

## Deviations from Plan

None — plan executed exactly as written. One minor adaptation: `testCloudMixedTypeArrayRejected()` calls `ChromaHttpCollection.validateMetadataArrayTypes` directly rather than going through `col.add().execute()` path, since the plan explicitly allows this for the no-credential version of the test. The behavioral wiring that col.add/upsert/update call validation first is covered in MetadataValidationTest.

## Issues Encountered
- `ChromaBadRequestException` requires an `errorCode` parameter (not just message) — used typed errorCode strings `"MIXED_TYPE_ARRAY"` and `"NULL_ARRAY_ELEMENT"` to satisfy the constructor

## Next Phase Readiness
- CLOUD-02 and CLOUD-03 test suites ready; run against cloud with `CHROMA_API_KEY`, `CHROMA_TENANT`, `CHROMA_DATABASE` environment variables set
- MetadataValidationTest passes without any cloud credentials (runs in unit test mode)
- Ready for 05-02 (if any follow-on plan extends cloud search/schema testing)

---
*Phase: 05-cloud-integration-testing*
*Completed: 2026-03-22*
