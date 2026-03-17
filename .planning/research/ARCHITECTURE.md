# Architecture Research

**Domain:** Java vector database SDK architecture
**Researched:** 2026-03-17
**Confidence:** HIGH

## Standard Architecture

### System Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    Public Fluent API Layer                 │
├─────────────────────────────────────────────────────────────┤
│  Client/Collection interfaces + operation builders         │
│  (add/query/get/update/upsert/delete/tenant/database)      │
├─────────────────────────────────────────────────────────────┤
│                Transport + Serialization Layer             │
├─────────────────────────────────────────────────────────────┤
│  ChromaApiClient + ChromaApiPaths + Gson DTO mapping       │
│  + error translation (ChromaExceptions factory)            │
├─────────────────────────────────────────────────────────────┤
│              Provider + Utility Integration Layer          │
├─────────────────────────────────────────────────────────────┤
│  AuthProvider implementations, embedding functions,         │
│  ID generators, schema/config value objects                │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Public API (`Client`, `Collection`) | User-facing fluent operations and contracts | Interfaces + immutable value objects |
| API client transport | HTTP calls, retries/timeouts, request/response handling | OkHttp-based implementation |
| DTO and mapping model | Stable JSON boundary with server | `ChromaDtos` + Gson serialization |
| Exception model | Map transport/API errors into actionable Java exceptions | `ChromaExceptions` hierarchy |
| Embedding/auth extensions | Optional capability integration points | Strategy interfaces + provider implementations |

## Recommended Project Structure

```text
src/
├── main/java/tech/amikos/chromadb/v2/      # v2 public API + transport + models
├── main/java/tech/amikos/chromadb/          # legacy/shared client API compatibility
├── main/java/tech/amikos/chromadb/embeddings/ # embedding providers and contracts
└── test/java/tech/amikos/chromadb/          # unit + integration + compatibility tests
```

### Structure Rationale

- **`v2/`:** centralizes new public contract and transport behavior for current milestone focus.
- **`embeddings/`:** isolates provider-specific concerns from core API contract.
- **`test/`:** keeps contract and integration safeguards close to implementation changes.

## Architectural Patterns

### Pattern 1: Fluent Operation Builder

**What:** Operations are configured incrementally and executed explicitly with `execute()`.
**When to use:** Mutating/query operations with optional fields and validation.
**Trade-offs:** Strong ergonomics and discoverability; requires careful validation at terminal step.

**Example:**
```java
collection.query()
    .queryTexts("find relevant docs")
    .nResults(5)
    .execute();
```

### Pattern 2: Immutable Value Objects

**What:** Configuration and identifier objects are immutable and validated at construction.
**When to use:** Schema/config/auth settings crossing module boundaries.
**Trade-offs:** Safer state management; slightly more verbose builders.

### Pattern 3: Exception Translation Boundary

**What:** Keep HTTP/transport details internal and expose typed domain exceptions.
**When to use:** All server/network interactions.
**Trade-offs:** Cleaner caller logic; requires disciplined mapping maintenance.

## Data Flow

### Request Flow

```text
[User Call]
    ↓
[Fluent Builder]
    ↓
[ChromaApiClient]
    ↓
[OkHttp Request]
    ↓
[Chroma Server]
    ↓
[DTO Parse + Exception Translation]
    ↓
[Typed Result / ChromaException]
```

### Key Data Flows

1. **Collection operation flow:** builder parameters are validated, serialized, sent, parsed, then returned as typed results.
2. **Embedding flow:** runtime embedding source (explicit function or descriptor fallback) is resolved before query/add/upsert.
3. **Auth flow:** selected `AuthProvider` injects headers for every request path.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Single service / low throughput | Current synchronous design is sufficient |
| Moderate concurrent calls | Tune OkHttp pooling/timeouts and reduce redundant serialization |
| High-throughput ingestion | Optimize batch operation surfaces and ID generation strategy |

### Scaling Priorities

1. **First bottleneck:** serialization and HTTP request volume in high-churn ingestion paths.
2. **Second bottleneck:** integration-test runtime and environment variance across Chroma versions.

## Anti-Patterns

### Anti-Pattern 1: Leaking transport internals through public API

**What people do:** Expose HTTP-specific artifacts to end users.
**Why it's wrong:** Couples callers to transport implementation details.
**Do this instead:** Keep transport concerns behind typed operation/results APIs.

### Anti-Pattern 2: Embedding-resolution ambiguity

**What people do:** Mix runtime and descriptor embedding sources without precedence clarity.
**Why it's wrong:** Produces inconsistent behavior between environments.
**Do this instead:** Document and enforce deterministic precedence with explicit tests.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Chroma server/cloud | REST over HTTP via OkHttp | Version behavior matrix required |
| Embedding providers (OpenAI/Cohere/HF/Ollama) | Strategy-based provider functions | Surface provider failures clearly |
| Maven Central | Signed artifact publishing | Keep release plugins and metadata intact |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Public API ↔ transport | Method calls and DTO conversion | Avoid direct HTTP exposure |
| Transport ↔ exceptions | Status/body to typed exception mapping | Contract-critical for DX |
| Collection operations ↔ embedding resolver | Resolver interface | Must stay deterministic |

## Sources

- `CLAUDE.md` architecture overview
- `README.md` usage examples and compatibility notes
- Current package structure under `src/main/java/tech/amikos/chromadb`

---
*Architecture research for: Java ChromaDB client library*
*Researched: 2026-03-17*
