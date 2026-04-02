---
phase: 04-embedding-ecosystem
plan: 01
subsystem: embeddings
tags: [sparse-vectors, multimodal, content-types, adapter-pattern, embedding-function]

requires:
  - phase: 03-embeddings-id-extensibility
    provides: EmbeddingFunction interface, Embedding class, EFException, SparseVector value type
provides:
  - SparseEmbeddingFunction interface for sparse vector providers (BM25, SPLADE)
  - ContentEmbeddingFunction interface for multimodal embedding providers (Gemini, CLIP)
  - Content/Part/BinarySource value types for multimodal content representation
  - Modality and Intent enums for content typing and embedding intent hints
  - Bidirectional adapters (TextEmbeddingAdapter, ContentToTextAdapter)
affects: [04-embedding-ecosystem, embedding-providers, gemini-multimodal, bm25-sparse]

tech-stack:
  added: []
  patterns: [static-factory-plus-builder for Content, enum-with-fromValue for Modality/Intent, adapter pattern for EF bridging]

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/embeddings/SparseEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/ContentEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/TextEmbeddingAdapter.java
    - src/main/java/tech/amikos/chromadb/embeddings/ContentToTextAdapter.java
    - src/main/java/tech/amikos/chromadb/embeddings/content/Content.java
    - src/main/java/tech/amikos/chromadb/embeddings/content/Part.java
    - src/main/java/tech/amikos/chromadb/embeddings/content/BinarySource.java
    - src/main/java/tech/amikos/chromadb/embeddings/content/Modality.java
    - src/main/java/tech/amikos/chromadb/embeddings/content/Intent.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestSparseEmbeddingFunction.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestContentEmbeddingFunction.java
    - src/test/java/tech/amikos/chromadb/embeddings/content/TestContentTypes.java
  modified: []

key-decisions:
  - "SparseEmbeddingFunction is a separate interface (not extending EmbeddingFunction) per D-01 — sparse and dense have incompatible return types"
  - "ContentEmbeddingFunction uses static factory fromTextOnly() rather than constructor for adapter creation — follows interface-first design"
  - "Content value types placed in embeddings.content subpackage per D-09 — keeps embeddings package clean"

patterns-established:
  - "Static factory + builder for content types: Content.text() for simple, Content.builder() for complex"
  - "Enum with getValue()/fromValue(String) pattern for Modality and Intent (same as DistanceFunction)"
  - "Bidirectional adapter pattern: TextEmbeddingAdapter (text->content) and ContentToTextAdapter (content->text)"

requirements-completed: [EMB-05, EMB-06]

duration: 3min
completed: 2026-04-01
---

# Phase 04 Plan 01: Embedding Foundation Interfaces Summary

**Sparse and content-based embedding interfaces with multimodal Content type hierarchy and bidirectional adapters**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-01T12:39:27Z
- **Completed:** 2026-04-01T12:42:05Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- SparseEmbeddingFunction interface for sparse vector providers (BM25, SPLADE) returning SparseVector
- ContentEmbeddingFunction interface with embedContents(List<Content>) for multimodal providers
- Complete Content type hierarchy: Content, Part, BinarySource value types with static factories and builder
- Modality (TEXT/IMAGE/AUDIO/VIDEO) and Intent (RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY/CLASSIFICATION/CLUSTERING) enums
- Bidirectional adapters enabling interop between text-only and content embedding functions
- 23 unit tests covering all interfaces, value types, adapters, enums, and edge cases

## Task Commits

Each task was committed atomically:

1. **Task 1: Create interfaces, content types, and adapters** - `53e4fc2` (feat)
2. **Task 2: Unit tests for interfaces, content types, and adapters** - `4d0aeac` (test)

## Files Created/Modified
- `src/main/java/tech/amikos/chromadb/embeddings/SparseEmbeddingFunction.java` - Sparse embedding interface returning SparseVector
- `src/main/java/tech/amikos/chromadb/embeddings/ContentEmbeddingFunction.java` - Content embedding interface with fromTextOnly factory
- `src/main/java/tech/amikos/chromadb/embeddings/TextEmbeddingAdapter.java` - Wraps EmbeddingFunction as ContentEmbeddingFunction
- `src/main/java/tech/amikos/chromadb/embeddings/ContentToTextAdapter.java` - Wraps ContentEmbeddingFunction as EmbeddingFunction
- `src/main/java/tech/amikos/chromadb/embeddings/content/Content.java` - Multimodal content with static factory and builder
- `src/main/java/tech/amikos/chromadb/embeddings/content/Part.java` - Typed content parts (text, image, audio, video)
- `src/main/java/tech/amikos/chromadb/embeddings/content/BinarySource.java` - Binary content source (URL, file, base64, bytes)
- `src/main/java/tech/amikos/chromadb/embeddings/content/Modality.java` - TEXT/IMAGE/AUDIO/VIDEO enum
- `src/main/java/tech/amikos/chromadb/embeddings/content/Intent.java` - RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY/CLASSIFICATION/CLUSTERING enum
- `src/test/java/tech/amikos/chromadb/embeddings/TestSparseEmbeddingFunction.java` - 2 tests
- `src/test/java/tech/amikos/chromadb/embeddings/TestContentEmbeddingFunction.java` - 6 tests
- `src/test/java/tech/amikos/chromadb/embeddings/content/TestContentTypes.java` - 15 tests

## Decisions Made
- SparseEmbeddingFunction is a separate interface (not extending EmbeddingFunction) per D-01 -- sparse and dense have incompatible return types
- ContentEmbeddingFunction uses static factory fromTextOnly() rather than constructor for adapter creation -- follows interface-first design
- Content value types placed in embeddings.content subpackage per D-09 -- keeps embeddings package clean

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None - all interfaces and types are fully implemented.

## Next Phase Readiness
- Foundation interfaces ready for concrete provider implementations (BM25, Gemini multimodal)
- Content type hierarchy ready for embedding function registry integration
- Adapters enable gradual migration from text-only to content-based embedding functions

---
*Phase: 04-embedding-ecosystem*
*Completed: 2026-04-01*
