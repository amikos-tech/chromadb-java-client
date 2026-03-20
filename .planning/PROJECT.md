# ChromaDB Java Client (Milestone 0.3.0 — Go Parity & Cloud)

## What This Is

This project is a production-focused Java client for the Chroma vector database API (`>=1.0.0`), centered on the `tech.amikos.chromadb.v2` interface-first API. It enables Java teams to manage tenants, databases, collections, and vector operations with a fluent, type-safe API while supporting common embedding providers. Milestone `0.3.0` extends the stable v2 foundation with Go-client API parity, advanced Search API, embedding ecosystem expansion, and cloud integration testing.

## Core Value

Java developers can integrate Chroma quickly and safely with a predictable, strongly-typed client that behaves consistently across environments.

## Requirements

### Validated

- ✓ Java client published to Maven Central and usable in external projects — existing
- ✓ Core collection lifecycle and record operations available in v2 (`add`, `get`, `query`, `update`, `upsert`, `delete`) — existing
- ✓ Multi-provider embedding integrations implemented (Default, OpenAI, Cohere, HuggingFace, Ollama) — existing
- ✓ Integration-test strategy with Testcontainers against Chroma server images — existing
- ✓ Typed exception hierarchy and auth-provider abstractions exist in v2 package — existing

### Active (Milestone 0.3.0)

- [ ] Search API with ranking expressions, field projection, groupBy, read levels (#105, #126)
- [x] Row-based result access pattern across get/query/search results (#104) — Validated in Phase 1: Result Ergonomics & WhereDocument
- [ ] Collection.fork and Collection.indexingStatus cloud APIs (#99, #100, #131)
- [x] WhereDocument contains/notContains end-to-end implementation (#128) — Validated in Phase 1: Result Ergonomics & WhereDocument
- [ ] Sparse and multimodal embedding function interfaces (#106)
- [ ] Reranking function interface and providers (#107)
- [ ] Additional embedding providers — Gemini, Bedrock, Voyage, Mistral, Jina, Together, Nomic (#108)
- [ ] Embedding function registry for auto-wiring from server config (#109)
- [ ] Cloud integration test parity suites — search, schema/index, array metadata (#127, #129, #130)

### Out of Scope

- Reactive/async API surface — defer until synchronous parity is complete
- Dropping Java 8 compatibility — ecosystem compatibility is a core adoption constraint
- Building Chroma server-side functionality — this repository focuses on client behavior only
- Local/embedded client mode (#111) — large effort, may warrant separate module
- Mobile/Android-specific optimizations

## Context

The repository already contains substantial v2 implementation under `src/main/java/tech/amikos/chromadb/v2`, plus embedding adapters and both unit/integration tests. README and code comments indicate an active rewrite toward Chroma API v2 and a focus on production ergonomics (fluent builders, typed value objects, explicit error mapping). Existing tests exercise API paths, schema/config handling, ID generation, and auth providers, providing a strong baseline for incremental hardening rather than a net-new greenfield build.

## Constraints

- **Compatibility**: Java 8 runtime support must be preserved — broad enterprise/JVM compatibility target
- **API Contract**: Chroma server compatibility target is `>=1.0.0` — avoid regressions across supported versions
- **Scope**: Only Chroma v2 API is supported in milestone `0.2.0` — do not add or retain v1 API compatibility
- **Distribution**: Maven Central publishing requirements (signing/checksums/release metadata) must remain intact — release trust and adoption
- **Architecture**: Keep v2 interface-first, fluent API style — consistency with current public surface
- **Testability**: Integration tests depend on containerized Chroma and optional external API keys — CI stability must be managed explicitly

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Treat the project as brownfield with validated capabilities | Existing code and tests already prove core value | — Pending |
| Prioritize API parity and reliability over net-new client paradigms | Stable v2 adoption is the immediate outcome needed | — Pending |
| Support Chroma v2 only in milestone `0.2.0` | Reduces ambiguity and avoids split investment on deprecated API surface | — Pending |
| Keep Java 8 + synchronous API as hard constraints for this milestone | Minimizes adoption friction and migration risk | — Pending |

---
*Last updated: 2026-03-20 — Phase 1 complete (ERGO-01, ERGO-02). Row-based result access and WhereDocument DSL shipped. Next: Phase 2 (Collection API Extensions).*
