# Phase 4: Compatibility & Test Matrix - Research

**Researched:** 2026-03-20
**Domain:** Maven build tooling (animal-sniffer), GitHub Actions matrix strategy, JUnit 4 test-skip patterns, reflection-based API compatibility testing
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Chroma version matrix**
- Matrix anchors: **1.0.0** (oldest supported), **1.3.7** (mid-range), and a **pinned latest** (currently 1.5.5). Three versions total.
- "Latest" is pinned to a specific version in config, bumped manually when new Chroma releases drop. No dynamic `:latest` tag.
- Version list centralized in a **Makefile variable** (`CHROMA_MATRIX_VERSIONS`). CI workflow maintains its own matrix definition but should match.
- A single `make test-matrix` target runs unit tests + integration tests sequentially across all 3 pinned versions.
- Version-specific test skips use an **annotation-based** approach (e.g., `@MinChromaVersion("1.3.0")` or `Assume.assumeTrue` with version comparison) so one test suite covers all versions without duplication.
- Container startup failures **fail fast** — if a Chroma container can't start for a version, that matrix cell fails (not skipped).

**JDK version matrix**
- Test across **JDK 8, 11, and 17** (all LTS releases).
- Matrix shape: **JDK 8 tests all 3 Chroma versions**. JDK 11 and 17 test only the latest pinned Chroma version. 5 total combinations.

**Java 8 API enforcement**
- Add **animal-sniffer-maven-plugin** with `java18` (JDK 1.8) API signature.
- Bound to **default lifecycle** (every build) — catches JDK 9+ API usage immediately during local `mvn compile`.
- **Test code excluded** from animal-sniffer — only main source is enforced. Tests can use JDK 9+ APIs.
- Existing `maven-enforcer-plugin` `requireJavaVersion [1.8,)` rule kept as-is — no tightening needed.

**Public API breakage detection**
- **Expand the existing reflection-based `PublicInterfaceCompatibilityTest`** rather than adding japicmp/revapi.
- Scope: **public interfaces + builders** — `Client`, `Collection`, `Collection.*Builder`, `ChromaClient.Builder`, `ChromaClient.CloudBuilder`.
- Assertions per method: **existence + return type** (and default-method status for interface methods that must remain default).
- Surface area growth detection via **count-based check** — assert total public method count on each key interface. Adding a new public method requires updating the expected count.

**CI gating strategy**
- Use **GitHub Actions `strategy.matrix`** to parameterize JDK version and Chroma version.
- **All matrix cells required** to pass for PR merge — no advisory-only cells.
- **No fail-fast** — all matrix cells run to completion even if one fails. Full picture of breakage.
- **Separate jobs**: unit tests run in a fast job (no Docker, all JDKs). Integration tests run in a matrix job (Docker + Testcontainers, JDK x Chroma).
- Cloud parity tests (v2-integration-test) **remain a separate job** — only runs when cloud credentials are available.
- Animal-sniffer and `PublicInterfaceCompatibilityTest` are **implicit** — they run as part of the normal build/test lifecycle, no extra CI step needed.

### Claude's Discretion
- Exact annotation/utility design for `@MinChromaVersion` version-skip mechanism.
- Exact animal-sniffer plugin version and signature artifact coordinates.
- Which specific method signatures to add in the expanded `PublicInterfaceCompatibilityTest` (as long as all public interfaces + builders are covered).
- Exact GitHub Actions matrix syntax and job naming.
- Whether to consolidate the existing Phase 2 Makefile targets (`test-phase-02-parity`, etc.) or leave them alongside the new `test-matrix`.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within Phase 4 scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| QLTY-01 | Maintainer can run unit + integration tests against supported Chroma versions with reproducible pass/fail behavior. | Makefile `test-matrix` target + GitHub Actions matrix job covering 1.0.0, 1.3.7, 1.5.5 with `CHROMA_VERSION` env-var; `@MinChromaVersion` skip mechanism for version-conditional tests |
| QLTY-02 | Maintainer is protected from Java 8 and public interface compatibility regressions before release. | animal-sniffer-maven-plugin 1.27 + `java18:1.0` signature on default lifecycle; expanded `PublicInterfaceCompatibilityTest` covering all public interfaces + builders with method-count assertions |
</phase_requirements>

---

## Summary

Phase 4 adds two orthogonal guardrails: a reproducible multi-version test matrix (Chroma versions + JDK versions) and compile-time/test-time API compatibility enforcement. Neither guardrail adds product features — they are purely quality gates.

The implementation splits into two distinct deliverables. Plan 04-01 implements the version matrix: a `CHROMA_MATRIX_VERSIONS` Makefile variable, a `make test-matrix` target, a `@MinChromaVersion` annotation + `AbstractChromaIntegrationTest` helper, and a GitHub Actions integration-test matrix job with 5 cells (JDK 8 x 3 Chroma versions + JDK 11/17 x latest only). Plan 04-02 implements the compatibility guardrails: animal-sniffer-maven-plugin 1.27 bound to the default lifecycle, and an expanded `PublicInterfaceCompatibilityTest` covering all public interfaces and builders with both method-signature and count-based assertions.

**Primary recommendation:** Use `animal-sniffer-maven-plugin` 1.27 with `java18:1.0` signature; structure the GitHub Actions matrix with JDK 8 as the base dimension and JDK 11/17 added as explicit `include` entries pointing at the latest Chroma version only.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| animal-sniffer-maven-plugin | 1.27 | Detect use of JDK 9+ APIs in main source | Industry standard for Java 8 bytecode-level API enforcement; used by Guava, OkHttp, and dozens of major Java libraries |
| org.codehaus.mojo.signature:java18 | 1.0 | JDK 1.8 API signature artifact consumed by animal-sniffer | Official blessed artifact from mojohaus, released Feb 2016, available on Maven Central |
| actions/setup-java | v4 (recommended upgrade from v3) | Configure JDK on GitHub Actions runners | v4 is the current stable release; current CI uses v3 (deprecated); temurin distribution cached on ubuntu-latest |
| actions/checkout | v4 (recommended upgrade from v3) | Checkout repo | Current stable; CI uses v3 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit 4 `Assume` | already present (4.13.2) | Skip tests at runtime when version preconditions are not met | Used inside `@Before` in `AbstractChromaIntegrationTest` to skip version-conditional tests |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| animal-sniffer-maven-plugin | `--release 8` javac flag | `--release` requires JDK 9+ to compile; project must compile on JDK 8 itself (`requireJavaVersion [1.8,)`), so --release is not available. animal-sniffer is the only option. |
| Expanding `PublicInterfaceCompatibilityTest` | japicmp-maven-plugin or revapi | Binary comparison tools add significant configuration complexity and require a published baseline artifact. User decision locks reflection-based expansion. |
| GitHub Actions `include` matrix pattern | Full cross-product + `exclude` | `include` to add JDK 11/17 cells is simpler and produces exactly 5 jobs with no risk of accidentally running excluded combinations |

**Installation:**
```bash
# No new runtime dependencies — animal-sniffer is a build plugin only
# Add to pom.xml <build><plugins>
```

**Version verification (confirmed):**
- `animal-sniffer-maven-plugin`: latest is **1.27** (confirmed via central.sonatype.com)
- `java18` signature artifact: **1.0** (only version, released 2016-02-17, confirmed on Maven Central)

## Architecture Patterns

### Recommended Project Structure

No new source directories needed. Changes are localized to:

```
pom.xml                                          # Add animal-sniffer plugin
Makefile                                         # Add CHROMA_MATRIX_VERSIONS + test-matrix target
.github/workflows/integration-test.yml           # Expand to matrix job
src/test/java/tech/amikos/chromadb/v2/
├── AbstractChromaIntegrationTest.java           # Add version comparison helper + assumeMinVersion()
├── MinChromaVersion.java                        # New: @MinChromaVersion annotation (test package)
└── PublicInterfaceCompatibilityTest.java        # Expand: add remaining methods + count assertions
```

### Pattern 1: animal-sniffer Plugin Configuration

**What:** Adds `animal-sniffer:check` goal to the `test` phase of the default lifecycle. Checks only main sources (test excluded by default via `checkTestClasses` default = `false`).

**When to use:** Every build — no profile gate needed. Developers catch JDK 9+ API usage immediately.

**Example:**
```xml
<!-- Source: https://www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/usage.html -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>animal-sniffer-maven-plugin</artifactId>
  <version>1.27</version>
  <configuration>
    <signature>
      <groupId>org.codehaus.mojo.signature</groupId>
      <artifactId>java18</artifactId>
      <version>1.0</version>
    </signature>
    <!-- checkTestClasses defaults to false — test sources not checked -->
  </configuration>
  <executions>
    <execution>
      <id>check-java-1.8-compat</id>
      <phase>test</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**Important:** Binding to `test` phase (not `compile`) ensures it runs on every `mvn test` but not on a bare `mvn compile` that skips tests. For stricter enforcement bind to `compile` phase if desired — but `test` phase is the pattern used by most Java libraries.

### Pattern 2: `@MinChromaVersion` Annotation + Version Skip Utility

**What:** A `@MinChromaVersion("1.3.0")` annotation declared in the test package, consumed by a `assumeMinVersion()` helper in `AbstractChromaIntegrationTest`. Test methods annotated with `@MinChromaVersion` call `assumeMinVersion()` in a `@Before` hook via reflection, or test methods themselves call the helper directly.

**Design choice:** The simpler path is a plain static helper method `AbstractChromaIntegrationTest.assumeMinVersion("1.3.0")` called at the start of tests that require it, combined with a `@MinChromaVersion` annotation that documents intent but is processed via a JUnit `@Rule` or `@Before` reflection scan. Given JUnit 4 (not 5), the most idiomatic approach is:

Option A (simpler — recommended): Static helper method only, no annotation processing:
```java
// In AbstractChromaIntegrationTest:
protected static void assumeMinVersion(String minVersion) {
    Assume.assumeTrue(
        "Skipping: requires Chroma >= " + minVersion
            + ", running " + configuredChromaVersion(),
        compareVersions(configuredChromaVersion(), minVersion) >= 0
    );
}

// Simple dotted-version comparator (Java 8 compatible — no String.split result stream):
private static int compareVersions(String v1, String v2) {
    String[] parts1 = v1.split("\\.");
    String[] parts2 = v2.split("\\.");
    int len = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < len; i++) {
        int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
        int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
        if (n1 != n2) return Integer.compare(n1, n2);
    }
    return 0;
}
```

Option B (annotation-driven — more declarative): A `@MinChromaVersion` `@interface` + a JUnit `@Rule` that reads it and calls `Assume.assumeTrue`. Requires a `TestRule` implementation. Adds more code but documents version requirements at the method site.

**Recommendation (Claude's discretion):** Use Option A (static helper method) for simplicity. The `@MinChromaVersion` annotation can still be added as a documentation-only marker (with `@Retention(RUNTIME)` and a `@Rule` consuming it) if the user wants annotation-based intent signaling. Both are valid; Option A is less infrastructure.

**When to use:** At the start of any integration test method that requires a minimum Chroma version feature.

### Pattern 3: GitHub Actions Matrix — 5 Cells, No Fail-Fast

**What:** Integration test job uses `strategy.matrix` with JDK 8 as the base dimension (3 Chroma versions) and `include:` to add JDK 11 + JDK 17 pointed at the latest Chroma version.

**When to use:** Integration test job only. Unit test job uses a separate simpler matrix (all 3 JDKs, no Chroma version dimension — no Docker needed).

**Example:**
```yaml
# Source: https://docs.github.com/actions/writing-workflows/choosing-what-your-workflow-does/running-variations-of-jobs-in-a-workflow
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java: ['8', '11', '17']
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: maven
      - name: Run unit tests
        run: mvn --batch-mode test

  integration-tests:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        chroma-version: ['1.0.0', '1.3.7', '1.5.5']
        java: ['8']
      include:
        - java: '11'
          chroma-version: '1.5.5'
        - java: '17'
          chroma-version: '1.5.5'
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: maven
      - name: Run integration tests
        run: mvn --batch-mode -Pintegration test
        env:
          CHROMA_VERSION: ${{ matrix.chroma-version }}
```

**Key details:**
- `fail-fast: false` on both jobs — all cells run to completion.
- `distribution: 'temurin'` replaces the deprecated `'adopt'` used in current CI.
- Unit test job has NO `CHROMA_VERSION` env — it uses Surefire's default excludes (no `*IntegrationTest`).
- All cells are implicitly required (no `continue-on-error`).

### Pattern 4: PublicInterfaceCompatibilityTest Expansion

**What:** Add method-existence + return-type assertions for all public methods on `Client`, `Collection`, each builder (`AddBuilder`, `QueryBuilder`, `GetBuilder`, `UpdateBuilder`, `UpsertBuilder`, `DeleteBuilder`), `ChromaClient.Builder`, and `ChromaClient.CloudBuilder`. Then add a count assertion per type.

**When to use:** The test runs as part of normal unit test suite (`mvn test`) — no special profile.

**Count-assertion pattern:**
```java
// Source: existing PublicInterfaceCompatibilityTest + java.lang.Class reflection API
@Test
public void testClientInterfaceMethodCount() throws Exception {
    // Count declared public methods (not inherited from Object)
    long count = Arrays.stream(Client.class.getMethods())
        .filter(m -> m.getDeclaringClass() == Client.class)
        .count();
    assertEquals(
        "Client public method count changed — update this test if intentional",
        EXPECTED_CLIENT_METHOD_COUNT,
        count
    );
}
```

**Java 8 note:** `Arrays.stream` is available in Java 8. The above pattern is Java 8 compatible.

### Anti-Patterns to Avoid

- **Dynamic `:latest` Chroma image in CI:** The decision mandates pinned versions. Do not use `chromadb/chroma:latest` — image changes silently break the matrix.
- **`fail-fast: true` in matrix jobs:** Hides the full regression picture. Always set `fail-fast: false`.
- **Binding animal-sniffer to `verify` phase only:** This delays detection until package/verify lifecycle. Binding to `test` catches violations on every `mvn test`.
- **Running animal-sniffer on test classes:** `checkTestClasses` defaults to `false` and must stay `false`. Tests legitimately use try-with-resources sugar and other constructs; enforcing Java 8 API on tests is counterproductive.
- **Using `Assume.assumeTrue` for container startup failure (WRONG):** The existing `AbstractChromaIntegrationTest` already skips tests when container startup fails. The decision says container startup failures MUST fail fast. The correct behavior is: if `CHROMA_STARTUP_FAILURE != null`, the test should throw (fail), not assume (skip). **This is a behavior change needed in Plan 04-01** — currently `setUp()` calls `Assume.assumeTrue(CHROMA_STARTUP_FAILURE == null, ...)` which SKIPS rather than FAILS. The decision requires FAIL on container startup failure.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Java API backward-compatibility checking | Custom bytecode scanner | animal-sniffer-maven-plugin | animal-sniffer compares compiled bytecode against a known-good API signature set; handles inner classes, varargs, generics, and JDK boot class path differences correctly |
| Semantic version comparison | String.split + manual int comparison (acceptable for this use) | No library needed | Semver comparison for 3-part dotted versions is trivial and adding a dep (semver4j, etc.) is unjustified. The compareVersions helper above is sufficient. |
| CI matrix orchestration | Shell scripts looping over versions | GitHub Actions `strategy.matrix` | Native matrix support provides per-cell status, parallel execution, and matrix visualization in the GitHub UI at zero script cost |

**Key insight:** animal-sniffer operates on bytecode, not source. It catches runtime API differences (e.g., `java.nio.file.Files` methods added in JDK 9+) that the compiler with `--source 1.8 --target 1.8` alone cannot detect because those flags only restrict language syntax, not API usage.

## Common Pitfalls

### Pitfall 1: animal-sniffer False Positives from Dependencies

**What goes wrong:** animal-sniffer reports violations for APIs used inside dependency jars (e.g., OkHttp using JDK 9+ internals), not just your own code.
**Why it happens:** By default animal-sniffer checks the compile classpath transitively.
**How to avoid:** The `ignoreDependencies` parameter defaults to `true` in 1.27 — dependency jars are already ignored. No action needed unless the default changes.
**Warning signs:** Errors pointing to classes outside `tech.amikos.*`.

### Pitfall 2: `adopt` Distribution Deprecated in actions/setup-java

**What goes wrong:** The current `integration-test.yml` uses `distribution: 'adopt'` (AdoptOpenJDK). This distribution is deprecated and may stop working without notice.
**Why it happens:** The workflow was written when `adopt` was the standard. Eclipse Temurin replaced AdoptOpenJDK in 2021.
**How to avoid:** Use `distribution: 'temurin'` in the new matrix workflow.
**Warning signs:** GitHub Actions deprecation warnings in job logs.

### Pitfall 3: Matrix Container Startup Skip vs. Fail

**What goes wrong:** The current `AbstractChromaIntegrationTest.setUp()` calls `Assume.assumeTrue(CHROMA_STARTUP_FAILURE == null, ...)`. This causes JUnit to SKIP tests (green "skipped" marker) when a container fails to start, rather than FAIL the build. This masks real breakage when a Chroma version is incompatible.
**Why it happens:** The original design was defensive. Phase 4 explicitly reverses this — container startup failure must fail the matrix cell.
**How to avoid:** In `setUp()`, replace the `Assume.assumeTrue` startup-failure check with an explicit `fail()` or `throw`:
```java
if (CHROMA_STARTUP_FAILURE != null) {
    throw new AssertionError(
        "Chroma container failed to start for version "
            + configuredChromaVersion(), CHROMA_STARTUP_FAILURE);
}
```
**Warning signs:** CI matrix cells show green/skipped instead of red when a container image is unavailable.

### Pitfall 4: PublicInterfaceCompatibilityTest Method Count Off-By-One

**What goes wrong:** `Class.getMethods()` returns all public methods including those inherited from `Object` and from extended interfaces. Counting all of them makes the count brittle.
**Why it happens:** `getMethods()` includes inherited methods; `getDeclaredMethods()` includes only non-public if filtering is skipped.
**How to avoid:** Filter to `m.getDeclaringClass() == TargetInterface.class` or use `getDeclaredMethods()` which already limits to the declaring class.
**Warning signs:** Method count assertions fail on JDK version changes (e.g., new synthetic bridge methods added by JVM).

### Pitfall 5: Makefile `test-matrix` Target Variable Expansion

**What goes wrong:** Makefile `foreach` loops with shell `for` loops can silently swallow non-zero exit codes if `set -e` is not used, making a failed matrix run appear successful.
**Why it happens:** Makefile recipes run each line in a new subshell by default.
**How to avoid:** Use `set -e` at the start of the recipe or use `&&` chaining between sequential commands. Or use a shell function with explicit exit code propagation.
**Warning signs:** `make test-matrix` exits 0 even when one Chroma version fails.

## Code Examples

Verified patterns from official sources and existing codebase:

### animal-sniffer Plugin (pom.xml)
```xml
<!-- Source: https://www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/usage.html -->
<!-- Place inside <build><plugins> alongside maven-compiler-plugin -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>animal-sniffer-maven-plugin</artifactId>
  <version>1.27</version>
  <configuration>
    <signature>
      <groupId>org.codehaus.mojo.signature</groupId>
      <artifactId>java18</artifactId>
      <version>1.0</version>
    </signature>
  </configuration>
  <executions>
    <execution>
      <id>check-java-1.8-compat</id>
      <phase>test</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### Makefile `test-matrix` Target
```makefile
# Centralized version list
CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5

.PHONY: test-matrix
test-matrix: check-tools ## Run full unit + integration test matrix across all pinned Chroma versions
	@echo "$(BLUE)Running unit tests (all JDKs implied via current JDK)...$(NC)"
	$(MAVEN) --batch-mode test
	@echo "$(BLUE)Running integration tests across Chroma versions: $(CHROMA_MATRIX_VERSIONS)$(NC)"
	@set -e; for v in $(CHROMA_MATRIX_VERSIONS); do \
	  echo "$(BLUE)  Testing with Chroma $$v...$(NC)"; \
	  CHROMA_VERSION=$$v $(MAVEN) --batch-mode -Pintegration test || exit $$?; \
	done
	@echo "$(GREEN)test-matrix complete$(NC)"
```

### `assumeMinVersion` Helper in AbstractChromaIntegrationTest
```java
// Source: existing AbstractChromaIntegrationTest + JUnit 4 Assume
protected static void assumeMinVersion(String minVersion) {
    Assume.assumeTrue(
        "Skipping: requires Chroma >= " + minVersion
            + ", currently running " + configuredChromaVersion(),
        compareVersions(configuredChromaVersion(), minVersion) >= 0
    );
}

/** Compares two dotted-version strings. Returns negative/zero/positive like Comparator. */
static int compareVersions(String v1, String v2) {
    String[] parts1 = v1.split("\\.");
    String[] parts2 = v2.split("\\.");
    int len = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < len; i++) {
        int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
        int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
        if (n1 != n2) return Integer.compare(n1, n2);
    }
    return 0;
}
```

### Container Startup Fail-Fast (setUp replacement)
```java
// Source: existing AbstractChromaIntegrationTest — replace Assume block with:
@Before
public void setUp() {
    if (CHROMA_STARTUP_FAILURE != null) {
        throw new AssertionError(
            "Chroma container failed to start for version "
                + configuredChromaVersion()
                + " — failing matrix cell",
            CHROMA_STARTUP_FAILURE);
    }
    // ... rest of setUp unchanged
}
```

### PublicInterfaceCompatibilityTest — Count Assertion Pattern
```java
// Source: java.lang.Class reflection API (Java 8+)
import java.lang.reflect.Method;

@Test
public void testClientInterfaceHasExpectedMethodCount() {
    long count = 0;
    for (Method m : Client.class.getDeclaredMethods()) {
        count++;
    }
    assertEquals(
        "Client.java public method count changed — update EXPECTED count if intentional",
        EXPECTED_CLIENT_COUNT,  // constant to be set after audit
        count
    );
}
```

**Note:** Use `getDeclaredMethods()` (not `getMethods()`) — it returns only methods declared directly on the class/interface, excluding inherited `Object` methods.

### GitHub Actions Matrix Workflow (integration-test.yml replacement)
```yaml
# Source: https://docs.github.com/actions/writing-workflows/choosing-what-your-workflow-does/running-variations-of-jobs-in-a-workflow
name: Integration test

on:
  pull_request:
    branches:
      - main
      - "**"

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java: ['8', '11', '17']
    name: Unit tests (JDK ${{ matrix.java }})
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: maven
      - name: Run unit tests
        run: mvn --batch-mode test

  integration-tests:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        chroma-version: ['1.0.0', '1.3.7', '1.5.5']
        java: ['8']
      include:
        - java: '11'
          chroma-version: '1.5.5'
        - java: '17'
          chroma-version: '1.5.5'
    name: Integration tests (JDK ${{ matrix.java }}, Chroma ${{ matrix.chroma-version }})
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: maven
      - name: Run integration tests
        run: mvn --batch-mode -Pintegration test
        env:
          CHROMA_VERSION: ${{ matrix.chroma-version }}

  v2-integration-test:
    runs-on: ubuntu-latest
    name: Cloud parity tests
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 8
        uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
          cache: maven
      - name: Run v2 integration tests
        run: mvn --batch-mode test -Pintegration
        env:
          CHROMA_VERSION: '1.5.5'
          CHROMA_API_KEY: ${{ secrets.CHROMA_API_KEY }}
          CHROMA_TENANT: ${{ secrets.CHROMA_TENANT }}
          CHROMA_DATABASE: ${{ secrets.CHROMA_DATABASE }}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| AdoptOpenJDK (`distribution: adopt`) | Eclipse Temurin (`distribution: temurin`) | 2021 (Eclipse Foundation takeover) | `adopt` is deprecated; temurin is cached on GitHub hosted runners for faster builds |
| `actions/setup-java@v3` | `actions/setup-java@v4` | 2023 | v4 is current stable; v3 works but is older |
| Single Chroma version in CI (1.0.0 + 1.5.2) | 3-version matrix (1.0.0, 1.3.7, 1.5.5) | Phase 4 | Closes regression gaps between oldest supported and mid-range versions |
| No Java API enforcement | animal-sniffer on default lifecycle | Phase 4 | Catches accidental JDK 9+ API use before merge |

**Deprecated/outdated:**
- `distribution: 'adopt'` in existing `integration-test.yml`: Deprecated. Replace with `temurin`.
- `actions/checkout@v3` and `actions/setup-java@v3`: Functional but older. Upgrade to v4 when editing the workflow.
- `DEFAULT_CHROMA_VERSION = "1.5.2"` in `AbstractChromaIntegrationTest`: Should be updated to `"1.5.5"` to match the pinned latest in the matrix.

## Open Questions

1. **Scope of `PublicInterfaceCompatibilityTest` method counts**
   - What we know: `Client` has ~22 declared methods, `Collection` has ~15 declared methods plus 6 builder interfaces each with ~5-10 methods.
   - What's unclear: The exact expected counts need to be established by running `getDeclaredMethods()` on each type at the time of writing Plan 04-02. Counts must be set as constants after that audit.
   - Recommendation: The planner should instruct the implementer to run a count-discovery step first, then hardcode the constants.

2. **Makefile target consolidation (`test-phase-02-parity` vs `test-matrix`)**
   - What we know: `test-phase-02-parity` currently tests 1.5.5 and 1.3.7. `test-matrix` will cover 1.0.0, 1.3.7, and 1.5.5 more comprehensively.
   - What's unclear: Whether `test-phase-02-parity` should be deprecated or kept for historical reference.
   - Recommendation (Claude's discretion): Keep `test-phase-02-parity` alongside `test-matrix` to avoid breaking any existing developer muscle memory. Add a comment in the Makefile noting that `test-matrix` is the canonical target.

3. **`@MinChromaVersion` annotation vs. plain helper method**
   - What we know: Both patterns work with JUnit 4. Annotation-driven requires a `TestRule` (more code). Static helper is simpler.
   - What's unclear: Whether any test authors will use the annotation as documentation vs. just calling the helper inline.
   - Recommendation: Implement the static helper `assumeMinVersion()` in `AbstractChromaIntegrationTest`. Optionally add a `@MinChromaVersion` `@interface` as a documentation-only marker that is processed by a `@Rule` in the base class.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 |
| Config file | none (Maven Surefire convention-based) |
| Quick run command | `mvn test` |
| Full suite command | `mvn test && mvn -Pintegration test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| QLTY-01 | `make test-matrix` runs all 3 Chroma versions sequentially without silent failure | smoke (manual verify) | `make test-matrix` | ❌ Wave 0 (Makefile target must be added) |
| QLTY-01 | `assumeMinVersion("1.3.0")` skips test when running 1.0.0, runs when 1.3.7 | unit | `mvn test -Dtest=AbstractChromaIntegrationTestVersionTest` | ❌ Wave 0 |
| QLTY-01 | Container startup failure throws `AssertionError` (not skips) | unit/integration | `mvn -Pintegration test -Dtest=...` (requires injecting failure) | Partial (behavior change in existing file) |
| QLTY-01 | GitHub Actions matrix runs 5 cells (3 + 2 include) with `fail-fast: false` | CI | Push/PR trigger | ❌ Wave 0 (workflow must be updated) |
| QLTY-02 | `mvn test` fails when JDK 9+ API is used in main source | unit lifecycle | `mvn test` with a deliberate violation then revert | ❌ Wave 0 (plugin must be added) |
| QLTY-02 | `PublicInterfaceCompatibilityTest` detects missing `Client.heartbeat()` method | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | ✅ (partial — expand) |
| QLTY-02 | Count assertions detect new public method additions to `Client`, `Collection` | unit | `mvn test -Dtest=PublicInterfaceCompatibilityTest` | ❌ Wave 0 (count assertions must be added) |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=PublicInterfaceCompatibilityTest` (< 5s)
- **Per wave merge:** `mvn test` (full unit suite including animal-sniffer check)
- **Phase gate:** `mvn test && CHROMA_VERSION=1.0.0 mvn -Pintegration test && CHROMA_VERSION=1.3.7 mvn -Pintegration test && CHROMA_VERSION=1.5.5 mvn -Pintegration test`

### Wave 0 Gaps
- [ ] `pom.xml` — add `animal-sniffer-maven-plugin` 1.27 configuration
- [ ] `Makefile` — add `CHROMA_MATRIX_VERSIONS` variable and `test-matrix` target
- [ ] `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` — add `assumeMinVersion()` + `compareVersions()` helpers, change container startup to fail-fast
- [ ] `.github/workflows/integration-test.yml` — replace with matrix job structure (5 cells, `fail-fast: false`, `temurin`)
- [ ] `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` — expand to cover all public interfaces + builders + count assertions

## Sources

### Primary (HIGH confidence)
- `central.sonatype.com/artifact/org.codehaus.mojo/animal-sniffer-maven-plugin` — latest version (1.27) confirmed
- `github.com/mojohaus/animal-sniffer/issues/1` — `java18:1.0` signature artifact existence confirmed, released 2016-02-17
- `www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/check-mojo.html` — `checkTestClasses` defaults to `false`, `ignores` parameter available
- `docs.github.com/actions/writing-workflows/choosing-what-your-workflow-does/running-variations-of-jobs-in-a-workflow` — matrix `include` syntax confirmed for asymmetric matrix shape
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` — existing `CHROMA_VERSION` env-var switching, `Assume.assumeTrue` pattern, `configuredChromaVersion()` helper
- `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` — existing reflection test structure
- `pom.xml` — compiler source/target 1.8, `maven-enforcer-plugin` [1.8,), `integration` profile, `maven-surefire-plugin` exclude pattern
- `Makefile` — `CHROMA_VERSIONS` variable (currently empty), `test-phase-02-parity` pattern
- `.github/workflows/integration-test.yml` — current 2-job structure with `distribution: 'adopt'`

### Secondary (MEDIUM confidence)
- `github.com/actions/setup-java` advanced usage docs — temurin distribution cached on ubuntu-latest GitHub hosted runners for JDK 8
- `www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/usage.html` — XML configuration pattern for plugin execution binding

### Tertiary (LOW confidence)
- WebSearch results on temurin JDK 8 availability on macOS (NOT relevant — CI uses ubuntu-latest)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — animal-sniffer 1.27 + java18:1.0 confirmed on Maven Central; temurin JDK support confirmed via official docs
- Architecture: HIGH — patterns derived directly from existing codebase (AbstractChromaIntegrationTest, PublicInterfaceCompatibilityTest, pom.xml) plus official GitHub Actions documentation
- Pitfalls: HIGH — startup fail-fast pitfall identified from direct code inspection; animal-sniffer false-positive behavior confirmed from official parameter docs

**Research date:** 2026-03-20
**Valid until:** 2026-06-20 (animal-sniffer and java18 artifact are stable; GitHub Actions matrix syntax is stable)
