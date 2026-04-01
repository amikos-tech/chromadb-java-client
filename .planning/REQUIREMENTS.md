# Requirements: ChromaDB Java Client (Milestone 0.3.0 — Go Parity & Cloud)

**Defined:** 2026-03-20
**Core Value:** Java developers get Go-client-level API parity with advanced search, embedding ecosystem, and cloud integration testing.

## Milestone 0.3.0 Requirements

Requirements for the current milestone. Each maps to roadmap phases.

### Result Ergonomics

- [x] **ERGO-01**: User can iterate query/get results row-by-row via `ResultRow`, `rows()`, and `at(index)` instead of column-oriented access only.
- [x] **ERGO-02**: User can use typed `WhereDocument.contains()` and `WhereDocument.notContains()` helpers in get/query builders with correct serialization.

### Collection API Extensions

- [x] **COLL-01**: User can fork (copy) a collection via `collection.fork("newName")` and receive a new Collection reference.
- [x] **COLL-02**: User can check indexing progress via `collection.indexingStatus()` returning `IndexingStatus` with progress metrics (Chroma >= 1.4.1).
- [x] **COLL-03**: Cloud feature parity status for fork and indexing is explicitly audited, tested, and documented.

### Search API

- [x] **SEARCH-01**: User can execute `collection.search()` with KNN ranking (queryText, queryVector, querySparseVector) and get typed `SearchResult`.
- [x] **SEARCH-02**: User can compose RRF (Reciprocal Rank Fusion) from multiple weighted rank expressions with arithmetic combinators.
- [x] **SEARCH-03**: User can project specific fields (`#id`, `#document`, `#embedding`, `#score`, `#metadata`, custom keys) in search results.
- [x] **SEARCH-04**: User can group search results by metadata key with min/max K controls, and specify read level (INDEX_AND_WAL vs INDEX_ONLY).

### Embedding Ecosystem

- [x] **EMB-05**: User can use sparse embedding functions (BM25, Chroma Cloud Splade) through a `SparseEmbeddingFunction` interface.
- [x] **EMB-06**: User can use multimodal embedding functions (image+text) through a `MultimodalEmbeddingFunction` interface.
- [ ] **EMB-07**: User can use at least 3 additional dense embedding providers (Gemini, Bedrock, Voyage prioritized) through the existing `EmbeddingFunction` contract.
- [ ] **EMB-08**: User can rely on an `EmbeddingFunctionRegistry` to auto-wire embedding functions from server-side collection configuration.
- [ ] **RERANK-01**: User can rerank query results using a `RerankingFunction` interface with at least one provider (Cohere or Jina).

### Cloud Integration Testing

- [x] **CLOUD-01**: Cloud search parity tests cover pagination, IDIn/IDNotIn, document filters, metadata projection, and combined filter scenarios.
- [x] **CLOUD-02**: Cloud schema/index tests cover distance space variants, HNSW/SPANN config paths, invalid transitions, and schema round-trip assertions.
- [x] **CLOUD-03**: Cloud array metadata tests cover string/number/bool arrays, round-trip retrieval, and contains/not_contains filter behavior.

## Future Milestones (Post-0.3.0)

Deferred to future milestones.

### Advanced

- **LOCAL-01**: User can run Chroma in-process without a separate server (local/embedded mode via JNI/JNA). (#111)
- **ASYNC-01**: User can call non-blocking/reactive client APIs for high-concurrency workflows.

### Platform Integrations

- **OBS-01**: User can plug in metrics/tracing hooks for observability tooling.
- **DX-01**: User can bootstrap quickly with optional framework integration helpers (e.g. Spring starter).

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ERGO-01 | Phase 1 | Complete |
| ERGO-02 | Phase 1 | Complete |
| COLL-01 | Phase 2 | Complete |
| COLL-02 | Phase 2 | Complete |
| COLL-03 | Phase 2 | Complete |
| SEARCH-01 | Phase 3 | Complete |
| SEARCH-02 | Phase 3 | Complete |
| SEARCH-03 | Phase 3 | Complete |
| SEARCH-04 | Phase 3 | Complete |
| EMB-05 | Phase 4 | Complete |
| EMB-06 | Phase 4 | Complete |
| EMB-07 | Phase 4 | Pending |
| EMB-08 | Phase 4 | Pending |
| RERANK-01 | Phase 4 | Pending |
| CLOUD-01 | Phase 5 | Complete |
| CLOUD-02 | Phase 5 | Complete |
| CLOUD-03 | Phase 5 | Complete |

**Coverage:**
- 0.3.0 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-20*
