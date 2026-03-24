---
phase: 06-documentation-site
plan: 03
subsystem: docs
tags: [mkdocs, mkdocs-material, documentation, cloud-features, schema, cmek, id-generators, transport, logging, migration]

# Dependency graph
requires: ["06-01"]
provides:
  - docs/docs/cloud-features.md with full parity table and cloud-only method docs (fork, forkCount, indexingStatus)
  - docs/docs/schema.md with Schema.builder(), EmbeddingFunctionSpec, CMEK, and HNSW config
  - docs/docs/id-generators.md with UUID/ULID/SHA-256 generators and validation rules
  - docs/docs/transport.md with SSL, timeouts, custom OkHttpClient, insecure mode, env-based config
  - docs/docs/logging.md with ChromaLogger interface and custom logger bridging
  - docs/docs/migration.md with v1-to-v2 mapping table and tabbed before/after examples
  - docs/docs/assets/snippets/CloudExample.java with cloud-client, fork, fork-count, indexing-status sections
  - docs/docs/assets/snippets/SchemaExample.java with basic-schema, with-ef-spec, cmek, create-with-schema, hnsw-config sections
  - docs/docs/assets/snippets/IdGeneratorsExample.java with uuid, ulid, sha256 sections
  - docs/docs/assets/snippets/TransportExample.java with ssl-cert, custom-timeouts, custom-http, insecure, env-tenant, full-example sections
  - docs/docs/assets/snippets/LoggingExample.java with default-logger, custom-logger sections
affects: [06-04]

# Tech tracking
tech-stack:
  added: []
  patterns: [pymdownx.snippets for Java snippet includes, pymdownx.tabbed for v1/v2 side-by-side migration examples, admonitions for warnings and notes]

key-files:
  created:
    - docs/docs/cloud-features.md
    - docs/docs/schema.md
    - docs/docs/id-generators.md
    - docs/docs/transport.md
    - docs/docs/logging.md
    - docs/docs/migration.md
    - docs/docs/assets/snippets/CloudExample.java
    - docs/docs/assets/snippets/SchemaExample.java
    - docs/docs/assets/snippets/IdGeneratorsExample.java
    - docs/docs/assets/snippets/TransportExample.java
    - docs/docs/assets/snippets/LoggingExample.java
  modified: []

key-decisions:
  - "Migration page uses pymdownx.tabbed for v1/v2 side-by-side examples (=== tabs) — v1 code is the designated exception per D-11; all other pages are v2-only"
  - "ChromaLoggers is package-private; logging docs expose only ChromaLogger interface and ChromaLogger.noop() as the public API surface"
  - "LoggingExample.java bridges to java.util.logging as the concrete example since it is in the JDK; snippet documents the bridging pattern applicable to SLF4J/Log4j2"

# Metrics
duration: 7min
completed: 2026-03-24
---

# Phase 06 Plan 03: Advanced Guide Pages Summary

**6 advanced guide pages (cloud-features, schema/CMEK, id-generators, transport, logging, migration) with 5 companion Java snippet files using pymdownx.snippets includes; all 12 documentation site guide pages now have substantive content**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-03-24T15:30:00Z
- **Completed:** 2026-03-24T15:37:11Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments

- **cloud-features.md** — Full feature parity table (28 operations), Chroma Cloud connection, fork/forkCount/indexingStatus docs with CloudExample.java snippets
- **schema.md** — Schema.builder() with VectorIndexConfig, EmbeddingFunctionSpec, CMEK (GCP KMS), and CollectionConfiguration HNSW tuning with SchemaExample.java snippets
- **id-generators.md** — UUID, ULID, SHA-256 generators with comparison table and validation rules with IdGeneratorsExample.java snippets
- **transport.md** — SSL cert loading, custom timeouts, OkHttpClient injection, insecure TLS, env-based tenant/database with TransportExample.java snippets
- **logging.md** — ChromaLogger interface, noop logger, java.util.logging bridge example with LoggingExample.java snippets
- **migration.md** — v1-to-v2 mapping table, 4 before/after examples in pymdownx.tabbed tabs, next steps links
- `mkdocs build --strict` exits 0 with all 12 guide pages having substantive content

## Task Commits

Each task was committed atomically:

1. **Task 1: cloud-features, schema, id-generators pages + snippets** - `e0b9be6` (feat)
2. **Task 2: transport, logging, migration pages + snippets** - `4a0e527` (feat)

## Files Created

- `docs/docs/cloud-features.md` — Cloud features guide (89 lines)
- `docs/docs/schema.md` — Schema & CMEK guide (74 lines)
- `docs/docs/id-generators.md` — ID generators guide (44 lines)
- `docs/docs/transport.md` — Transport options guide (67 lines)
- `docs/docs/logging.md` — Logging guide (55 lines)
- `docs/docs/migration.md` — Migration from v1 guide (150 lines)
- `docs/docs/assets/snippets/CloudExample.java` — cloud-client, fork, fork-count, indexing-status named sections
- `docs/docs/assets/snippets/SchemaExample.java` — basic-schema, with-ef-spec, cmek, create-with-schema, hnsw-config named sections
- `docs/docs/assets/snippets/IdGeneratorsExample.java` — uuid, ulid, sha256 named sections
- `docs/docs/assets/snippets/TransportExample.java` — ssl-cert, custom-timeouts, custom-http, insecure, env-tenant, full-example named sections
- `docs/docs/assets/snippets/LoggingExample.java` — default-logger, custom-logger named sections

## Decisions Made

- **Migration page tab format:** Used `=== "v1 (Removed)"` and `=== "v2 (Current)"` with `pymdownx.tabbed` — the migration page is the single designated exception to D-10 (v2-only docs) per D-11.
- **Logging API scope:** `ChromaLoggers` is package-private; exposed only `ChromaLogger` interface and `ChromaLogger.noop()` in docs. The logging example bridges to `java.util.logging` as a concrete pattern.
- **LoggingExample.java scope:** The ChromaLogger bridging pattern is illustrative (not compilable standalone). The snippet documents the interface contract and bridging approach.

## Deviations from Plan

None — plan executed exactly as written. All 6 guide pages created with the specified content structure. All 5 companion snippet files created with the specified named sections. `mkdocs build --strict` passes.

## Known Stubs

None. All 6 guide pages contain complete, substantive documentation with working pymdownx.snippets includes. The migration page has all 4 before/after example sections from MIGRATION.md.

## Self-Check: PASSED

- FOUND: docs/docs/cloud-features.md
- FOUND: docs/docs/schema.md
- FOUND: docs/docs/id-generators.md
- FOUND: docs/docs/transport.md
- FOUND: docs/docs/logging.md
- FOUND: docs/docs/migration.md
- FOUND: docs/docs/assets/snippets/CloudExample.java
- FOUND: docs/docs/assets/snippets/SchemaExample.java
- FOUND: docs/docs/assets/snippets/IdGeneratorsExample.java
- FOUND: docs/docs/assets/snippets/TransportExample.java
- FOUND: docs/docs/assets/snippets/LoggingExample.java
- FOUND commit: e0b9be6 (Task 1)
- FOUND commit: 4a0e527 (Task 2)
- mkdocs build --strict: PASSED

---
*Phase: 06-documentation-site*
*Completed: 2026-03-24*
