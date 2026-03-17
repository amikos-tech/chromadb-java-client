# Requirements: ChromaDB Java Client (v2 Stabilization)

**Defined:** 2026-03-17
**Core Value:** Java developers can integrate Chroma quickly and safely with a predictable, strongly-typed client that behaves consistently across environments.

## v1 Requirements

Requirements for the current stabilization release. Each maps to roadmap phases.

### API Parity

- [ ] **API-01**: User can manage tenants and databases through v2 client methods (create, get, list, delete) with typed results.
- [ ] **API-02**: User can manage collections with schema/config/CMEK options without lossy serialization.
- [ ] **API-03**: User can execute add/get/query/update/upsert/delete with full filter/include/queryTexts behavior expected by Chroma v2.
- [ ] **API-04**: User receives typed `ChromaException` subclasses mapped from HTTP status and server error details.

### Authentication

- [ ] **AUTH-01**: User can configure `BasicAuth`, `TokenAuth`, or `ChromaTokenAuth` once and have auth applied consistently across all API requests.
- [ ] **AUTH-02**: User can use cloud token auth flows and retrieve preflight identity context when required.
- [ ] **AUTH-03**: User gets actionable validation errors when auth inputs are missing or malformed.

### Embeddings & ID Generation

- [ ] **EMB-01**: User can use OpenAI, Cohere, HuggingFace, and Ollama embedding functions through one consistent embedding contract.
- [ ] **EMB-02**: User can use the default local embedding function without external model API keys.
- [ ] **EMB-03**: User can provide a custom embedding function, and runtime/descriptor precedence is deterministic and documented.
- [ ] **EMB-04**: User can generate deterministic or random IDs (`UUID`, `ULID`, `SHA-256`) for add/upsert flows with client-side validation.

### Quality & Release

- [ ] **QLTY-01**: Maintainer can run unit + integration tests against supported Chroma versions with reproducible pass/fail behavior.
- [ ] **QLTY-02**: Maintainer is protected from Java 8 and public interface compatibility regressions before release.
- [ ] **QLTY-03**: User can follow README examples for v2 auth, schema, collection lifecycle, and query workflows end-to-end.
- [ ] **QLTY-04**: Maintainer can produce Maven Central-ready artifacts (signed, checksummed, documented) through a repeatable release flow.

## v2 Requirements

Deferred to a future milestone. Tracked but not in current roadmap.

### Extended API Surface

- **ASYNC-01**: User can call non-blocking/reactive client APIs for high-concurrency workflows.

### Provider Expansion

- **EMB-05**: User can use Cloudflare Workers AI as an embedding provider.
- **EMB-06**: User can use Gemini/PaLM-family embedding provider integration when stable APIs are confirmed.

### Platform Integrations

- **OBS-01**: User can plug in metrics/tracing hooks for observability tooling.
- **DX-01**: User can bootstrap quickly with optional framework integration helpers (for example Spring-first starter patterns).

## Out of Scope

Explicitly excluded for this milestone.

| Feature | Reason |
|---------|--------|
| Chroma server feature development | This repository is client SDK only |
| Breaking Java 8 compatibility | Violates compatibility constraint for target users |
| Full reactive API redesign | High scope and architectural shift; defer until v2 stabilization is complete |
| Android/mobile-specific optimizations | Not required for current release goals |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| API-01 | TBD | Pending |
| API-02 | TBD | Pending |
| API-03 | TBD | Pending |
| API-04 | TBD | Pending |
| AUTH-01 | TBD | Pending |
| AUTH-02 | TBD | Pending |
| AUTH-03 | TBD | Pending |
| EMB-01 | TBD | Pending |
| EMB-02 | TBD | Pending |
| EMB-03 | TBD | Pending |
| EMB-04 | TBD | Pending |
| QLTY-01 | TBD | Pending |
| QLTY-02 | TBD | Pending |
| QLTY-03 | TBD | Pending |
| QLTY-04 | TBD | Pending |

**Coverage:**
- v1 requirements: 15 total
- Mapped to phases: 0
- Unmapped: 15 ⚠️

---
*Requirements defined: 2026-03-17*
*Last updated: 2026-03-17 after initial definition*
