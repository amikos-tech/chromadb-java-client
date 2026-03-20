---
phase: 06-tech-debt-cleanup
plan: "02"
subsystem: ci-infra
tags: [ci, release, nd4j, dependency, github-actions]
dependency_graph:
  requires: []
  provides: [corrected-release-trigger, release-check-ci-gate, nd4j-1.0.0-M2.1]
  affects: [.github/workflows/release.yml, pom.xml]
tech_stack:
  added: []
  patterns: [make-release-check-gate, release-trigger-hygiene]
key_files:
  created: []
  modified:
    - .github/workflows/release.yml
    - pom.xml
decisions:
  - "branches filter removed from release trigger: GitHub Actions ignores branches on release events (only applies to push/pull_request)"
  - "release-check step inserted after Version bump and before Publish package to validate non-SNAPSHOT version in CHANGELOG/README"
  - "nd4j-native-platform bumped from 1.0.0-M2 to 1.0.0-M2.1 (latest patch, no transitive conflicts)"
metrics:
  duration: "~4 min"
  completed_date: "2026-03-20"
  tasks: 2
  files_modified: 2
---

# Phase 06 Plan 02: CI Workflow Fixes and nd4j Patch Summary

**One-liner:** Removed misleading branches filter from release trigger, added make release-check CI gate after version bump, and bumped nd4j-native-platform to 1.0.0-M2.1.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Fix release.yml: remove branches filter and add release-check step | 4eb3bd6 | .github/workflows/release.yml |
| 2 | Bump nd4j-native-platform to 1.0.0-M2.1 | 31dbc60 | pom.xml |

## What Was Built

### Task 1: release.yml Corrections (INFRA-1, INFRA-2)

**INFRA-1 — Remove branches filter:** The `branches: ["main"]` line under `on.release` was removed. GitHub Actions ignores the `branches:` filter on `release:` events (it only applies to `push:` and `pull_request:`). The filter provided false confidence about branch restriction. Removing it makes the YAML accurate.

**INFRA-2 — Add release-check gate:** A new `Run release-check` step was inserted after `Version bump` and before `Publish package`. This step invokes `make release-check`, which validates that:
- The version in pom.xml is non-SNAPSHOT
- CHANGELOG.md contains an entry for the release version
- README.md references the release version
- No stale TODO markers exist
- Artifacts are present (if target/ exists)

Placement after Version bump is critical: release-check validates the bumped (non-SNAPSHOT) version. Running it before Version bump would cause it to fail with "Version contains -SNAPSHOT".

**Final step order in release.yml:**
1. actions/checkout@v4
2. Set up JDK 8
3. Install gpg secret key
4. Run unit tests
5. Run integration tests
6. Version bump
7. Run release-check (NEW)
8. Publish package

### Task 2: nd4j-native-platform Bump (ND4J-BUMP)

Updated `nd4j-native-platform` from `1.0.0-M2` to `1.0.0-M2.1` in pom.xml. The 1.0.0-M2.1 patch resolves correctly with no transitive dependency conflicts (verified via `mvn dependency:tree`). The ARM64 UnsatisfiedLinkError is not fixed in M2.1 (inherent nd4j 1.x limitation on Apple Silicon) but is handled gracefully by the existing `Assume.assumeNoException` guard in `TestDefaultEmbeddings.checkNd4jAvailable()`. CI runs on ubuntu-latest (x86_64) so no CI impact.

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- `grep -c "branches:" .github/workflows/release.yml` returns 0 — PASS
- `grep -c "make release-check" .github/workflows/release.yml` returns 1 — PASS
- `grep "1.0.0-M2.1" pom.xml` returns the nd4j dependency line — PASS
- `grep -c '1.0.0-M2"' pom.xml` returns 0 (old version fully replaced) — PASS
- `mvn test-compile -q` exits 0 — PASS
- Step ordering: Run release-check at char 1409, after Version bump (1161) and before Publish package (1473) — PASS

## Self-Check: PASSED

Files exist:
- `.github/workflows/release.yml` — FOUND
- `pom.xml` — FOUND

Commits:
- `4eb3bd6` — FOUND
- `31dbc60` — FOUND
