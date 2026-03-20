---
phase: 01
slug: transport-auth-hardening
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-18
validated: 2026-03-20
---

# Phase 01 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Maven Surefire + WireMock + Testcontainers |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn -Dtest=AuthProviderTest,ChromaApiClientTest,ChromaClientBuilderTest,ChromaClientImplTest,ChromaExceptionTest,Phase01ValidationTest test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~4 seconds (unit only) / ~240 seconds (full with integration) |

---

## Sampling Rate

- **After every task commit:** Run `mvn -Dtest=AuthProviderTest,ChromaApiClientTest,ChromaClientBuilderTest,ChromaClientImplTest,ChromaExceptionTest,Phase01ValidationTest test`
- **After every plan wave:** Run `mvn test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 240 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | AUTH-01 | unit | `mvn -Dtest=ChromaApiClientTest,ChromaClientBuilderTest,Phase01ValidationTest test` | yes | green |
| 01-02-01 | 02 | 1 | API-04, AUTH-02 | unit | `mvn -Dtest=ChromaApiClientTest,ChromaExceptionTest,ChromaClientImplTest,Phase01ValidationTest test` | yes | green |
| 01-03-01 | 03 | 2 | AUTH-02, AUTH-03 | unit+integration | `mvn -Dtest=AuthProviderTest,ChromaClientBuilderTest,ChromaClientImplTest,ErrorHandlingIntegrationTest,Phase01ValidationTest test` | yes | green |

*Status: pending / green / red / flaky*

---

## Nyquist Validation Test

| File | Tests | Runner | Status |
|------|-------|--------|--------|
| `src/test/java/tech/amikos/chromadb/v2/Phase01ValidationTest.java` | 27 | `mvn -Dtest=Phase01ValidationTest test` | green |

### Requirement Coverage via Phase01ValidationTest

| Requirement | Tests | Verified Behavior |
|-------------|-------|-------------------|
| AUTH-01 | `testUserCanConfigureTokenAuthAndHeartbeatHonorsIt`, `testUserCanConfigureBasicAuthAndVersionEndpointHonorsIt`, `testUserCanConfigureChromaTokenAuthAndCollectionListHonorsIt`, `testUserCanConfigureApiKeyShortcutAndItSendsBearerAuth`, `testAuthIsHonoredAcrossMultipleClientOperations` | Auth configured via builder is applied on every client-level HTTP request. |
| AUTH-02 | `testPreFlightUnauthorizedReturnsTypedExceptionNotFallback`, `testIdentityForbiddenReturnsTypedExceptionNotFallback`, `testPreFlightMalformedPayloadThrowsDeserializationWithEndpointContext`, `testIdentityMalformedPayloadThrowsDeserializationWithFieldContext` | Cloud preflight/identity 401/403 produce typed exceptions with actionable messages; malformed payloads produce ChromaDeserializationException with endpoint/field context. |
| AUTH-03 | `testSecondAuthStrategyOnBuilderFailsWithActionableMessage`, `testCloudBuilderSecondApiKeyFailsWithActionableMessage`, `testReservedAuthHeaderInDefaultHeadersFailsWithGuidance`, `testReservedXChromaTokenHeaderInDefaultHeadersFailsWithGuidance`, `testNullTokenAuthRejectedWithClearException`, `testBlankTokenAuthRejectedWithActionableMessage`, `testMissingEnvVarTokenAuthFailsWithVariableName`, `testCloudBuilderMissingRequiredFieldsFailsAtBuildTime` | Auth misconfiguration yields immediate, actionable exceptions with field context. |
| API-04 | `testClientLevelCallMaps400ToBadRequest`, `testClientLevelCallMaps404ToNotFoundWithErrorCode`, `testClientLevelCallMaps409ToConflict`, `testClientLevelCallMaps500ToServerException`, `testClientLevelCallMaps401ToUnauthorized`, `testNonJsonErrorFallsBackToDeterministicFormat`, `testErrorCodePreservedThroughClientCall`, `testConnectionErrorMapsToConnectionExceptionWithEndpointContext`, `testExceptionHierarchyIsRuntimeBased`, `testFactoryMappingCoversAllDocumentedStatusCodes` | HTTP errors map to correct ChromaException subclasses with status codes, error_code metadata, and deterministic fallback formatting. |

---

## Wave 0 Requirements

- Existing infrastructure covers all phase requirements.
- No new test framework or tooling was needed.

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 240s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated 2026-03-20

---

## Verification Commands (Copy-Paste)

```bash
# Phase 1 unit tests (fast, ~4s)
mvn -Dtest=AuthProviderTest,ChromaApiClientTest,ChromaClientBuilderTest,ChromaClientImplTest,ChromaExceptionTest,Phase01ValidationTest test

# Phase 1 Nyquist validation only (~3s)
mvn -Dtest=Phase01ValidationTest test
```
