---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: "Completed 03-01-PLAN.md"
last_updated: "2026-03-19T09:58:00.000Z"
progress:
  total_phases: 5
  completed_phases: 2
  total_plans: 9
  completed_plans: 7
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-17)

**Core value:** Java developers can integrate Chroma quickly and safely with a predictable, strongly-typed client that behaves consistently across environments.
**Current focus:** Phase 03 — embeddings-id-extensibility

## Current Position

Phase: 03 (embeddings-id-extensibility) — EXECUTING
Plan: 2 of 3

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

### Pending Todos

None.

### Blockers/Concerns

- Full `mvn test` run was interrupted after stalling on first-time ONNX model download; targeted Phase 1 regression suites passed.

## Session Continuity

Last session: 2026-03-19T09:58:00.000Z
Stopped at: Completed 03-01-PLAN.md
Resume file: .planning/phases/03-embeddings-id-extensibility/03-02-PLAN.md
