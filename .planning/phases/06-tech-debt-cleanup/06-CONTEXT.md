# Phase 6: Tech Debt Cleanup - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix documentation inaccuracies, CI workflow issues, remove dead code, and improve test robustness as identified by the v1.0 milestone audit. This phase addresses accumulated non-blocking tech debt — no new features or capabilities.

</domain>

<decisions>
## Implementation Decisions

### README doc fixes
- Fix v2 HuggingFaceEmbeddingFunction example (line 378): change `new HuggingFaceEmbeddingFunction(apiKey)` to `new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey))`
- Fix v1 HuggingFace examples (lines 627-675): update constructor calls to match actual API
- Fix Sha256IdGenerator docs (line 308): change "requires non-null documents" to "requires non-null document or non-null metadata" (matches Phase 3 metadata fallback)

### CI workflow fixes
- INFRA-1: Remove misleading `branches: ["main"]` filter from release.yml — GitHub Actions ignores it on `release:` events anyway
- INFRA-2: Add `make release-check` step to release.yml before deploy step — enforces CHANGELOG/README validation in CI, not just locally

### Dead code removal
- Wire `assumeMinVersion()` into integration tests that are version-sensitive — makes the helper useful instead of inert
- Identify which integration tests have version-dependent behavior and add appropriate `assumeMinVersion()` calls

### Test robustness
- Keep HuggingFace @Ignored tests as-is (document the deprecated Inference API endpoint)
- Investigate ND4J/ONNX dependency upgrade for macOS ARM64 compatibility — check if newer nd4j-native version resolves the `libjnind4jcpu.dylib` symbol error. If straightforward, include fix; if risky, document findings and defer

### Claude's Discretion
- Exact wording of updated README examples (match existing style)
- Which integration tests get `assumeMinVersion()` calls (based on actual version-dependent behavior)
- Whether ND4J upgrade is safe to include or should be deferred based on investigation

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Documentation
- `README.md` lines 370-404 — v2 HuggingFace examples (DOC-BUG-1)
- `README.md` lines 297-310 — ID generator documentation (DOC-BUG-2)
- `README.md` lines 627-675 — v1 HuggingFace full examples

### CI/Release
- `.github/workflows/release.yml` — Release workflow (INFRA-1: branches filter, INFRA-2: missing release-check)
- `Makefile` — `release-check` and `release-dry-run` targets

### Test infrastructure
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` line 127 — `assumeMinVersion()` helper
- `src/test/java/tech/amikos/chromadb/embeddings/hf/TestHuggingFaceEmbeddings.java` — @Ignored HF Inference API tests
- `pom.xml` — nd4j-native dependency version (currently 1.0.0-M2)

### Audit source
- `.planning/v1.0-MILESTONE-AUDIT.md` — Full audit with tech debt inventory

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `HuggingFaceEmbeddingFunction(WithParam...)` — correct constructor already exists, README just references wrong one
- `Sha256IdGenerator.serializeMetadata()` — Phase 3 added metadata fallback, docs just need to reflect it
- `assumeMinVersion()` with `Assume.assumeTrue` — already implemented correctly, just needs callers

### Established Patterns
- `Assume.assumeTrue` for conditional test skipping — just added for API keys and ND4J in commit c3224b6
- `WithParam` varargs pattern for embedding function construction — consistent across all providers
- `Makefile` targets chain: `release-check` validates before `release-dry-run` builds

### Integration Points
- `release.yml` step ordering: test → release-check → deploy (new step inserts between existing steps)
- Integration tests extend `AbstractChromaIntegrationTest` which provides `assumeMinVersion()` and container lifecycle

</code_context>

<specifics>
## Specific Ideas

No specific requirements — the audit items are well-defined. Apply fixes matching existing code style and conventions.

</specifics>

<deferred>
## Deferred Ideas

- ND4J dependency upgrade may be deferred to a separate investigation if the upgrade has ripple effects
- HuggingFace Inference API tests: if HF restores the endpoint in the future, un-ignore and test

</deferred>

---

*Phase: 06-tech-debt-cleanup*
*Context gathered: 2026-03-20*
