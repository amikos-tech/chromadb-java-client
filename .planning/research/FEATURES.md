# Feature Research

**Domain:** Java SDK for vector database (Chroma)
**Researched:** 2026-03-17
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Configurable endpoint client bootstrap | Every SDK must connect predictably to target server/cloud | LOW | Builder-based setup with clear defaults |
| Auth provider support (basic/bearer/chroma-token) | Real deployments require auth from day one | MEDIUM | Must apply consistently to all requests |
| Collection lifecycle operations | Core object management expectation | MEDIUM | Create/list/get/delete/reset parity required |
| CRUD + query operations on collection records | Primary reason to use vector DB client | HIGH | Includes filters, include options, and batch handling |
| Typed error mapping by HTTP class | Users need actionable failure semantics | MEDIUM | Avoid opaque generic exceptions |
| Integration-test coverage against real Chroma | SDK credibility depends on behavior parity | MEDIUM | Testcontainers matrix is expected |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Type-safe fluent v2 API surface | Faster onboarding and fewer misuse paths | MEDIUM | Builder ergonomics are key for Java users |
| Runtime + descriptor-based embedding resolution | Flexible deployment models | HIGH | Must be deterministic and documented |
| Deterministic client-side ID generators | Reproducible ingestion pipelines | MEDIUM | Helpful for upsert/idempotent workflows |
| Local default embeddings (ONNX) | Works without external model API keys | HIGH | Strong adoption lever for offline or cost-sensitive usage |
| Schema/CMEK-first collection configuration | Better enterprise readiness | HIGH | Needs strict serialization/deserialization guarantees |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Hiding all HTTP/API semantics behind magic | Users want less configuration | Obscures debuggability and causes surprise behavior | Keep fluent API explicit with documented defaults |
| Large “all-in-one” convenience method for full lifecycle | Feels quick for demos | Couples concerns and hurts testability | Keep composable operations with narrow responsibilities |
| Reactive API bolted on mid-stabilization | Perceived modernity | Splits focus during stability push | Defer reactive surface to a dedicated milestone |
| Silent auth fallback behavior | Avoids setup friction | Security and debugging hazards | Fail fast with explicit auth errors |

## Feature Dependencies

```text
Collection lifecycle + auth
    └──requires──> Stable HTTP transport + DTO mapping
                          └──requires──> Typed error model

Query/get advanced options
    └──requires──> Schema/config parity + filter DSL

Embedding provider flexibility
    └──requires──> Resolver precedence rules + robust tests
```

### Dependency Notes

- **Collection operations require stable transport/error mapping:** without predictable HTTP and exception handling, every feature becomes brittle.
- **Advanced query/get requires schema and filter parity:** response shaping must match server behavior to avoid client-side bugs.
- **Embedding flexibility requires resolver precedence rules:** ambiguous source-of-truth causes hard-to-debug runtime failures.

## MVP Definition

### Launch With (v1)

Minimum viable product for stable v2 adoption.

- [ ] Full v2 endpoint parity for critical lifecycle + record operations — required for trust
- [ ] Auth behavior consistency across cloud/self-hosted paths — required for production
- [ ] Deterministic embedding + ID workflows with robust tests — required for reliable ingestion
- [ ] Compatibility and docs polish for release confidence — required for adoption

### Add After Validation (v1.x)

- [ ] Additional provider integrations (for example Cloudflare Workers AI) — after core parity is stable
- [ ] Expanded convenience APIs for common patterns — after usage telemetry/feedback

### Future Consideration (v2+)

- [ ] Reactive/async API surface — separate design and compatibility milestone
- [ ] Deep observability hooks (metrics/tracing interfaces) — once baseline usage scales

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| v2 parity completion | HIGH | HIGH | P1 |
| Auth hardening | HIGH | MEDIUM | P1 |
| Embedding/ID determinism | HIGH | MEDIUM | P1 |
| Documentation/release polish | MEDIUM | MEDIUM | P2 |
| New provider additions | MEDIUM | MEDIUM | P2 |
| Reactive API | LOW (current audience) | HIGH | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Chroma Python Client | Chroma JS Client | Our Approach |
|---------|----------------------|------------------|--------------|
| API parity velocity | Usually first-party reference behavior | Fast-moving for web ecosystem | Prioritize compatible semantics in Java idioms |
| Onboarding ergonomics | Strong docs/examples | Strong docs/examples | Improve fluent examples and defaults in README |
| Embedding integrations | Broad ecosystem patterns | Broad ecosystem patterns | Keep provider coverage plus local default path |

## Sources

- `README.md` feature checklist and TODOs
- `CLAUDE.md` v2 architecture notes
- Current test suite under `src/test/java/tech/amikos/chromadb/v2`

---
*Feature research for: Java ChromaDB client library*
*Researched: 2026-03-17*
