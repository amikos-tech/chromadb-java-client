# Phase 1: Transport & Auth Hardening - Research

**Date:** 2026-03-18
**Phase:** 1
**Requirements:** AUTH-01, AUTH-02, AUTH-03, API-04

## Summary

The v2 client already has a strong transport/auth baseline:
- HTTP request construction and auth header injection are centralized in `ChromaApiClient.newRequest()`.
- HTTP error translation is centralized in `ChromaApiClient.execute()` + `ChromaExceptions.fromHttpResponse(...)`.
- Cloud-specific auth (`X-Chroma-Token`) and identity/preflight APIs are implemented and tested.

Phase 1 should focus on hardening and consistency guarantees rather than net-new architecture:
1. Prove auth behavior is consistently applied for every request path and endpoint surface.
2. Close edge-case error translation gaps (especially blank/atypical server payloads and malformed success payloads).
3. Strengthen explicit validation and integration coverage for auth misconfiguration and cloud identity flows.

## Current Implementation Findings

### Transport/Auth Choke Points

- `src/main/java/tech/amikos/chromadb/v2/ChromaApiClient.java`
  - `newRequest()` always applies:
    - `User-Agent`
    - `Accept: application/json`
    - `defaultHeaders`
    - `authProvider.applyAuth(...)` when present
  - All verbs (`get/post/put/delete`) route through `newRequest()` and `execute()`.
  - Network failures are mapped to `ChromaConnectionException`.
  - Error payload parsing (`error`, `message`, `error_code`) is centralized in `parseErrorBody(...)`.

### Auth Provider Surface

- `src/main/java/tech/amikos/chromadb/v2/AuthProvider.java`
- `src/main/java/tech/amikos/chromadb/v2/TokenAuth.java` (Bearer auth)
- `src/main/java/tech/amikos/chromadb/v2/BasicAuth.java` (Basic auth)
- `src/main/java/tech/amikos/chromadb/v2/ChromaTokenAuth.java` (Cloud `X-Chroma-Token`)

Validation behavior today:
- Token-based providers reject null/blank tokens.
- Basic auth rejects null username/password.
- Cloud builder enforces required apiKey/tenant/database.

### Cloud/Identity Flow Surface

- `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java`
  - `CloudBuilder` uses `ChromaTokenAuth` and validates required fields.
  - `preFlight()` validates `max_batch_size` presence and positivity.
  - `getIdentity()` validates required fields (`user_id`, `tenant`, `databases`) and normalizes content.

### Exception Mapping Surface

- `src/main/java/tech/amikos/chromadb/v2/ChromaExceptions.java`
  - Explicit mappings: 400/401/403/404/409
  - Generic 4xx -> `ChromaClientException`
  - 5xx -> `ChromaServerException`

## Test Baseline (Already Strong)

- `src/test/java/tech/amikos/chromadb/v2/ChromaApiClientTest.java`
  - Covers verb behavior, auth headers, default header precedence, broad status mapping, malformed error payload handling, lifecycle (`close`), and deserialization failures.
- `src/test/java/tech/amikos/chromadb/v2/AuthProviderTest.java`
  - Covers provider header output and input validation.
- `src/test/java/tech/amikos/chromadb/v2/ChromaClientBuilderTest.java`
  - Covers cloud builder contract and ChromaTokenAuth usage.
- `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java`
  - Covers preflight + identity success/failure deserialization paths and unauthorized behavior.
- `src/test/java/tech/amikos/chromadb/v2/ErrorHandlingIntegrationTest.java`
  - Covers typed exception semantics against integration stack.

## Gaps and Hardening Opportunities

1. Endpoint-wide auth consistency should be asserted from top-level `Client` operations (not only transport unit tests), so regressions in future call paths are caught where users interact.
2. Cloud identity/preflight sequences need explicit integration scenarios validating both happy-path and auth-failure transitions under cloud-style auth setup.
3. Error translation should be hardened around edge payloads where message extraction may produce weak/ambiguous diagnostics.
4. Validation messaging for auth misconfiguration should remain explicit and consistent between `Builder` and `CloudBuilder` paths.

## Recommended Plan Structure

Break into three execution plans matching roadmap intent:
- 01-01: Audit and normalize auth application across request paths
- 01-02: Harden exception/error translation + auth validation diagnostics
- 01-03: Expand auth-focused unit/integration coverage

## Validation Architecture

### Test Infrastructure
- Framework: Maven + JUnit4 + WireMock + Testcontainers
- Quick run:
  - `mvn -Dtest=AuthProviderTest,ChromaApiClientTest,ChromaClientBuilderTest,ChromaClientImplTest,ChromaExceptionTest test`
- Full run:
  - `mvn test`

### Fast Feedback Contract
- After each task: run focused unit test subset for touched auth/transport files.
- After each plan wave: run full v2 unit suite.
- Before phase verification: run full test suite (`mvn test`) with integration tests.

### Required Evidence
- Auth headers observed correctly in transport tests for all configured provider types.
- Cloud preflight/identity behavior validated under both success and unauthorized/error paths.
- HTTP error status and payload variants map deterministically to expected `ChromaException` subclasses.
- Auth misconfiguration inputs return actionable validation errors (not null/blank ambiguity).

## Risks

- Overlapping coverage between transport unit tests and client-level tests can create redundant test cost if scope is not sharply partitioned.
- Behavior around server payload oddities may vary by Chroma version; assertions should pin guarantees that are stable across `>=1.0.0`.
- Hardening changes in exception parsing can unintentionally alter message text that downstream users/tests depend on.

## Sources

Primary code references:
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiClient.java`
- `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java`
- `src/main/java/tech/amikos/chromadb/v2/ChromaExceptions.java`
- `src/main/java/tech/amikos/chromadb/v2/AuthProvider.java`
- `src/main/java/tech/amikos/chromadb/v2/TokenAuth.java`
- `src/main/java/tech/amikos/chromadb/v2/BasicAuth.java`
- `src/main/java/tech/amikos/chromadb/v2/ChromaTokenAuth.java`

Primary test references:
- `src/test/java/tech/amikos/chromadb/v2/ChromaApiClientTest.java`
- `src/test/java/tech/amikos/chromadb/v2/AuthProviderTest.java`
- `src/test/java/tech/amikos/chromadb/v2/ChromaClientBuilderTest.java`
- `src/test/java/tech/amikos/chromadb/v2/ChromaClientImplTest.java`
- `src/test/java/tech/amikos/chromadb/v2/ErrorHandlingIntegrationTest.java`
- `src/test/java/tech/amikos/chromadb/v2/ChromaExceptionTest.java`

## RESEARCH COMPLETE

Research completed and written to `.planning/phases/01-transport-auth-hardening/01-RESEARCH.md`.
