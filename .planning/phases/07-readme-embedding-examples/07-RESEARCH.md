# Phase 7: Fix README OpenAI/Cohere Embedding Examples - Research

**Researched:** 2026-03-20
**Domain:** README documentation — Java code example correctness
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **v2 OpenAI example (line 340):** Fix constructor to `new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))`. Show explicit model selection. Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block.
- **v2 Cohere example (line 359):** Fix constructor to `new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"))`. Show explicit model selection. Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block.
- **v1 appendix OpenAI example (line 568):** Fix constructor to `new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))`. Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block (v1 examples are full class files).
- **v1 appendix Cohere example (line 604):** Fix constructor to `new CohereEmbeddingFunction(WithParam.apiKey(apiKey))` (apiKey only, no explicit model — matches original minimal intent). Add `import tech.amikos.chromadb.embeddings.WithParam;` to the import block.
- **Import consistency sweep:** Add `import tech.amikos.chromadb.embeddings.WithParam;` to v2 HuggingFace example (line ~375) and HFEI example (line ~399) — both already use WithParam but don't show the import.
- **Requirement closure:** Constructor fixes alone are sufficient to close EMB-01 and QLTY-03. No additional content or restructuring needed.

### Claude's Discretion

- Exact line placement of WithParam import within existing import blocks (alphabetical or grouped).
- Whether to adjust whitespace/formatting to match surrounding code style.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| EMB-01 | User can use OpenAI, Cohere, HuggingFace, and Ollama embedding functions through one consistent embedding contract. | The `WithParam` varargs constructor is the single consistent contract for all four providers. Fixing 4 broken README examples makes all providers follow the same documented pattern. |
| QLTY-03 | User can follow README examples for v2 auth, schema, collection lifecycle, and query workflows end-to-end. | The v2 OpenAI and Cohere constructors in README currently use removed/non-existent signatures. Fixing them enables copy-paste compilation. |
</phase_requirements>

---

## Summary

Phase 7 is a pure documentation fix. The OpenAI and Cohere embedding function examples in README.md reference constructor signatures that do not exist. Both classes expose only two constructors: a zero-arg constructor (reads from environment variables) and a `WithParam...` varargs constructor. The README examples use a removed `(String apiKey, String model)` positional constructor pattern.

There are four broken locations: the v2 section (lines 340 and 359) and the v1 legacy appendix (lines 568 and 604). All four need the constructor replaced with the `WithParam` varargs pattern. Additionally, the HuggingFace (line 375) and HFEI (line 399) v2 examples use `WithParam` but omit the import — fixing this makes all five embedding code blocks copy-paste friendly.

The fix pattern is identical to what Phase 6 established for HuggingFace: replace positional String arguments with `WithParam.apiKey(...)` and `WithParam.model(...)` factory calls. The Nyquist validation test follows the established `Phase06TechDebtValidationTest` static-analysis pattern — read README.md as a string, assert correct patterns present and broken patterns absent.

**Primary recommendation:** Apply four constructor fixes and two import additions to README.md, then create a `Phase07ReadmeEmbeddingValidationTest` that asserts the final state using file-content string matching.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 | 4.x (pom.xml) | Validation test framework | Used by all prior phase validation tests in this project |

No new dependencies required. This phase touches only README.md and adds one test file.

---

## Architecture Patterns

### Recommended Project Structure

No structural changes. One file modified, one test file added:

```
README.md                                        # 4 constructor fixes + 2 import additions
src/test/java/tech/amikos/chromadb/
└── Phase07ReadmeEmbeddingValidationTest.java    # Nyquist static-analysis validation
```

### Pattern 1: WithParam Varargs Constructor

**What:** Both `OpenAIEmbeddingFunction` and `CohereEmbeddingFunction` accept `WithParam...` varargs. Defaults are applied first internally; caller-supplied params override. The `WithParam` abstract class exposes factory methods: `apiKey(String)`, `model(String)`, `baseAPI(String)`, `defaultModel(String)`, `apiKeyFromEnv(String)`, `modelFromEnv(String)`.

**When to use:** Always when supplying explicit credentials or model name in examples.

**Verified constructors (source: `OpenAIEmbeddingFunction.java` lines 43-52, `CohereEmbeddingFunction.java` lines 46-54):**

```java
// OpenAI — WithParam varargs (only non-zero-arg constructor)
public OpenAIEmbeddingFunction(WithParam... params) throws EFException

// Cohere — WithParam varargs (only non-zero-arg constructor)
public CohereEmbeddingFunction(WithParam... params) throws EFException
```

**Correct call sites for README:**

```java
// v2 and v1-appendix OpenAI — show explicit model
new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"))

// v2 Cohere — show explicit model
new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"))

// v1-appendix Cohere — minimal (apiKey only, matches original example intent)
new CohereEmbeddingFunction(WithParam.apiKey(apiKey))
```

### Pattern 2: Static Analysis Validation Test

**What:** Each phase creates a `Phase0NXxxValidationTest` in `tech.amikos.chromadb` package. The test reads README.md and other project files from disk, then asserts string patterns are present/absent. No I/O to external services.

**When to use:** Every documentation fix that must be verified programmatically.

**Example (from `Phase06TechDebtValidationTest.java`):**

```java
// Source: src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java
@BeforeClass
public static void loadFiles() throws IOException {
    projectRoot = findProjectRoot();
    readme = readFile(projectRoot.resolve("README.md"));
}

@Test
public void test_readme_v2_hf_example_uses_withparam() {
    assertTrue(
        "README v2 HuggingFace example must use WithParam constructor",
        readme.contains("new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey))")
    );
    assertFalse(
        "README must NOT contain bare-String constructor",
        readme.contains("new HuggingFaceEmbeddingFunction(apiKey);")
    );
}
```

**`findProjectRoot()` helper (canonical, from Phase 5/6 pattern):**

```java
private static Path findProjectRoot() {
    Path candidate = Paths.get(System.getProperty("user.dir"));
    for (int i = 0; i < 10; i++) {
        if (Files.exists(candidate.resolve("pom.xml"))
                && Files.exists(candidate.resolve("README.md"))) {
            return candidate;
        }
        candidate = candidate.getParent();
        if (candidate == null) break;
    }
    return Paths.get(System.getProperty("user.dir"));
}
```

### Pattern 3: Import Block Placement

**What:** README code blocks that use `WithParam` must include the import. The v2 OpenAI and Cohere examples import `tech.amikos.chromadb.v2.*` already — add `import tech.amikos.chromadb.embeddings.WithParam;` as a separate line after the wildcard import. The v1 examples are full class files so the import goes in the class-level imports block. The HF v2 block uses `WithParam` but has no import — add it.

**Locked placement rule:** After existing imports, alphabetical or grouped — Claude's discretion per CONTEXT.md.

### Anti-Patterns to Avoid

- **Positional String constructors:** `new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small")` — this constructor does not exist. Will not compile.
- **Single bare String:** `new CohereEmbeddingFunction(apiKey)` where `apiKey` is a `String` — this constructor does not exist. Will not compile.
- **Omitting WithParam import:** Using `WithParam.apiKey(...)` in an example without the import makes the snippet non-copy-paste-safe.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Test for correct README content | Manual grep scripts | JUnit static analysis test | Validates automatically on every `mvn test`, catches regressions |
| Custom embedding parameter object | Custom Map/properties builder | `WithParam` factory methods | Already exists, handles defaults and overrides correctly |

---

## Common Pitfalls

### Pitfall 1: Line Number Drift

**What goes wrong:** The CONTEXT.md lists specific line numbers (340, 359, 568, 604) but editing earlier sections of README.md shifts subsequent line numbers.

**Why it happens:** Earlier README edits in this or prior phases shift content down.

**How to avoid:** Use content matching, not line numbers. Search for the surrounding context string (e.g., `#### OpenAI`, `new OpenAIEmbeddingFunction(apiKey,`) to locate the exact block. The CONTEXT.md explicitly notes: "use content matching, not line numbers."

**Warning signs:** Line numbers in CONTEXT.md differ from actual file by more than 5 lines.

### Pitfall 2: Incorrect Constructor Variant for v1 Cohere

**What goes wrong:** Applying `WithParam.model(...)` to the v1 Cohere example, which was intentionally minimal.

**Why it happens:** The v2 Cohere decision shows `WithParam.model(...)` but the v1 decision explicitly says "apiKey only, no explicit model — matches original minimal intent."

**How to avoid:** The v1 Cohere fix is `new CohereEmbeddingFunction(WithParam.apiKey(apiKey))` — no model param.

### Pitfall 3: Missing WithParam Import in v1 Full-Class Examples

**What goes wrong:** The v1 examples are full class files with `package` declarations and import blocks. Adding `WithParam` to the constructor without adding the import leaves the example non-compilable.

**Why it happens:** The v2 examples use snippet-style with just a few imports, so it's easy to notice missing ones. The v1 examples have more boilerplate.

**How to avoid:** After each constructor fix, verify the import block above contains `import tech.amikos.chromadb.embeddings.WithParam;`.

### Pitfall 4: Validation Test Asserts Incomplete State

**What goes wrong:** Validation test only checks that the correct pattern exists but doesn't assert the broken pattern is gone. Both assertions are needed — positive (new pattern present) and negative (old broken pattern absent).

**Why it happens:** Easy to forget the negative assertion.

**How to avoid:** For each fix, write two assertions: `assertTrue(readme.contains(...correct...))` and `assertFalse(readme.contains(...broken...))`. Follow the Phase 6 test pattern exactly.

---

## Code Examples

### Verified: README Fix Targets (current broken state)

```java
// BROKEN — v2 OpenAI (currently in README ~line 340)
OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small");

// BROKEN — v2 Cohere (currently in README ~line 359)
CohereEmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);

// BROKEN — v1 appendix OpenAI (currently in README ~line 568)
EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small");

// BROKEN — v1 appendix Cohere (currently in README ~line 604)
EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);
```

### Verified: Correct Replacements (from CONTEXT.md + source verification)

```java
// FIXED — v2 OpenAI
import tech.amikos.chromadb.embeddings.WithParam;
// ...
OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"));

// FIXED — v2 Cohere
import tech.amikos.chromadb.embeddings.WithParam;
// ...
CohereEmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"));

// FIXED — v1 appendix OpenAI (add import to class-level import block)
import tech.amikos.chromadb.embeddings.WithParam;
// ...
EmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"));

// FIXED — v1 appendix Cohere (add import to class-level import block; no model param)
import tech.amikos.chromadb.embeddings.WithParam;
// ...
EmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey));
```

### Verified: Import Additions for HF/HFEI v2 Examples (already-correct constructors)

```java
// HuggingFace v2 example — add missing import
import tech.amikos.chromadb.embeddings.WithParam;
// constructor already correct: new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey))

// HFEI v2 example — add missing import
import tech.amikos.chromadb.embeddings.WithParam;
// constructor already correct: WithParam.baseAPI("http://localhost:8008")
```

### Verified: Default Model Values (from source)

```java
// Source: OpenAIEmbeddingFunction.java line 24
public static final String DEFAULT_MODEL_NAME = "text-embedding-ada-002";
// Note: README example should use "text-embedding-3-small" (explicit model override per CONTEXT.md decision)

// Source: CohereEmbeddingFunction.java line 24
public static final String DEFAULT_MODEL_NAME = "embed-english-v2.0";
// Note: DEFAULT_MODEL_NAME happens to equal the explicit model in decisions — WithParam.model() overrides the default
```

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 |
| Config file | pom.xml (surefire plugin) |
| Quick run command | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest` |
| Full suite command | `mvn test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EMB-01 | README OpenAI example uses WithParam constructor | unit/static | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest#test_readme_v2_openai_example_uses_withparam` | ❌ Wave 0 |
| EMB-01 | README Cohere example uses WithParam constructor | unit/static | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest#test_readme_v2_cohere_example_uses_withparam` | ❌ Wave 0 |
| EMB-01 | README v1 OpenAI example uses WithParam constructor | unit/static | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest#test_readme_v1_openai_example_uses_withparam` | ❌ Wave 0 |
| EMB-01 | README v1 Cohere example uses WithParam constructor | unit/static | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest#test_readme_v1_cohere_example_uses_withparam` | ❌ Wave 0 |
| QLTY-03 | All v2 WithParam-using examples have WithParam import | unit/static | `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest#test_readme_v2_withparam_examples_have_import` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `mvn test -Dtest=Phase07ReadmeEmbeddingValidationTest`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/tech/amikos/chromadb/Phase07ReadmeEmbeddingValidationTest.java` — covers EMB-01 (all four constructor fixes) and QLTY-03 (import sweep)

---

## Sources

### Primary (HIGH confidence)

- `src/main/java/tech/amikos/chromadb/embeddings/openai/OpenAIEmbeddingFunction.java` — verified constructor signatures (lines 36-52)
- `src/main/java/tech/amikos/chromadb/embeddings/cohere/CohereEmbeddingFunction.java` — verified constructor signatures (lines 39-54)
- `src/main/java/tech/amikos/chromadb/embeddings/WithParam.java` — verified factory methods
- `README.md` — confirmed broken constructor calls at ~lines 340, 359, 568, 604
- `.planning/phases/07-readme-embedding-examples/07-CONTEXT.md` — locked decisions
- `src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java` — canonical test pattern

### Secondary (MEDIUM confidence)

- `.planning/phases/06-tech-debt-cleanup/06-01-PLAN.md` — plan structure pattern to follow

### Tertiary (LOW confidence)

None.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; JUnit 4 already in pom.xml
- Architecture: HIGH — constructor signatures verified directly from source files; test pattern from Phase 6 identical
- Pitfalls: HIGH — identified from source diff between broken README and actual constructors; line number drift is observable

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable — constructor signatures won't change without a Phase; README structure is stable)
