# Roadmap: ChromaDB Java Client (Milestone 0.3.0 — Go Parity & Cloud)

## Overview

This roadmap defines milestone `0.3.0` which extends the stable v2 Java client (shipped in 0.2.0) with Go-client API parity, advanced Search API support, embedding ecosystem expansion, and cloud integration testing. The goal is to make the Java client a first-class citizen alongside the Go client for both self-hosted and Chroma Cloud deployments.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3...): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Result Ergonomics & WhereDocument** — Add row-based result access and complete WhereDocument typed helpers.
- [x] **Phase 2: Collection API Extensions** — Add Collection.fork, Collection.indexingStatus, and cloud feature parity audit.
- [x] **Phase 3: Search API** — Implement the Search endpoint with ranking expressions, field projection, groupBy, and read levels. (completed 2026-03-22)
- [ ] **Phase 4: Embedding Ecosystem** — Add sparse/multimodal interfaces, reranking, new providers, and embedding registry.
- [ ] **Phase 5: Cloud Integration Testing** — Build cloud parity test suites for search, schema/index, and array metadata. (gap closure in progress)
- [x] **Phase 6: Documentation Site** — Build a rich documentation site with API surfaces, examples, and feature guides (similar to chroma-go docs). (completed 2026-03-24)
- [ ] **Phase 7: Working Examples** — Add full working examples for all major features (similar to chroma-go examples/).

## Phase Details

### Phase 1: Result Ergonomics & WhereDocument
**Goal:** Give users row-based iteration on query/get results and complete the WhereDocument typed filter helpers, improving daily-use ergonomics.
**Depends on:** Nothing (first phase)
**Requirements:** [ERGO-01, ERGO-02]
**Issues:** #104, #128
**Success Criteria** (what must be TRUE):
  1. User can iterate query/get results row-by-row via `ResultRow`, `rows()`, and `at(index)`.
  2. User can use `WhereDocument.contains()` and `WhereDocument.notContains()` in get/query builders with correct serialization.
  3. Unit and integration tests validate both features end-to-end.
**Plans:** 2/3 plans executed

Plans:
- [x] 01-01-PLAN.md — Create ResultRow, QueryResultRow, ResultGroup type hierarchy with impls and unit tests
- [x] 01-02-PLAN.md — Wire row access into QueryResult/GetResult and add integration tests
- [x] 01-03-PLAN.md — Implement WhereDocument typed operators and replace stubs with real tests

### Phase 2: Collection API Extensions
**Goal:** Add cloud-relevant collection operations (fork, forkCount, indexing status) and audit cloud feature parity for all v2 operations.
**Depends on:** Phase 1
**Requirements:** [COLL-01, COLL-02, COLL-03]
**Issues:** #99, #100, #131
**Success Criteria** (what must be TRUE):
  1. User can fork a collection via `collection.fork("newName")`.
  2. User can check indexing progress via `collection.indexingStatus()` returning progress metrics.
  3. Cloud feature parity status for fork and indexing is explicitly documented (supported, partial, or unsupported).
**Plans:** 2/2 plans executed

Plans:
- [x] 02-01-PLAN.md — Implement fork/forkCount/indexingStatus core (interfaces, DTOs, paths, HTTP impl, WireMock tests)
- [x] 02-02-PLAN.md — Cloud/integration tests, Javadoc Availability tags, README parity table, CHANGELOG

### Phase 3: Search API
**Goal:** Implement the Chroma Search endpoint (v1.5+) with full ranking expression DSL, field projection, groupBy, and read levels — matching Go client capabilities.
**Depends on:** Phase 1 (ResultRow used in SearchResult)
**Requirements:** [SEARCH-01, SEARCH-02, SEARCH-03, SEARCH-04]
**Issues:** #105, #126
**Success Criteria** (what must be TRUE):
  1. User can execute `collection.search()` with KNN ranking expressions and get typed results.
  2. User can compose RRF (Reciprocal Rank Fusion) from multiple weighted rank expressions.
  3. User can project specific fields (id, document, embedding, score, metadata keys) in search results.
  4. User can group results by metadata key with min/max K controls.
  5. User can specify read level (INDEX_AND_WAL vs INDEX_ONLY).
  6. Integration tests validate search against Chroma >= 1.5.
**Plans:** 3/3 plans complete

Plans:
- [x] 03-01-PLAN.md — Create Search API value types, ranking builders, result interfaces, and SearchBuilder on Collection
- [x] 03-02-PLAN.md — Implement Search DTOs, HTTP wiring, result converters, and SearchBuilderImpl
- [x] 03-03-PLAN.md — Create unit tests, integration tests, and update PublicInterfaceCompatibilityTest

### Phase 4: Embedding Ecosystem
**Goal:** Expand the embedding ecosystem with sparse/multimodal interfaces, reranking functions, additional providers, and an auto-wiring registry.
**Depends on:** Nothing (independent of Phases 1-3)
**Requirements:** [EMB-05, EMB-06, EMB-07, EMB-08, RERANK-01]
**Issues:** #106, #107, #108, #109
**Success Criteria** (what must be TRUE):
  1. SparseEmbeddingFunction and MultimodalEmbeddingFunction interfaces exist with at least one provider each.
  2. RerankingFunction interface exists with at least one provider (Cohere or Jina).
  3. At least 3 new dense embedding providers implemented (prioritize Gemini, Bedrock, Voyage).
  4. EmbeddingFunctionRegistry supports registering and auto-wiring providers from server-side collection config.
  5. All providers have unit tests; integration tests where API keys are available.
**Plans:** TBD

### Phase 5: Cloud Integration Testing
**Goal:** Build deterministic cloud parity test suites that validate search, schema/index, and array metadata behavior against Chroma Cloud.
**Depends on:** Phases 2, 3 (cloud APIs and search must be implemented first)
**Requirements:** [CLOUD-01, CLOUD-02, CLOUD-03]
**Issues:** #127, #129, #130
**Success Criteria** (what must be TRUE):
  1. Cloud search parity tests cover pagination, IDIn/IDNotIn, document filters, metadata projection, combined filters.
  2. Cloud schema/index tests cover distance space variants, HNSW/SPANN config, invalid transitions, round-trip assertions.
  3. Cloud array metadata tests cover string/number/bool arrays, round-trip retrieval, contains/not_contains filters.
  4. Test suite can run in CI with cloud credentials or be skipped gracefully without them.
**Plans:** 3/3 plans complete

Plans:
- [x] 05-01-PLAN.md — Schema/index + array metadata cloud tests, mixed-type array client validation
- [x] 05-02-PLAN.md — Search parity cloud tests (KNN, RRF, GroupBy, batch, pagination, filters, projection, read levels)
- [x] 05-03-PLAN.md — Gap closure: fix embedding projection assertion and WAL read-level test target

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5
Phase 4 can execute in parallel with Phases 1-3 (independent).

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Result Ergonomics & WhereDocument | 2/3 | In Progress|  |
| 2. Collection API Extensions | 2/2 | Complete | 2026-03-21 |
| 3. Search API | 3/3 | Complete   | 2026-03-22 |
| 4. Embedding Ecosystem | 0/TBD | Pending | — |
| 5. Cloud Integration Testing | 2/3 | In Progress|  |

### Phase 6: Documentation Site

**Goal:** Build a rich documentation site (similar to amikos-tech/chroma-go) covering all library features, API surfaces, and usage examples, deployed to java.chromadb.dev via GitHub Pages.
**Requirements**: [DOC-01, DOC-02, DOC-03, DOC-04, DOC-05, DOC-06]
**Depends on:** Phases 1-5 (documents features built in earlier phases)
**Success Criteria** (what must be TRUE):
  1. MkDocs Material site builds with `--strict` flag without errors.
  2. All 12 guide pages have rich content with snippet-included Java code examples (v2 API only).
  3. GitHub Actions workflow deploys MkDocs + Javadoc to GitHub Pages on push to main.
  4. Custom domain java.chromadb.dev configured via CNAME file.
  5. Examples section stubbed with 7 topic directories for Phase 7.
  6. User visually approves the site via local `mkdocs serve`.
**Plans:** 4/4 plans complete

Plans:
- [x] 06-01-PLAN.md — MkDocs scaffold, config, homepage, CI workflow, Javadoc plugin upgrade
- [x] 06-02-PLAN.md — Core guide pages (client, auth, records, filtering, search, embeddings) with snippet files
- [x] 06-03-PLAN.md — Advanced guide pages (cloud-features, schema, id-generators, transport, logging, migration) with snippet files
- [x] 06-04-PLAN.md — Examples section stubs, nav expansion, and visual verification checkpoint

### Phase 7: Working Examples

**Goal:** Add full working examples for all major features, organized by topic, runnable standalone — similar to amikos-tech/chroma-go examples/.
**Requirements**: TBD
**Depends on:** Phases 1-5 (examples demonstrate features built in earlier phases)
**Plans:** 0 plans

Plans:
- [ ] TBD (run /gsd:plan-phase 7 to break down)
