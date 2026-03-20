# Phase 1: Transport & Auth Hardening - Context

**Gathered:** 2026-03-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver endpoint-wide auth consistency and reliable exception semantics for the existing v2 client surface. This phase clarifies and hardens auth behavior, cloud identity/preflight behavior, and error translation behavior within current scope; it does not add new product capabilities.

</domain>

<decisions>
## Implementation Decisions

### Auth configuration boundary (repo-wide)
- Auth ambiguity is forbidden across the repository.
- Every builder/util entry point supports exactly one auth strategy per instance.
- Convenience methods stay, but they route through one auth slot (`auth(...)`-backed behavior).
- If a second auth strategy is set, fail fast immediately.
- `build()` performs final auth invariant validation as a safety net.
- `defaultHeaders` must reject auth-conflicting keys (`Authorization`, `X-Chroma-Token`) with an error that nudges users to `auth(...)`.

### Cloud identity and preflight behavior
- For auth failures on cloud identity/preflight endpoints, behavior is strict and typed: 401/403 map to `ChromaUnauthorizedException` (no fallback objects).
- If auth succeeds but response payload is malformed/missing required fields, throw `ChromaDeserializationException` with endpoint + field detail.
- Error messages for 401/403 include actionable hints (for example: verify `auth(...)`, token type, tenant/database setup) without leaking sensitive values.
- Coverage requirement: both unit and integration tests must assert typed behavior for these flows.

### Error translation contract
- Status-to-exception mapping is allowed to evolve, but any behavior change must include explicit tests and changelog entry.
- Message style policy: actionable normalized message first, sanitized raw snippet second.
- Malformed/non-JSON error fallback is deterministic: `HTTP <status>: <sanitized-truncated-body or reason>`.
- `error_code` is preserved as first-class exception data and must be test-asserted.

### Auth validation strictness
- Validate auth inputs immediately at setter/factory time (do not defer obvious validation).
- Keep final `build()` invariant checks for cross-field safety.
- Exception-type contract:
  - `NullPointerException` for null required inputs
  - `IllegalArgumentException` for blank/invalid values
  - `IllegalStateException` for missing environment variables
- Validation messages must be field-specific, actionable, and must not echo secrets.
- Add explicit repo-level auth validation contract documentation plus conformance tests so new builders follow the same rules.

### Claude's Discretion
- Exact API shape for introducing unified auth strategy types (for example helper factories or wrapper types) as long as the one-auth-only rule and fail-fast semantics are preserved.
- Exact wording of actionable hints/messages as long as they remain explicit, safe, and test-asserted.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and milestone contracts
- `.planning/ROADMAP.md` — Phase 1 goal, requirements linkage, and plan intent.
- `.planning/REQUIREMENTS.md` — AUTH-01/AUTH-02/AUTH-03/API-04 acceptance targets.
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8, synchronous API).
- `.planning/STATE.md` — current project state and continuity baseline.

### Project implementation conventions
- `CLAUDE.md` — repository architecture constraints, testing commands, and Java/client design conventions.

### Phase research baseline
- `.planning/phases/01-transport-auth-hardening/01-RESEARCH.md` — identified transport/auth choke points, risk areas, and validation architecture.
- `.planning/phases/01-transport-auth-hardening/01-VALIDATION.md` — per-task validation expectations for this phase.

### Existing auth and error implementation anchors
- `src/main/java/tech/amikos/chromadb/v2/AuthProvider.java` — auth abstraction contract.
- `src/main/java/tech/amikos/chromadb/v2/TokenAuth.java` — bearer auth behavior and validation.
- `src/main/java/tech/amikos/chromadb/v2/BasicAuth.java` — basic auth behavior and validation.
- `src/main/java/tech/amikos/chromadb/v2/ChromaTokenAuth.java` — cloud token auth behavior and validation.
- `src/main/java/tech/amikos/chromadb/v2/ChromaApiClient.java` — centralized request construction/auth header injection/error parsing.
- `src/main/java/tech/amikos/chromadb/v2/ChromaExceptions.java` — status-to-exception mapping surface.
- `src/main/java/tech/amikos/chromadb/v2/ChromaClient.java` — builder APIs, cloud flow, preflight/identity endpoints.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChromaApiClient.newRequest()` already centralizes default headers + auth header application.
- `AuthProvider` with `TokenAuth`, `BasicAuth`, and `ChromaTokenAuth` already provides pluggable auth strategies.
- `ChromaExceptions.fromHttpResponse(...)` is a single mapping factory for typed HTTP failures.

### Established Patterns
- Builder-driven client construction (`ChromaClient.builder()` / `ChromaClient.cloud()`) is the existing API pattern.
- Fail-fast validation with explicit exception types is already used in several value/auth classes.
- Error typing is treated as part of public API behavior and backed by dedicated tests.

### Integration Points
- Auth exclusivity and header-conflict policy should be enforced in builder/auth configuration paths before request execution.
- Cloud preflight/identity hardening hooks into existing `preFlight()` and `getIdentity()` methods in `ChromaClient`.
- Error normalization behavior is implemented in `ChromaApiClient.parseErrorBody(...)` + `ChromaExceptions` mapping.
- Conformance enforcement belongs in existing v2 auth/error test suites (`AuthProviderTest`, `ChromaApiClientTest`, `ChromaClientBuilderTest`, `ChromaClientImplTest`, `ChromaExceptionTest`, `ErrorHandlingIntegrationTest`).

</code_context>

<specifics>
## Specific Ideas

- API surface should not allow ambiguous auth configuration states.
- Fold key/password conveniences into a single conceptual auth pathway while keeping ergonomics.
- This policy is a rule across the whole repository, not a one-off for one builder.
- Error guidance should help users fix configuration quickly without exposing secrets.

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within Phase 1 scope.

</deferred>

---

*Phase: 01-transport-auth-hardening*
*Context gathered: 2026-03-18*
