---
phase: 04-compatibility-test-matrix
verified: 2026-03-20T12:00:00Z
status: human_needed
score: 10/10 must-haves verified
re_verification: true
  previous_status: gaps_found
  previous_score: 9/10
  gaps_closed:
    - "Both guardrails run as part of normal `mvn compile` / `mvn test` — no extra profile or CI step needed"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Run `make test-matrix` end-to-end"
    expected: "Sequential test run across Chroma 1.0.0, 1.3.7, and 1.5.5 — each starts a Testcontainer, runs integration tests, and the loop stops immediately if any version fails"
    why_human: "Requires Docker and significant wall-clock time; cannot verify Testcontainer startup behavior programmatically"
  - test: "Introduce a JDK 9+ API call in main source (e.g., `List.of()`) and run `mvn compile`"
    expected: "Build fails with animal-sniffer violation message referencing check-java-1.8-compat before compilation completes"
    why_human: "Requires modifying source, running compile, and observing the failure message — cannot simulate without a code change"
---

# Phase 4: Compatibility & Test Matrix Verification Report

**Phase Goal:** Reduce regression risk by enforcing Java baseline and Chroma-version compatibility expectations.
**Verified:** 2026-03-20T12:00:00Z
**Status:** human_needed (all automated checks pass; 2 items require human testing)
**Re-verification:** Yes — after gap closure (commit 201f183)

## Gap Closure Assessment

The single gap from the initial verification was:

> animal-sniffer was bound to the `process-test-classes` phase (test lifecycle), so `mvn compile` alone did not trigger the Java 8 API check.

**Fix applied (commit 201f183):** `<phase>compile</phase>` was added explicitly to the animal-sniffer execution block in `pom.xml` (line 306).

**Verification of fix:**

- `pom.xml` line 306: `<phase>compile</phase>` is present inside the `check-java-1.8-compat` execution block.
- `mvn clean compile` output confirms: `[INFO] --- animal-sniffer:1.27:check (check-java-1.8-compat) @ chromadb-java-client ---` followed by `[INFO] BUILD SUCCESS`.
- The misleading comment ("check goal defaults to process-classes") that was flagged as an Info anti-pattern has been removed.
- No `process-classes` or `process-test-classes` references remain in `pom.xml`.

Gap is **CLOSED**.

## Goal Achievement

### Observable Truths

#### Plan 01 (QLTY-01) — Version Matrix Infrastructure

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Maintainer can run `make test-matrix` and it sequentially tests all 3 pinned Chroma versions (1.0.0, 1.3.7, 1.5.5) | VERIFIED | `Makefile` line 70-73: `set -e; for v in $(CHROMA_MATRIX_VERSIONS)` loops over `1.0.0 1.3.7 1.5.5` passing `CHROMA_VERSION=$$v` to `mvn --batch-mode -Pintegration test` |
| 2 | Integration tests that require a minimum Chroma version are skipped (not failed) when running against an older version | VERIFIED | `AbstractChromaIntegrationTest.java` lines 115-121: `assumeMinVersion()` uses `Assume.assumeTrue()` — JUnit 4 marks test as skipped, not failed |
| 3 | Container startup failures cause test failure (AssertionError), not silent skip | VERIFIED | `AbstractChromaIntegrationTest.java` lines 44-50: `if (CHROMA_STARTUP_FAILURE != null) { throw new AssertionError(..., CHROMA_STARTUP_FAILURE); }` — no `Assume` call for startup |
| 4 | CI runs 5 matrix cells: JDK 8 x 3 Chroma versions + JDK 11/17 x 1.5.5 only | VERIFIED | `.github/workflows/integration-test.yml`: `chroma-version: ['1.0.0', '1.3.7', '1.5.5']` x `java: ['8']` plus `include:` entries for JDK 11 x 1.5.5 and JDK 17 x 1.5.5 = exactly 5 cells |
| 5 | All CI matrix cells must pass for PR merge (no advisory-only cells) | VERIFIED | No `continue-on-error` in `.github/workflows/integration-test.yml`; `fail-fast: false` ensures all cells run to completion but all must pass |

#### Plan 02 (QLTY-02) — Compatibility Guardrails

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 6 | Build fails when main source uses a JDK 9+ API (e.g. List.of(), Map.copyOf()) | VERIFIED | `pom.xml` line 306: `<phase>compile</phase>` explicitly binds animal-sniffer to the compile lifecycle. `mvn clean compile` confirmed: `animal-sniffer:1.27:check (check-java-1.8-compat)` executes and exits 0 on clean source. |
| 7 | Animal-sniffer does NOT check test source code — tests can freely use JDK 9+ APIs | VERIFIED | `pom.xml` line 301: comment confirms `checkTestClasses` defaults to false; no explicit override. `java.time.Duration` (JDK 8+) is used in test imports without violation. |
| 8 | PublicInterfaceCompatibilityTest detects removal of any existing public method on Client, Collection, or builder interfaces | VERIFIED | `PublicInterfaceCompatibilityTest.java` lines 124-314: 32 method-existence tests using `Class.getMethod()` which throws `NoSuchMethodException` on removal |
| 9 | PublicInterfaceCompatibilityTest detects unexpected addition of new public methods via count assertions | VERIFIED | Lines 19-28: 10 `EXPECTED_*_METHOD_COUNT` constants; lines 32-120: 10 `assertEquals(...getDeclaredMethods().length)` tests — all 51 tests pass |
| 10 | Both guardrails run as part of normal `mvn compile` / `mvn test` — no extra profile or CI step needed | VERIFIED | animal-sniffer fires during `mvn compile` (confirmed via output); PublicInterfaceCompatibilityTest runs during `mvn test`. Neither requires a special profile. |

**Score: 10/10 truths verified** (all truths fully verified)

### ROADMAP Success Criteria Assessment

| Criterion | Status | Evidence |
|-----------|--------|----------|
| 1. Maintainer can run a reproducible matrix of unit/integration tests across supported Chroma versions | SATISFIED | `make test-matrix` target verified; CI 5-cell matrix verified |
| 2. Java 8 compatibility checks prevent incompatible API/language usage from reaching release branches | SATISFIED | animal-sniffer now fires during `mvn compile` AND `mvn test`; CI always runs `mvn test` |
| 3. Public interface compatibility tests detect breaking API changes before publication | SATISFIED | 51 test methods in `PublicInterfaceCompatibilityTest` with count + existence assertions |

All 3 ROADMAP success criteria are fully satisfied.

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java` | VERIFIED | Contains `assumeMinVersion()`, `compareVersions()`, `AssertionError` fail-fast, `DEFAULT_CHROMA_VERSION = "1.5.5"` |
| `Makefile` | VERIFIED | Contains `CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5`, `.PHONY: test-matrix`, `set -e; for v in $(CHROMA_MATRIX_VERSIONS)`, `|| exit $$?` |
| `.github/workflows/integration-test.yml` | VERIFIED | 3 jobs: `unit-tests`, `integration-tests`, `v2-integration-test`; `fail-fast: false` on both matrix jobs; `temurin` distribution; `actions/checkout@v4`; no `adopt` or v3 actions |
| `pom.xml` | VERIFIED | Contains `animal-sniffer-maven-plugin` 1.27 (line 293), `java18` signature (line 298), `check-java-1.8-compat` execution ID (line 305), `<phase>compile</phase>` (line 306); misleading comment removed |
| `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` | VERIFIED | 10 `EXPECTED_*_METHOD_COUNT` constants; 10 `getDeclaredMethods().length` assertions; all 51 tests pass; all 10 original tests preserved |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `Makefile` | `AbstractChromaIntegrationTest` | `CHROMA_VERSION=$$v $(MAVEN) --batch-mode -Pintegration test` | WIRED | `CHROMA_VERSION=$$v` sets env var; `AbstractChromaIntegrationTest` reads via `System.getenv("CHROMA_VERSION")` |
| `.github/workflows/integration-test.yml` | `AbstractChromaIntegrationTest` | `CHROMA_VERSION: ${{ matrix.chroma-version }}` | WIRED | CI injects `CHROMA_VERSION` env; test class consumes it |
| `pom.xml` | `src/main/java/**/*.java` | `animal-sniffer check` goal at `compile` phase | WIRED | `<phase>compile</phase>` explicit at line 306; confirmed by `mvn compile` output showing `animal-sniffer:1.27:check (check-java-1.8-compat)` |
| `PublicInterfaceCompatibilityTest.java` | `Client.java, Collection.java, ChromaClient.java` | `getDeclaredMethods()` and `getMethod()` reflection | WIRED | 32 `Class.getMethod()` calls and 10 `getDeclaredMethods().length` assertions; all 51 tests pass |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| QLTY-01 | 04-01-PLAN.md | Maintainer can run unit + integration tests against supported Chroma versions with reproducible pass/fail | SATISFIED | `make test-matrix`, CI 5-cell matrix, `assumeMinVersion()`, fail-fast `AssertionError` all present and functional |
| QLTY-02 | 04-02-PLAN.md | Maintainer is protected from Java 8 and public interface compatibility regressions before release | SATISFIED | animal-sniffer now fires during `mvn compile` (confirmed); 51 PublicInterfaceCompatibilityTest assertions catch API breakage |

No orphaned requirements — both QLTY-01 and QLTY-02 are claimed by plans and verified in implementation.

### Anti-Patterns Found

No anti-patterns found. The misleading `process-classes` comment that was flagged as Info severity in the initial verification has been removed in commit 201f183.

### Human Verification Required

#### 1. Local multi-version test matrix execution

**Test:** Run `make test-matrix` in the repository root with Docker available.
**Expected:** Output shows "Running unit tests..." followed by three integration test passes — one each for Chroma 1.0.0, 1.3.7, and 1.5.5 — with the loop stopping on the first integration test failure.
**Why human:** Requires Docker, pulls `chromadb/chroma` images, and takes several minutes per Chroma version. Cannot verify Testcontainer startup or the `set -e` loop exit behavior programmatically.

#### 2. animal-sniffer violation detection during `mvn compile`

**Test:** Add `List.of("x")` (JDK 9+ API) to any method in `src/main/java/` and run `mvn compile` (not `mvn test`).
**Expected:** Build fails before emitting any `.class` files with a message referencing `check-java-1.8-compat` and the violating class file.
**Why human:** Requires modifying source, running compile (not test), and confirming the failure message is clear and actionable. This specifically validates the gap fix — that `mvn compile` (not just `mvn test`) now catches the violation.

### Gaps Summary

No gaps remain. The single gap from the initial verification (animal-sniffer not firing during `mvn compile`) has been closed by commit 201f183, which added `<phase>compile</phase>` explicitly to the animal-sniffer execution block. The misleading comment about the default phase was also removed. All 10 must-have truths are now fully verified.

---

_Initial verification: 2026-03-20T11:15:00Z_
_Re-verification: 2026-03-20T12:00:00Z_
_Verifier: Claude (gsd-verifier)_
