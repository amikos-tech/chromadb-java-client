---
phase: 07-readme-embedding-examples
verified: 2026-03-20T18:55:00Z
status: passed
score: 6/6 must-haves verified
re_verification: false
---

# Phase 7: Fix README Embedding Examples Verification Report

**Phase Goal:** Fix README embedding function examples for OpenAI and Cohere to use the correct `WithParam` constructor pattern, closing the last 2 partial requirements (EMB-01, QLTY-03).
**Verified:** 2026-03-20T18:55:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                 | Status     | Evidence                                                                                                   |
| --- | --------------------------------------------------------------------- | ---------- | ---------------------------------------------------------------------------------------------------------- |
| 1   | v2 OpenAI README example compiles with WithParam constructor          | VERIFIED   | README line 341: `new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))` |
| 2   | v2 Cohere README example compiles with WithParam constructor          | VERIFIED   | README line 361: `new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"))` |
| 3   | v1 appendix OpenAI README example uses WithParam constructor          | VERIFIED   | README line 573: `EmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))` |
| 4   | v1 appendix Cohere README example uses WithParam constructor          | VERIFIED   | README line 610: `EmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey))` |
| 5   | All v2 embedding examples that use WithParam include the WithParam import | VERIFIED | README lines 338, 358, 378, 403 — 4 v2 imports present; test `test_readme_v2_withparam_examples_have_import` passes |
| 6   | All v1 appendix examples that use WithParam include the WithParam import  | VERIFIED | README lines 564, 600, 637, 673 — 4 v1 imports present; total count = 8; test `test_readme_v1_withparam_examples_have_import` passes |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact                                                   | Expected                                          | Status   | Details                                                                                     |
| ---------------------------------------------------------- | ------------------------------------------------- | -------- | ------------------------------------------------------------------------------------------- |
| `README.md`                                                | Fixed constructors (4) + WithParam imports (8)    | VERIFIED | `grep -c "import tech.amikos.chromadb.embeddings.WithParam;" README.md` returns 8; no broken constructor patterns remain |
| `src/test/java/tech/amikos/chromadb/Phase07ReadmeEmbeddingValidationTest.java` | 6-method Nyquist validation test | VERIFIED | 160-line file with all 6 test methods present; `@BeforeClass loadFiles()`, `findProjectRoot()`, and `countOccurrences()` helper all present |

### Key Link Verification

| From                                      | To         | Via                          | Status   | Details                                                                                              |
| ----------------------------------------- | ---------- | ---------------------------- | -------- | ---------------------------------------------------------------------------------------------------- |
| `Phase07ReadmeEmbeddingValidationTest.java` | `README.md` | `readme.contains(...)` / `readFile(projectRoot.resolve("README.md"))` | WIRED | `@BeforeClass loadFiles()` reads README via `findProjectRoot().resolve("README.md")`; 6 assertions exercise README content |

### Requirements Coverage

| Requirement | Source Plan | Description                                                                                  | Status    | Evidence                                                                                        |
| ----------- | ----------- | -------------------------------------------------------------------------------------------- | --------- | ----------------------------------------------------------------------------------------------- |
| EMB-01      | 07-01-PLAN  | User can use OpenAI, Cohere, HuggingFace, and Ollama embedding functions through one consistent embedding contract | SATISFIED | Both OpenAI and Cohere README examples (v2 + v1 appendix) use correct `WithParam`-based constructors matching actual API: `OpenAIEmbeddingFunction(WithParam... params)` and `CohereEmbeddingFunction(WithParam... params)` |
| QLTY-03     | 07-01-PLAN  | User can follow README examples for v2 auth, schema, collection lifecycle, and query workflows end-to-end | SATISFIED | All 8 embedding code blocks (4 v2 + 4 v1) include `import tech.amikos.chromadb.embeddings.WithParam;`; examples are now copy-paste correct |

**Requirement traceability check:** REQUIREMENTS.md maps exactly EMB-01 and QLTY-03 to Phase 7. No orphaned requirements.

### Anti-Patterns Found

No anti-patterns detected.

- No `TODO`, `FIXME`, or `PLACEHOLDER` comments in test file or README
- No stub constructor patterns (`return null`, empty handlers, etc.)
- No broken constructor patterns remain in README (`new OpenAIEmbeddingFunction(apiKey, ...)`, `new CohereEmbeddingFunction(apiKey)`)

### Human Verification Required

None. All phase behaviors have automated verification via `Phase07ReadmeEmbeddingValidationTest`.

### Commit Verification

Both commits claimed in SUMMARY exist and match stated content:

- `4562c0e` — test(07-01): add failing validation test for README embedding examples (160-line test file, +160 lines)
- `28adcee` — fix(07-01): fix README embedding function constructors and add WithParam imports (+12/-4 lines in README.md)

### Test Execution

`mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest` result:

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 - in tech.amikos.chromadb.Phase07ReadmeEmbeddingValidationTest
BUILD SUCCESS
```

### Source API Alignment

README constructors align with actual source signatures:

- `OpenAIEmbeddingFunction(WithParam... params)` — line 43 of `OpenAIEmbeddingFunction.java`
- `CohereEmbeddingFunction(WithParam... params)` — line 46 of `CohereEmbeddingFunction.java`
- `WithParam.apiKey(String)`, `WithParam.model(String)` factory methods — lines 12, 20 of `WithParam.java`

---

_Verified: 2026-03-20T18:55:00Z_
_Verifier: Claude (gsd-verifier)_
