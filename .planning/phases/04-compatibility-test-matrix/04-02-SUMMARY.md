---
phase: 04-compatibility-test-matrix
plan: "02"
subsystem: testing
tags: [java8, animal-sniffer, reflection, compatibility, maven, pom]

requires:
  - phase: 04-compatibility-test-matrix/04-01
    provides: Initial PublicInterfaceCompatibilityTest with 10 baseline tests

provides:
  - animal-sniffer-maven-plugin 1.27 bound to process-test-classes phase checking main source against Java 1.8 API signatures
  - Expanded PublicInterfaceCompatibilityTest with method-count assertions for 10 public types and method-existence checks for key Client/Collection/Builder/CloudBuilder methods
  - 51 total test assertions guarding public interface stability

affects:
  - future-api-changes: any public method add/remove on Client/Collection/Builders requires updating test counts
  - ci: animal-sniffer will fail mvn test if JDK 9+ API is used in main source

tech-stack:
  added:
    - org.codehaus.mojo:animal-sniffer-maven-plugin:1.27
    - org.codehaus.mojo.signature:java18:1.0 (signature artifact)
  patterns:
    - getDeclaredMethods().length count assertions as public-API change detection gate
    - Method-existence + return-type assertions via reflection for compile-time-safe API surface checks

key-files:
  created: []
  modified:
    - pom.xml
    - src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java

key-decisions:
  - "animal-sniffer check goal defaults to process-test-classes (not process-classes as plan stated) — runs during mvn test, satisfying the mvn test coverage requirement without requiring an explicit phase override"
  - "EXPECTED_BUILDER_METHOD_COUNT=34 and EXPECTED_CLOUD_BUILDER_METHOD_COUNT=8 reflect getDeclaredMethods() actual counts (includes all declared methods, not just public), which is what the plan specified"

patterns-established:
  - "Public API surface guard: method count constants + getDeclaredMethods().length assertions catch accidental adds/removes"
  - "Method-existence + return-type assertions: Class.getMethod() with parameter types confirms exact signature stability"

requirements-completed: [QLTY-02]

duration: 15min
completed: "2026-03-20"
---

# Phase 04 Plan 02: Compatibility Guardrails Summary

**Java 8 API compat enforced by animal-sniffer-maven-plugin 1.27 and PublicInterfaceCompatibilityTest expanded to 51 assertions guarding all 10 public types via method-count and method-existence checks**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-03-20T10:43:00Z
- **Completed:** 2026-03-20T11:01:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added animal-sniffer-maven-plugin 1.27 to pom.xml checking main source against java18:1.0 signature; runs automatically during `mvn test` via process-test-classes phase
- Expanded PublicInterfaceCompatibilityTest from 10 to 51 test methods: 10 method-count assertions (one per public type) + 32 method-existence/return-type assertions + 9 preserved original tests
- Adding or removing any public method on Client, Collection, the 6 builders, Builder, or CloudBuilder now requires an intentional update to test count constants

## Task Commits

Each task was committed atomically:

1. **Task 1: Add animal-sniffer-maven-plugin to pom.xml** - `8903bf5` (chore)
2. **Task 2: Expand PublicInterfaceCompatibilityTest with full coverage** - `077b2df` (feat)

## Files Created/Modified

- `pom.xml` - Added animal-sniffer-maven-plugin 1.27 with java18:1.0 signature, execution ID `check-java-1.8-compat`, bound to default `process-test-classes` phase
- `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java` - Expanded from 82 to 389 lines; 10 count-assertion constants + 51 test methods covering all public types

## Decisions Made

- **animal-sniffer default phase is process-test-classes**: The plan stated the `check` goal defaults to `process-classes`, but inspection of the plugin's `plugin.xml` confirms the actual default is `process-test-classes`. Per plan instructions ("Do NOT add an explicit `<phase>` element"), the default was kept. The plugin runs during `mvn test` (not `mvn compile`), satisfying the "runs as part of normal `mvn test`" requirement.
- **getDeclaredMethods() counts include private methods for concrete classes**: For interfaces (Client, Collection, builders), all declared methods are public. For concrete classes Builder and CloudBuilder, getDeclaredMethods() returns all methods including private helpers. Actual counts (Builder=34, CloudBuilder=8) were used instead of public-only counts (20, 6) per the plan instruction to use actual values when counts differ.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected EXPECTED_BUILDER_METHOD_COUNT and EXPECTED_CLOUD_BUILDER_METHOD_COUNT**
- **Found during:** Task 2 (test execution)
- **Issue:** Plan specified Builder=20 and CloudBuilder=6 based on public-method counting, but getDeclaredMethods() on concrete classes returns all declared methods (including private). Actual counts: Builder=34, CloudBuilder=8.
- **Fix:** Updated constants to actual getDeclaredMethods() values (34 and 8) per plan instruction: "If the counts differ from above, use the actual values and note the discrepancy."
- **Files modified:** `src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java`
- **Verification:** All 51 tests pass with corrected values.
- **Committed in:** 077b2df (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (incorrect constant values caught by first test run)
**Impact on plan:** Correction was required for test correctness. No scope creep. All acceptance criteria met.

## Issues Encountered

- animal-sniffer plugin pom.xml stated "the `check` goal defaults to `process-classes`" but the actual plugin.xml shows `process-test-classes`. This means animal-sniffer does not run during `mvn compile` but does run during `mvn test`. This is acceptable per the must_haves truth "Both guardrails run as part of normal `mvn compile` / `mvn test`" — the `mvn test` part is satisfied.

## Next Phase Readiness

- QLTY-02 delivered: Java 8 API compat and public interface stability are both guarded
- Both guardrails run during normal `mvn test` without any extra profile or CI step
- Any future public API change to Client, Collection, or builders will require updating count constants — this is intentional and documents the change

---
*Phase: 04-compatibility-test-matrix*
*Completed: 2026-03-20*

## Self-Check: PASSED

- FOUND: pom.xml
- FOUND: PublicInterfaceCompatibilityTest.java
- FOUND: 04-02-SUMMARY.md
- FOUND commit 8903bf5 (Task 1)
- FOUND commit 077b2df (Task 2)
