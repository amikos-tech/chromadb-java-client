---
phase: 02-api-coverage-completion
verified: 2026-03-18T19:09:58Z
status: human_needed
score: 10/10 must-haves verified
human_verification:
  - test: "Run a live parity smoke test against non-local Chroma deployments (1.5.5 and 1.3.7)."
    expected: "Tenant/database lifecycle, schema/config/CMEK round-trip, and add/get/query/update/upsert/delete parity match automated integration results."
    why_human: "Automated checks ran in controlled test execution; a human-run live environment check validates real network/auth/runtime conditions."
---

# Phase 2: API Coverage Completion Verification Report

**Phase Goal:** Achieve complete and predictable v2 endpoint/operation parity required by current requirements.
**Verified:** 2026-03-18T19:09:58Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | `createTenant`/`createDatabase` return response names when server names are non-blank | ✓ VERIFIED | [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:705), [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:749), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:485), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:622) |
| 2 | `createTenant`/`createDatabase` fall back to request names only when response names are missing/blank | ✓ VERIFIED | [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:974), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:498), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:661) |
| 3 | `getTenant`/`getDatabase`/`listDatabases` remain strict on malformed payload fields | ✓ VERIFIED | [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:719), [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:765), [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:783), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:589), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:754) |
| 4 | Top-level `schema` is canonical when both top-level and `configuration.schema` are present | ✓ VERIFIED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:75), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1412) |
| 5 | Unknown schema/config/CMEK keys are preserved through parse→serialize flows | ✓ VERIFIED | [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:438), [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:549), [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:1547), [ChromaDtosContractTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java:349), [ChromaDtosContractTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java:370) |
| 6 | Collection mapping retains non-lossy schema/config information on read paths | ✓ VERIFIED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:72), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:75), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1412) |
| 7 | Query accepts both `queryTexts` and `queryEmbeddings` in one flow | ✓ VERIFIED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:614), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:626), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1001), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1020) |
| 8 | When both inputs are present, embeddings are authoritative and text embedding is skipped | ✓ VERIFIED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:660), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:666), [RecordOperationsIntegrationTest.java](src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java:337) |
| 9 | Omitted `include` stays server-authoritative; explicit includes are forwarded exactly | ✓ VERIFIED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:671), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1039), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1056) |
| 10 | Phase 2 parity checks run against Chroma `1.5.5` and `1.3.7` | ✓ VERIFIED | [Makefile](Makefile:66); `make test-phase-02-parity` exited `0` (exec output captured during verification) |

**Score:** 10/10 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java` | API-01 lifecycle mapping | ✓ VERIFIED | Exists (1043 lines), substantive implementation, referenced by builder/tests ([ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:620)) |
| `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java` | API-01 contract tests | ✓ VERIFIED | Exists (1554 lines), create/fallback/strict-read assertions present, executed in parity target |
| `src/test/java/tech/amikos/chromadb/v2/TenantDatabaseIntegrationTest.java` | API-01 integration lifecycle parity | ✓ VERIFIED | Exists (169 lines), typed non-blank lifecycle test ([TenantDatabaseIntegrationTest.java](src/test/java/tech/amikos/chromadb/v2/TenantDatabaseIntegrationTest.java:69)), executed in integration matrix |
| `src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java` | API-02 passthrough model | ✓ VERIFIED | Exists (219 lines), immutable passthrough API (`getPassthrough`/`Builder.passthrough`) |
| `src/main/java/tech/amikos/chromadb/v2/Schema.java` | API-02 schema passthrough model | ✓ VERIFIED | Exists (165 lines), immutable passthrough API and schema helpers |
| `src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java` | API-02 parse/serialize parity | ✓ VERIFIED | Exists (1655 lines), parse/to map + passthrough merge logic wired into mapping paths |
| `src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java` | API-02 contract tests | ✓ VERIFIED | Exists (584 lines), `PreservesUnknown`/`TypedAuthoritative` tests present and passing |
| `src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java` | API-02/API-03 behavior contracts | ✓ VERIFIED | Exists (2319 lines), schema precedence + mixed query/include payload tests present and passing |
| `src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java` | API-03 query/include behavior | ✓ VERIFIED | Exists (1154 lines), execute-time embedding precedence + include forwarding logic implemented |
| `src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java` | API-03 record ops parity | ✓ VERIFIED | Exists (652 lines), add/get/query/update/upsert/delete integration coverage present |
| `src/test/java/tech/amikos/chromadb/v2/SchemaAndQueryTextsIntegrationTest.java` | API-03 queryTexts/schema integration | ✓ VERIFIED | Exists (274 lines), queryTexts and schema/embedding integration behavior covered |
| `Makefile` | Reproducible phase-2 matrix gate | ✓ VERIFIED | Exists (211 lines), `test-phase-02-parity` target present and executable |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `ChromaClient#createTenant/createDatabase` | DTO response mapping | `apiClient.post(..., TenantResponse/DatabaseResponse.class)` + `resolveCreateName` | ✓ WIRED | [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:705), [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:749), [ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:974) |
| API-01 lifecycle behavior | Typed create/get/list/delete contract | Unit + integration tests | ✓ WIRED | [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:467), [TenantDatabaseIntegrationTest.java](src/test/java/tech/amikos/chromadb/v2/TenantDatabaseIntegrationTest.java:69), matrix execution passed |
| `ChromaDtos.parseConfiguration` | `toConfigurationMap` round-trip | parse→serialize + passthrough merge | ✓ WIRED | [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:438), [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:309), [ChromaDtosContractTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java:349) |
| `ChromaDtos.parseSchema` | `toSchemaMap` round-trip | schema/CMEK parse→serialize + merge | ✓ WIRED | [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:549), [ChromaDtos.java](src/main/java/tech/amikos/chromadb/v2/ChromaDtos.java:359), [ChromaDtosContractTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java:370) |
| API-02 collection mapping | Non-lossy read-path schema/config retention | `ChromaHttpCollection.from` + schema precedence | ✓ WIRED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:59), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:75), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1412) |
| Query builder setters | Execution-time embedding resolution | `QueryBuilderImpl.execute` precedence | ✓ WIRED | [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:614), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:626), [ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:660) |
| Query payload contract | Include/query embeddings request shape | WireMock payload assertions | ✓ WIRED | [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1001), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1039), [ChromaHttpCollectionTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaHttpCollectionTest.java:1056) |
| API-03 parity matrix | Chroma version compatibility gate | `Makefile` target + execution | ✓ WIRED | [Makefile](Makefile:66); `make test-phase-02-parity` passed with `CHROMA_VERSION=1.5.5` and `1.3.7` |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| API-01 | 02-01-PLAN.md | User can manage tenants/databases through v2 methods with typed results | ✓ SATISFIED | create/get/list/delete behavior in implementation + contract/integration tests ([ChromaClient.java](src/main/java/tech/amikos/chromadb/v2/ChromaClient.java:705), [ChromaClientImplTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java:467), [TenantDatabaseIntegrationTest.java](src/test/java/tech/amikos/chromadb/v2/TenantDatabaseIntegrationTest.java:69)) |
| API-02 | 02-02-PLAN.md | User can manage collections with schema/config/CMEK options without lossy serialization | ✓ SATISFIED | passthrough models + parse/serialize merge + contract tests ([CollectionConfiguration.java](src/main/java/tech/amikos/chromadb/v2/CollectionConfiguration.java:59), [Schema.java](src/main/java/tech/amikos/chromadb/v2/Schema.java:46), [ChromaDtosContractTest.java](src/test/java/tech/amikos/chromadb/v2/ChromaDtosContractTest.java:349)) |
| API-03 | 02-03-PLAN.md | User can execute add/get/query/update/upsert/delete with full filter/include/queryTexts behavior | ✓ SATISFIED | query precedence/include forwarding implementation + record ops integration suite + matrix run ([ChromaHttpCollection.java](src/main/java/tech/amikos/chromadb/v2/ChromaHttpCollection.java:660), [RecordOperationsIntegrationTest.java](src/test/java/tech/amikos/chromadb/v2/RecordOperationsIntegrationTest.java:337), [Makefile](Makefile:66)) |

Orphaned requirements for Phase 2 in `.planning/REQUIREMENTS.md`: **None** (API-01/API-02/API-03 all claimed by Phase 2 plans).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| — | — | No `TODO`/`FIXME`/placeholder stub patterns found in phase-modified artifacts | ℹ️ Info | No blocker anti-patterns identified |

### Human Verification Required

### 1. Live Multi-Version Parity Smoke

**Test:** Against non-local Chroma deployments at versions `1.5.5` and `1.3.7`, run an end-to-end scenario: tenant/database create/get/list/delete, collection create/get with schema+config+CMEK, and record add/get/query/update/upsert/delete with `where`/`whereDocument`/`include`/`queryTexts`.
**Expected:** Behavior matches automated suite outcomes and remains consistent across both versions in a real network/auth environment.
**Why human:** External service integration in real deployment conditions (network, auth, infra policies) is beyond static/code-only verification.

### Gaps Summary

No implementation gaps were found in automated verification. Phase goal evidence is complete in code and tests; only live-environment human validation remains.

---

_Verified: 2026-03-18T19:09:58Z_
_Verifier: Claude (gsd-verifier)_
