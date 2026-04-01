# Phase 4: Embedding Ecosystem - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-01
**Phase:** 04-embedding-ecosystem
**Areas discussed:** Sparse embedding interface, Multimodal embedding input, New dense provider strategy, Registry & reranking design

---

## Sparse Embedding Interface

### Interface relationship to EmbeddingFunction

| Option | Description | Selected |
|--------|-------------|----------|
| Separate interface (Recommended) | SparseEmbeddingFunction is its own interface with embedSparse → List<SparseVector>. Clean type separation. | ✓ |
| Shared base with generics | Generic BaseEmbeddingFunction<T>. More unified but Java 8 generics complexity. | |
| You decide | Claude picks best approach. | |

**User's choice:** Separate interface
**Notes:** Clean type boundary, mirrors Go client.

### Sparse embedding providers

| Option | Description | Selected |
|--------|-------------|----------|
| Cloud Splade only (Recommended) | Minimal deps, cloud credentials for tests. | |
| Cloud Splade + BM25 local | Both providers. BM25 needs tokenizer dependency. | ✓ |
| You decide | Claude picks based on Go client parity. | |

**User's choice:** Cloud Splade + BM25 local

### BM25 tokenizer approach

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror Go approach (Recommended) | Custom tokenizer: lowercase → regex → stopwords → Snowball → Murmur3. Cross-client compat. | ✓ |
| Mirror Go, pure Java hashing | Same pipeline but bundled Murmur3 impl instead of Guava. | |
| You decide | Claude picks minimal-dep approach. | |

**User's choice:** Mirror Go approach
**Notes:** User asked to review Go BM25 implementation first (https://github.com/amikos-tech/chroma-go/tree/main/pkg/embeddings/bm25). After reviewing, confirmed the full Go pipeline should be mirrored for cross-client index compatibility.

---

## Multimodal Embedding Input

### Interface design

| Option | Description | Selected |
|--------|-------------|----------|
| 2-tier (Recommended) | Keep EF as-is, add ContentEmbeddingFunction. Skip legacy MultimodalEF. | ✓ |
| 3-tier like Go | Port all three tiers including MultimodalEmbeddingFunction. | |
| You decide | Claude picks. | |

**User's choice:** 2-tier
**Notes:** User asked to research Go client's content embedding pattern first (https://github.com/amikos-tech/chroma-go/tree/main/pkg/embeddings). After review, confirmed skipping the legacy middle tier. Also commissioned Java adaptation research.

### Package placement

| Option | Description | Selected |
|--------|-------------|----------|
| embeddings.content sub-package (Recommended) | tech.amikos.chromadb.embeddings.content — clean separation. | ✓ |
| Flat in embeddings | Same package as EmbeddingFunction. Simpler but cluttered. | |
| In v2 package | Alongside SparseVector. Mixes concerns. | |

**User's choice:** embeddings.content sub-package

### Value type pattern

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — factories + builder (Recommended) | Content.text() for simple, .builder() for complex. Part/BinarySource static factories. | ✓ |
| You decide | Claude picks. | |

**User's choice:** Factories + builder matching codebase conventions

---

## New Dense Provider Strategy

### Dependency strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Vendor SDK for majors, OkHttp for rest (Recommended) | Google AI SDK for Gemini, AWS SDK for Bedrock. OkHttp for Voyage, Jina, etc. | ✓ |
| You decide | Claude picks. | |

**User's choice:** Vendor SDK for major labs, OkHttp for rest
**Notes:** User clarified: "for the major labs like Google, AWS I feel we should use their SDKs, those are mature and secure by virtue of being first-party. For the rest I think we can stick with OkHttp"

### Provider selection

| Option | Description | Selected |
|--------|-------------|----------|
| Gemini + Bedrock + Voyage | Per original requirements. Covers 3 biggest enterprise use cases. | ✓ |
| Gemini + Bedrock + Voyage + Jina | Add Jina as 4th. | |
| You decide | Claude picks. | |

**User's choice:** Gemini + Bedrock + Voyage

---

## Registry & Reranking Design

### Registry relationship to EmbeddingFunctionResolver

| Option | Description | Selected |
|--------|-------------|----------|
| Public registry wrapping resolver (Recommended) | New public API with register(name, factory). Resolver becomes internal. | ✓ |
| Expand resolver to public | Make existing class public directly. | |
| You decide | Claude picks. | |

**User's choice:** Public registry wrapping resolver

### Reranking provider

| Option | Description | Selected |
|--------|-------------|----------|
| Cohere Rerank (Recommended) | Cohere Rerank v3 API as first provider. | |
| Jina Reranker | Jina as first provider. | |
| Both Cohere + Jina | Ship two providers from the start. | ✓ |
| You decide | Claude picks. | |

**User's choice:** Both Cohere + Jina

### Registry scope

| Option | Description | Selected |
|--------|-------------|----------|
| All three types (Recommended) | Dense, sparse, and content. Separate register/resolve per type. | ✓ |
| Dense only, expand later | Start simple. | |
| You decide | Claude picks. | |

**User's choice:** All three types

### Registry alignment with Go client

| Option | Description | Selected |
|--------|-------------|----------|
| Align with Go (Recommended) | 3 maps, content fallback chain, same provider names. Singleton + instance. | ✓ |
| Simpler — single map | Single map, each entry declares types. | |
| You decide | Claude picks. | |

**User's choice:** Align with Go
**Notes:** User requested verification of registry pattern against Go client before finalizing. Research confirmed 4-map design (we use 3, skipping multimodal), content fallback chain, and provider name conventions.

### Registration API

| Option | Description | Selected |
|--------|-------------|----------|
| Instance method + static default (Recommended) | getDefault() singleton + custom instances for testing. | ✓ |
| Static-only global | All methods static. | |
| You decide | Claude picks. | |

**User's choice:** Instance method + static default

---

## Claude's Discretion

- Exact Snowball stemmer and Murmur3 library choices
- Exact vendor SDK artifact coordinates and versions
- Internal class organization within provider packages
- Registry factory functional interface signatures
- Closeable support for registry-resolved instances
- BM25 stop word list (match Go/Python defaults)

## Deferred Ideas

- CapabilityMetadata for provider capability declaration
- Closeable support in registry
- Additional providers beyond Gemini/Bedrock/Voyage
- MultimodalEmbeddingFunction middle-tier interface
