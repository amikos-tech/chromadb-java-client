---
phase: 04-embedding-ecosystem
plan: 05
status: complete
started: 2026-04-01T16:00:00Z
completed: 2026-04-01T16:10:00Z
duration: ~10min
tasks_completed: 2
tasks_total: 2
---

# Plan 04-05 Summary: EmbeddingFunctionRegistry with Auto-Wiring

## What Was Built

Public `EmbeddingFunctionRegistry` with three factory maps (dense, sparse, content) that replaces hardcoded provider dispatch with an extensible registry pattern.

## Key Decisions

- EmbeddingFunctionResolver.resolve() now delegates to EmbeddingFunctionRegistry.getDefault().resolveDense()
- buildParams/buildHuggingFaceParams made public in EmbeddingFunctionResolver for registry access across packages
- Gemini/Bedrock registration guarded by try-catch NoClassDefFoundError for optional SDK jars
- Content fallback chain: content factory first → dense + TextEmbeddingAdapter wrapping
- All register/resolve methods synchronized for thread safety

## Key Files

### Created
- `src/main/java/tech/amikos/chromadb/embeddings/EmbeddingFunctionRegistry.java` — Public registry with 3 factory maps and singleton
- `src/test/java/tech/amikos/chromadb/embeddings/TestEmbeddingFunctionRegistry.java` — Registry tests including thread safety

### Modified
- `src/main/java/tech/amikos/chromadb/v2/EmbeddingFunctionResolver.java` — Delegates to registry, helpers made public

## Self-Check: PASSED

- [x] EmbeddingFunctionRegistry.getDefault() returns singleton with all built-in providers
- [x] registerDense/registerSparse/registerContent + resolveDense/resolveSparse/resolveContent work
- [x] Content fallback chain (dense + adapter) works
- [x] ChromaHttpCollection uses registry path (via EmbeddingFunctionResolver delegation)
- [x] Thread safety verified with concurrent test
- [x] All tests pass

## Deviations

None.
