# Phase 7: Fix README OpenAI/Cohere Embedding Examples - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix non-compiling README embedding function examples for OpenAI and Cohere providers to use the correct `WithParam` constructor pattern. This closes the last 2 partial requirements (EMB-01, QLTY-03) from the v1.0 milestone audit. No new features or capabilities — documentation fixes only.

</domain>

<decisions>
## Implementation Decisions

### v2 OpenAI example (line 340)
- Fix constructor to `new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))`
- Show explicit model selection — demonstrates the `WithParam` pattern fully
- Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block

### v2 Cohere example (line 359)
- Fix constructor to `new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"))`
- Show explicit model selection — consistent with OpenAI decision above
- Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block

### v1 appendix OpenAI example (line 568)
- Fix constructor to `new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))`
- Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block (v1 examples are full class files)

### v1 appendix Cohere example (line 604)
- Fix constructor to `new CohereEmbeddingFunction(WithParam.apiKey(apiKey))` (apiKey only, no explicit model — matches original minimal intent)
- Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block

### Import consistency sweep
- Add `import tech.amikos.chromadb.embeddings.WithParam;` to v2 HuggingFace example (line ~375) and HFEI example (line ~399) — both already use WithParam but don't show the import
- All v2 embedding examples become copy-paste friendly with correct imports

### Requirement closure
- Constructor fixes alone are sufficient to close EMB-01 (consistent embedding contract) and QLTY-03 (README examples work end-to-end)
- No additional content or restructuring needed beyond the constructor and import fixes

### Claude's Discretion
- Exact line placement of WithParam import within existing import blocks (alphabetical or grouped)
- Whether to adjust whitespace/formatting to match surrounding code style

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Documentation targets
- `README.md` lines 331-348 — v2 OpenAI embedding example (broken constructor)
- `README.md` lines 350-367 — v2 Cohere embedding example (broken constructor)
- `README.md` lines 369-404 — v2 HuggingFace and HFEI examples (import missing, constructor correct)
- `README.md` lines 551-585 — v1 appendix OpenAI example (broken constructor)
- `README.md` lines 587-620 — v1 appendix Cohere example (broken constructor)

### Source of truth for correct constructors
- `src/main/java/tech/amikos/chromadb/embeddings/openai/OpenAIEmbeddingFunction.java` lines 43-52 — `WithParam...` varargs constructor
- `src/main/java/tech/amikos/chromadb/embeddings/cohere/CohereEmbeddingFunction.java` lines 46-54 — `WithParam...` varargs constructor
- `src/main/java/tech/amikos/chromadb/embeddings/WithParam.java` — WithParam factory methods (apiKey, model, baseAPI, etc.)

### Prior phase fixes (pattern to follow)
- Phase 6 decision: "v1 legacy HuggingFace example updated to WithParam.apiKey() because the bare-String constructor no longer exists"
- `.planning/phases/06-tech-debt-cleanup/06-CONTEXT.md` — README doc fix decisions and conventions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WithParam.apiKey(String)` — factory method for API key parameter
- `WithParam.model(String)` — factory method for model selection parameter
- `WithParam.baseAPI(String)` — factory method for base URL parameter
- All embedding functions accept `WithParam...` varargs — consistent pattern across OpenAI, Cohere, HuggingFace, Ollama

### Established Patterns
- HuggingFace v2 example (line 378) already uses `WithParam.apiKey()` pattern — fixed in Phase 6
- HuggingFace v1 example (line 639) already uses `WithParam.apiKey()` pattern — fixed in Phase 6
- HFEI example uses `WithParam.baseAPI()` and custom `WithAPIType` — already correct
- Ollama example uses no-arg constructor — already correct (reads from env)

### Integration Points
- README.md is the only file being modified — no code behavior changes
- Line numbers may shift if earlier sections are edited; use content matching, not line numbers

</code_context>

<specifics>
## Specific Ideas

- v2 examples should show explicit model selection (`WithParam.model(...)`) to demonstrate the full WithParam pattern
- v1 appendix Cohere stays minimal (apiKey only) to preserve the original example's intent — it was always a simpler example than OpenAI
- All WithParam-using examples should include the WithParam import for copy-paste friendliness

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 07-readme-embedding-examples*
*Context gathered: 2026-03-20*
