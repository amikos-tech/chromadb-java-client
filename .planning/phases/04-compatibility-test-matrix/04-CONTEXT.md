# Phase 4: Compatibility & Test Matrix - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Reduce regression risk by enforcing Java baseline and Chroma-version compatibility expectations. Deliver a reproducible test matrix across supported Chroma versions and JDK LTS releases, Java 8 API-level enforcement via build tooling, expanded public interface compatibility tests, and a CI workflow that gates PRs on all supported configurations. This phase does not add new product capabilities or change client behavior.

</domain>

<decisions>
## Implementation Decisions

### Chroma version matrix
- Matrix anchors: **1.0.0** (oldest supported), **1.3.7** (mid-range), and a **pinned latest** (currently 1.5.5). Three versions total.
- "Latest" is pinned to a specific version in config, bumped manually when new Chroma releases drop. No dynamic `:latest` tag.
- Version list centralized in a **Makefile variable** (`CHROMA_MATRIX_VERSIONS`). CI workflow maintains its own matrix definition but should match.
- A single `make test-matrix` target runs unit tests + integration tests sequentially across all 3 pinned versions.
- Version-specific test skips use an **annotation-based** approach (e.g., `@MinChromaVersion("1.3.0")` or `Assume.assumeTrue` with version comparison) so one test suite covers all versions without duplication.
- Container startup failures **fail fast** — if a Chroma container can't start for a version, that matrix cell fails (not skipped).

### JDK version matrix
- Test across **JDK 8, 11, and 17** (all LTS releases).
- Matrix shape: **JDK 8 tests all 3 Chroma versions**. JDK 11 and 17 test only the latest pinned Chroma version. 5 total combinations.

### Java 8 API enforcement
- Add **animal-sniffer-maven-plugin** with `java18` (JDK 1.8) API signature.
- Bound to **default lifecycle** (every build) — catches JDK 9+ API usage immediately during local `mvn compile`.
- **Test code excluded** from animal-sniffer — only main source is enforced. Tests can use JDK 9+ APIs.
- Existing `maven-enforcer-plugin` `requireJavaVersion [1.8,)` rule kept as-is — no tightening needed.

### Public API breakage detection
- **Expand the existing reflection-based `PublicInterfaceCompatibilityTest`** rather than adding japicmp/revapi.
- Scope: **public interfaces + builders** — `Client`, `Collection`, `Collection.*Builder`, `ChromaClient.Builder`, `ChromaClient.CloudBuilder`.
- Assertions per method: **existence + return type** (and default-method status for interface methods that must remain default).
- Surface area growth detection via **count-based check** — assert total public method count on each key interface. Adding a new public method requires updating the expected count.

### CI gating strategy
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirement contracts
- `.planning/ROADMAP.md` — Phase 4 goal, success criteria, plan scaffolding (QLTY-01, QLTY-02).
- `.planning/REQUIREMENTS.md` — QLTY-01/QLTY-02 acceptance targets.
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8, sync API).

### Project conventions and architecture
- `CLAUDE.md` — repository conventions, test commands, architecture notes.

### Existing test infrastructure
- `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` — Testcontainers base class with `CHROMA_VERSION` env-var switching.
- `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` — existing reflection-based public API compatibility checks (expand this).

### Build configuration
- `pom.xml` — Maven build config: compiler source/target 1.8, enforcer plugin, surefire config, quality/mutation/integration profiles.
- `Makefile` — Local dev targets including `test-version`, `test-phase-02-parity`, `test-matrix` (to be added).

### CI workflows
- `.github/workflows/integration-test.yml` — current CI workflow (2 jobs, JDK 8 only, Chroma 1.0.0 + 1.5.2). Needs matrix expansion.

### Prior phase context
- `.planning/phases/02-api-coverage-completion/02-CONTEXT.md` — Phase 2 established Chroma 1.5.5 + 1.3.7 as representative version pair.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AbstractChromaIntegrationTest` already handles `CHROMA_VERSION` env-var based container creation. Supports version switching out of the box.
- `PublicInterfaceCompatibilityTest` already has reflection-based method signature assertions. Expand rather than replace.
- `Makefile` already has `test-version`, `test-phase-02-parity` targets. Pattern established for version-specific test targets.
- `pom.xml` already has `maven-enforcer-plugin`, `quality` profile with JaCoCo, `integration` profile with surefire include override.

### Established Patterns
- Surefire default excludes `*IntegrationTest.java`; `integration` profile includes them. This split enables the separate-jobs CI strategy.
- `Assume.assumeTrue` is already used for container startup gating. Same pattern can be extended for version-conditional test skips.
- Maven profiles (`quality`, `mutation`, `integration`) are the established mechanism for build variants.

### Integration Points
- Animal-sniffer plugin integrates into `pom.xml` `<build><plugins>` section alongside existing compiler and enforcer plugins.
- Version-skip annotation/utility integrates into `AbstractChromaIntegrationTest` and any tests that need version-conditional behavior.
- CI matrix replaces the two existing jobs in `integration-test.yml` with parameterized matrix jobs.
- `make test-matrix` target integrates into the existing Makefile conventions.

</code_context>

<specifics>
## Specific Ideas

- Container startup failures should fail the matrix cell, not skip — maintainer wants strict regression signals.
- JDK 8 + 11 + 17 coverage but with a smart matrix shape (8 full, 11/17 latest only) to keep CI time reasonable.
- The version list is centralized in the Makefile for local dev. CI can mirror it but maintains its own definition.
- No fail-fast in CI — all cells run to completion so the full regression picture is visible in one run.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 4 scope.

</deferred>

---

*Phase: 04-compatibility-test-matrix*
*Context gathered: 2026-03-20*
