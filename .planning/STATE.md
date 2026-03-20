---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: unknown
stopped_at: Completed 04-02-PLAN.md
last_updated: "2026-03-20T09:03:03.589Z"
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 11
  completed_plans: 11
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-17)

**Core value:** Java developers can integrate Chroma quickly and safely with a predictable, strongly-typed client that behaves consistently across environments.
**Current focus:** Phase 04 — compatibility-test-matrix

## Current Position

Phase: 04 (compatibility-test-matrix) — EXECUTING
Plan: 1 of 2

## Performance Metrics

**Velocity:**

- Total plans completed: 3
- Average duration: ~25 min
- Total execution time: ~1.3 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Transport & Auth Hardening | 3 | ~75 min | ~25 min |

**Recent Trend:**

- Last 3 plans: 01-01, 01-02, 01-03
- Trend: Stable

*Updated after each plan completion*
| Phase 02-api-coverage-completion P02 | 10min | 2 tasks | 5 files |
| Phase 02-api-coverage-completion P01 | 16min | 2 tasks | 3 files |
| Phase 02-api-coverage-completion P03 | 8min | 3 tasks | 4 files |
| Phase 03-embeddings-id-extensibility P01 | 20min | 2 tasks | 13 files |
| Phase 03-embeddings-id-extensibility PP03 | 28min | 2 tasks | 4 files |
| Phase 03-embeddings-id-extensibility P02 | 5min | 2 tasks | 2 files |
| Phase 04-compatibility-test-matrix PP01 | 15min | 3 tasks | 3 files |
| Phase 04-compatibility-test-matrix PP02 | 15 | 2 tasks | 2 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Repo-wide auth ambiguity is forbidden: exactly one auth strategy per builder.
- `defaultHeaders` must reject auth-conflicting keys and nudge users to `auth(...)`.
- Cloud preflight/identity auth failures are strict/typed (`ChromaUnauthorizedException`) with actionable safe hints.
- Mapping/auth contract changes require explicit tests and changelog entry.
- [Phase 02-api-coverage-completion]: Unknown config/schema payload is stored as immutable passthrough maps to prevent lossy round-trips.
- [Phase 02-api-coverage-completion]: Schema serialization is typed-authoritative: typed known keys override passthrough conflicts.
- [Phase 02-api-coverage-completion]: Unsupported CMEK provider entries are preserved in schema passthrough while known providers remain strictly validated.
- [Phase 02-api-coverage-completion]: createTenant/createDatabase are response-authoritative when response name is non-blank.
- [Phase 02-api-coverage-completion]: Fallback to request names is limited to create methods and not applied to get/list.
- [Phase 02-api-coverage-completion]: Mixed query inputs are accepted in either setter order; explicit queryEmbeddings are authoritative and queryTexts are embed fallback only.
- [Phase 02-api-coverage-completion]: Query include stays server-authoritative when omitted and is forwarded exactly when explicitly set by caller.
- [Phase 03-embeddings-id-extensibility P01]: ChromaException convenience constructors widened to public so embeddings.* packages can throw directly.
- [Phase 03-embeddings-id-extensibility P01]: EF precedence: explicit runtime > configuration.embedding_function > schema.default_embedding_function.
- [Phase 03-embeddings-id-extensibility P01]: WARNING log fires when explicit EF overrides persisted spec; FINE log fires on auto-wire from spec.
- [Phase 03-embeddings-id-extensibility]: Sha256IdGenerator throws IllegalArgumentException when both document AND metadata are null; empty metadata map is valid
- [Phase 03-embeddings-id-extensibility]: ChromaException is the boundary exception for all IdGenerator failures (null, blank, runtime exception) per EMB-04
- [Phase 03-embeddings-id-extensibility]: serializeMetadata uses TreeMap (sorted keys), key=value;key=value format, package-private for testability
- [Phase 03-embeddings-id-extensibility P02]: DefaultEmbeddingFunction download failures throw ChromaException (unchecked, v2) not EFException; validateModel() is stateless file-existence check (no static boolean flag)
- [Phase 03-embeddings-id-extensibility P02]: modelDownloadUrl is package-private non-final static for WireMock test injection without reflection
- [Phase 04]: Container startup failure throws AssertionError (fail-fast) not Assume.assumeTrue (silent skip) to make bad images visible
- [Phase 04]: GitHub Actions include pattern (not exclude) for JDK 11/17 x 1.5.5 cells produces exactly 5 cells with no accidental combinations
- [Phase 04]: CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5 centralized in Makefile; CI workflow maintains its own parallel matrix definition
- [Phase 04]: animal-sniffer check goal defaults to process-test-classes (runs during mvn test, not mvn compile) — no explicit phase override added per plan instructions
- [Phase 04]: EXPECTED_BUILDER_METHOD_COUNT=34 and EXPECTED_CLOUD_BUILDER_METHOD_COUNT=8 (getDeclaredMethods includes private methods for concrete classes; public-only counts would be 20 and 6)

### Pending Todos

None.

### Blockers/Concerns

- [RESOLVED by P02] Full `mvn test` run was interrupted after stalling on first-time ONNX model download — resolved by OkHttp download with 300s timeout in DefaultEmbeddingFunction.

## Session Continuity

Last session: 2026-03-20T09:03:03.586Z
Stopped at: Completed 04-02-PLAN.md
Resume file: None
