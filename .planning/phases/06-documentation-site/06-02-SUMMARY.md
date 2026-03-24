---
phase: 06-documentation-site
plan: 02
subsystem: docs
tags: [mkdocs, mkdocs-material, pymdownx.snippets, java, documentation, embeddings, filtering, search]

# Dependency graph
requires:
  - phase: 06-01
    provides: MkDocs scaffold with snippet infrastructure and 12 guide page stubs
provides:
  - client.md — Client Setup guide with self-hosted, cloud, lifecycle, embedding function, and health sections
  - auth.md — Authentication guide covering Basic, Token, ChromaToken, and Cloud auth methods
  - records.md — Records operations guide covering all CRUD operations and row-based result access
  - filtering.md — Filtering guide covering full Where and WhereDocument DSL
  - search.md — Search API guide covering KNN, RRF, GroupBy, ReadLevel, batch, and result access
  - embeddings.md — Embedding providers guide covering all 5 providers with WithParam factory usage
  - 6 companion Java snippet files in docs/docs/assets/snippets/ with named section markers
affects: [06-03, 06-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "pymdownx.snippets named sections: --8<-- [start:name] / --8<-- [end:name] markers in .java files"
    - "--8<-- \"FileName.java:section-name\" inclusion syntax in .md files"

key-files:
  created:
    - docs/docs/assets/snippets/ClientExample.java
    - docs/docs/assets/snippets/AuthExample.java
    - docs/docs/assets/snippets/RecordsExample.java
    - docs/docs/assets/snippets/FilteringExample.java
    - docs/docs/assets/snippets/SearchExample.java
    - docs/docs/assets/snippets/EmbeddingsExample.java
  modified:
    - docs/docs/client.md
    - docs/docs/auth.md
    - docs/docs/records.md
    - docs/docs/filtering.md
    - docs/docs/search.md
    - docs/docs/embeddings.md

key-decisions:
  - "All snippet files use named section markers for granular inclusion — no full-file includes"
  - "SearchExample uses Search.builder() + collection.search().searches(...) pattern (not convenience queryText shortcut) to show the full API surface"
  - "FilteringExample separates metadata-contains (array containment) from document-inline (Cloud-oriented #document) and id-filter sections to match Where DSL structure"
  - "EmbeddingsExample uses WithParam factory methods throughout per Phase 06-tech-debt-cleanup decision"

patterns-established:
  - "Guide pages follow consistent structure: overview -> sections with snippet includes -> admonitions for notes/tips/warnings"
  - "All code examples v2 API only: no tech.amikos.chromadb.Client v1 imports anywhere in snippet files"

requirements-completed: [DOC-02, DOC-03]

# Metrics
duration: 4min
completed: 2026-03-24
---

# Phase 06 Plan 02: Core Guide Pages Summary

**6 core guide pages (client, auth, records, filtering, search, embeddings) with 6 compilable Java snippet files using pymdownx.snippets named section markers — all v2 API, MkDocs strict build passes**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-24T15:33:12Z
- **Completed:** 2026-03-24T15:37:36Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- 6 guide pages populated from stub placeholders to rich content with pymdownx.snippets inclusions
- 6 companion `.java` snippet files covering 45+ named sections total
- Filtering page covers all Where operators (eq, ne, gt/gte/lt/lte, in/nin, contains/notContains, idIn/idNotIn, documentContains, and/or) and WhereDocument (contains, notContains, regex, notRegex, and/or)
- Search page covers KNN (text/embedding/filtered), RRF, GroupBy, ReadLevel, batch search, and result row access
- Embeddings page covers all 5 providers with WithParam factory methods and embedding function precedence

## Task Commits

Each task was committed atomically:

1. **Task 1: Create client, auth, and records guide pages with snippet files** - `d204e40` (feat)
2. **Task 2: Create filtering, search, and embeddings guide pages with snippet files** - `94c5fc3` (feat)

**Plan metadata:** (to be added in final commit)

## Files Created/Modified
- `docs/docs/assets/snippets/ClientExample.java` — 5 named sections: self-hosted, cloud, lifecycle, with-ef, health
- `docs/docs/assets/snippets/AuthExample.java` — 4 named sections: basic, token, chroma-token, cloud
- `docs/docs/assets/snippets/RecordsExample.java` — 11 named sections: add-docs, add-embeddings, query-text, query-filter, query-embedding, get, update, upsert, delete, count, row-access
- `docs/docs/assets/snippets/FilteringExample.java` — 11 named sections covering all Where and WhereDocument operators
- `docs/docs/assets/snippets/SearchExample.java` — 8 named sections: knn-text, knn-embedding, knn-with-filter, rrf, group-by, read-level, search-result, batch-search
- `docs/docs/assets/snippets/EmbeddingsExample.java` — 6 named sections: default, openai, cohere, huggingface, huggingface-hfei, ollama
- `docs/docs/client.md` — Client Setup guide (5 snippet sections)
- `docs/docs/auth.md` — Authentication guide (4 snippet sections, IllegalStateException warning)
- `docs/docs/records.md` — Records guide (11 snippet sections, row-access tip)
- `docs/docs/filtering.md` — Filtering guide (11 snippet sections, WhereDocument warning about Cloud vs local)
- `docs/docs/search.md` — Search API guide (8 snippet sections, Chroma >= 1.5 note)
- `docs/docs/embeddings.md` — Embeddings guide (6 snippet sections, provider comparison table, precedence section)

## Decisions Made
- `SearchExample` uses `Search.builder() + collection.search().searches(...)` pattern rather than
  the convenience `queryText()` shortcut on `SearchBuilder` — this exposes the full `Knn`/`Rrf`/
  `Search` type hierarchy which is the most instructive pattern for users.
- `FilteringExample` separates `contains`/`notContains` (array-valued metadata) from
  `documentContains` (inline `#document` Cloud filter) into distinct sections so the warning about
  Cloud-only semantics is clearly co-located.

## Deviations from Plan

None — plan executed exactly as written. All 6 pages and 6 snippet files created per specification.

## Issues Encountered

None.

## Known Stubs

None — all 6 guide pages are fully populated. No placeholder text or empty data sources.

## Next Phase Readiness
- Plans 03 (cloud-features, schema, id-generators, transport, logging, migration) can populate remaining 6 guide page stubs without touching mkdocs.yml
- All snippet infrastructure is established and working — add more `.java` files to `docs/docs/assets/snippets/` as needed
- `mkdocs build --strict` passes with 0 warnings for all 6 new pages

## Self-Check: PASSED

- FOUND: docs/docs/assets/snippets/ClientExample.java
- FOUND: docs/docs/assets/snippets/AuthExample.java
- FOUND: docs/docs/assets/snippets/RecordsExample.java
- FOUND: docs/docs/assets/snippets/FilteringExample.java
- FOUND: docs/docs/assets/snippets/SearchExample.java
- FOUND: docs/docs/assets/snippets/EmbeddingsExample.java
- FOUND: docs/docs/client.md (contains # Client Setup and snippet inclusions)
- FOUND: docs/docs/auth.md (contains # Authentication and snippet inclusions)
- FOUND: docs/docs/records.md (contains # Records and snippet inclusions)
- FOUND: docs/docs/filtering.md (contains # Filtering and snippet inclusions)
- FOUND: docs/docs/search.md (contains # Search and snippet inclusions)
- FOUND: docs/docs/embeddings.md (contains # Embeddings and snippet inclusions)
- FOUND commit: d204e40 (Task 1)
- FOUND commit: 94c5fc3 (Task 2)

---
*Phase: 06-documentation-site*
*Completed: 2026-03-24*
