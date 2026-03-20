---
phase: 02
slug: api-coverage-completion
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-18
validated: 2026-03-20
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Maven Surefire + WireMock 2.35.2 + Testcontainers |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn -q -Dtest=Phase02ValidationTest test` |
| **Full suite command** | `mvn -q -Dtest=ChromaClientImplTest,ChromaHttpCollectionTest,ChromaDtosContractTest,Phase02ValidationTest test` |
| **Integration suite** | `CHROMA_VERSION=1.5.5 mvn -q -Pintegration -Dtest=TenantDatabaseIntegrationTest,CollectionLifecycleIntegrationTest,RecordOperationsIntegrationTest,SchemaAndQueryTextsIntegrationTest test` |
| **Estimated runtime** | ~5 seconds (unit), ~600 seconds (full with integration) |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -Dtest=ChromaClientImplTest,ChromaHttpCollectionTest,ChromaDtosContractTest test`
- **After every plan wave:** Run `CHROMA_VERSION=1.5.5 mvn -q -Pintegration -Dtest=TenantDatabaseIntegrationTest,CollectionLifecycleIntegrationTest,RecordOperationsIntegrationTest,SchemaAndQueryTextsIntegrationTest test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 600 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | API-01 | unit | `mvn -q -Dtest=ChromaClientImplTest test` | yes | green |
| 02-02-01 | 02 | 1 | API-02 | unit | `mvn -q -Dtest=ChromaDtosContractTest,ChromaHttpCollectionTest,ChromaClientImplTest test` | yes | green |
| 02-03-01 | 03 | 2 | API-03 | unit | `mvn -q -Dtest=ChromaHttpCollectionTest,ChromaClientImplTest test` | yes | green |
| nyquist | all | -- | SC-1..4 | unit | `mvn -q -Dtest=Phase02ValidationTest test` | yes | green |

*Status: pending -- green -- red -- flaky*

---

## Nyquist Validation Tests

| # | File | Type | Command | Tests | Status |
|---|------|------|---------|-------|--------|
| 1 | `src/test/java/tech/amikos/chromadb/v2/Phase02ValidationTest.java` | unit (WireMock) | `mvn -q -Dtest=Phase02ValidationTest test` | 25 | green |

### Success Criteria Coverage

| SC | Requirement | Test Methods | Status |
|----|-------------|-------------|--------|
| SC-1 | Tenant/database lifecycle with typed request/response contracts | testUserCanCreateTenantAndGetTypedResult, testUserCanCreateDatabaseAndGetTypedResult, testCreateTenantFallsBackToRequestNameWhenServerOmitsName, testCreateDatabaseFallsBackToRequestNameWhenServerReturnsBlank, testGetTenantRejectsServerPayloadWithMissingName, testListDatabasesRejectsEntryWithBlankName, testUserCanDeleteDatabase | green |
| SC-2 | Collections with schema/config/CMEK data preserved round-trip | testUnknownConfigKeysPreservedThroughParseSerializeRoundTrip, testUnknownSchemaKeysAndCmekProvidersPreservedThroughRoundTrip, testTypedFieldsOverrideConflictingPassthroughOnSerialization, testTopLevelSchemaIsCanonicalWhenBothSchemaLocationsPresent | green |
| SC-3 | Full record operations with filter/include/queryTexts behavior | testUserCanAddRecordsWithEmbeddingsAndDocuments, testUserCanQueryWithEmbeddingsAndGetResults, testMixedQueryInputsAcceptedWithEmbeddingsAuthoritative, testOmittedIncludeDoesNotForceClientDefaults, testExplicitIncludeForwardedExactly, testUserCanGetRecordsByIds, testUserCanUpdateRecords, testUserCanUpsertRecords, testUserCanDeleteRecords | green |
| SC-4 | Contract tests prove response mapping for covered API surface | testTenantResponseMapsToTypedObject, testDatabaseResponseMapsToTypedObject, testCollectionResponseMapsConfigurationAndMetadata, testQueryResponseMapsIdsAndDistances, testParityMatrixTargetExistsInMakefile | green |

---

## Wave 0 Requirements

- [x] `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java` -- lifecycle fallback and response-authoritative mapping assertions (99 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java` -- schema/config/CMEK round-trip and unknown-key preservation fixtures (31 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` -- mixed query input precedence and include behavior parity checks (125 tests, all green)
- [x] `src/test/java/tech/amikos/chromadb/v2/Phase02ValidationTest.java` -- Nyquist behavioral validation for all 4 success criteria (25 tests, all green)

---

## Manual-Only Verifications

All phase behaviors have automated verification. No manual-only checks are required.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 600s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** complete (2026-03-20)
