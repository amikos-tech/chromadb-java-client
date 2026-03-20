---
phase: 5
slug: documentation-release-readiness
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-20
validated: 2026-03-20
---

# Phase 5 -- Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 |
| **Config file** | pom.xml (surefire plugin; `-Pintegration` for integration tests) |
| **Quick run command** | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~2 seconds (39 file-based assertions) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** `make release-check` must fail only on SNAPSHOT version (expected)
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File / Tests | Status |
|---------|------|------|-------------|-----------|-------------------|--------------|--------|
| 05-01-01 | 01 | 1 | QLTY-03 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | `Phase05DocumentationReleaseReadinessTest` (15 tests: readme tone, v2 quick start, auth examples, cloud, status, appendix, lifecycle, querying, upgrading, internal sections removed) | green |
| 05-01-02 | 01 | 1 | QLTY-03 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest#test_readme_maven_dependency_references_020` | `test_readme_maven_dependency_references_020` | green |
| 05-01-03 | 01 | 1 | QLTY-03 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | MIGRATION.md tests (5 tests: breaking changes, mapping table, before/after, v2 refs, readme link) | green |
| 05-01-04 | 01 | 1 | QLTY-03 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | CHANGELOG.md tests (5 tests: format, unreleased, 0.2.0 entry, subsections, github link) | green |
| 05-01-05 | 01 | 1 | QLTY-03 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest#test_readme_links_to_migration` | Cross-file link validation | green |
| 05-02-01 | 02 | 1 | QLTY-04 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | Makefile release-check tests (5 tests: target exists, SNAPSHOT check, CHANGELOG, README, TODO) | green |
| 05-02-02 | 02 | 1 | QLTY-04 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | Makefile release-dry-run tests (4 tests: target exists, verify phase, gpg skip, calls release-check) | green |
| 05-02-03 | 02 | 1 | QLTY-04 | smoke | `make release-check` | Expected to fail with "SNAPSHOT" error (correct behavior for pre-release state) | green |
| 05-02-04 | 02 | 1 | QLTY-04 | unit | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` | release.yml tests (5 tests: v4 actions, temurin, no skipTests, unit tests before deploy, integration tests before deploy) | green |

*Status: green (all 39 tests pass, smoke test behaves correctly)*

---

## Wave 0 Requirements

- [x] `CHANGELOG.md` -- created by 05-01, validated by tests
- [x] `Makefile` `release-check` target -- created by 05-02, validated by tests
- [x] `Makefile` `release-dry-run` target -- created by 05-02, validated by tests

---

## Test File

| File | Type | Tests | Command |
|------|------|-------|---------|
| `src/test/java/tech/amikos/chromadb/v2/Phase05DocumentationReleaseReadinessTest.java` | unit | 39 | `mvn test -Dtest=Phase05DocumentationReleaseReadinessTest` |

### Test Coverage Breakdown

**QLTY-03 (Documentation quality) -- 25 tests:**
- `test_readme_opens_with_professional_tone` -- README leads with professional description
- `test_readme_contains_no_self_deprecating_language` -- no "very basic" or "naive implementation"
- `test_readme_contains_no_note_blockquote` -- no old NOTE blockquote
- `test_readme_has_quick_start_with_builder` -- Quick Start section with ChromaClient.builder()
- `test_readme_has_v2_auth_examples` -- BasicAuth, TokenAuth, ChromaTokenAuth sections with factory methods
- `test_readme_has_cloud_example` -- Cloud section with ChromaClient.cloud()
- `test_readme_maven_dependency_references_020` -- Maven dependency shows 0.2.0
- `test_readme_has_status_section_not_features_or_todo` -- Status replaces Features/TODO
- `test_readme_has_collapsed_v1_appendix` -- v1 appendix in details/summary tags
- `test_readme_has_collection_lifecycle_section` -- Collection Lifecycle section present
- `test_readme_has_querying_section` -- Querying section present
- `test_readme_has_upgrading_section_with_migration_link` -- Upgrading section links MIGRATION.md
- `test_readme_has_no_internal_sections` -- no internal maintainer sections
- `test_migration_has_breaking_changes` -- MIGRATION.md has Breaking Changes section
- `test_migration_has_v1_to_v2_mapping_table` -- MIGRATION.md has mapping table with pipe characters
- `test_migration_has_before_after_examples` -- MIGRATION.md has connect/add/query examples
- `test_migration_references_v2_api` -- MIGRATION.md references ChromaClient.builder() and v2 package
- `test_migration_links_to_readme` -- MIGRATION.md links back to README.md
- `test_changelog_follows_keep_a_changelog_format` -- Keep a Changelog and Semantic Versioning links
- `test_changelog_has_unreleased_section` -- [Unreleased] section present
- `test_changelog_has_020_entry` -- [0.2.0] entry present
- `test_changelog_has_required_subsections` -- Added/Changed/Removed/Fixed sections
- `test_changelog_has_github_link_reference` -- GitHub link reference at bottom
- `test_readme_links_to_migration` -- README -> MIGRATION.md cross-link

**QLTY-04 (Release readiness) -- 14 tests:**
- `test_makefile_has_release_check_target` -- target exists with PHONY
- `test_release_check_validates_snapshot_version` -- SNAPSHOT validation
- `test_release_check_validates_changelog_entry` -- CHANGELOG.md grep
- `test_release_check_validates_readme_version` -- README.md grep
- `test_release_check_validates_no_stale_todo` -- TODO section detection
- `test_makefile_has_release_dry_run_target` -- target exists with PHONY
- `test_release_dry_run_uses_verify_phase` -- uses "clean verify" not "clean package"
- `test_release_dry_run_skips_gpg` -- -Dgpg.skip=true
- `test_release_dry_run_calls_release_check` -- invokes release-check
- `test_release_yml_uses_v4_actions` -- checkout@v4 and setup-java@v4
- `test_release_yml_uses_temurin` -- temurin distribution
- `test_release_yml_has_no_skip_tests` -- no DskipTests anywhere
- `test_release_yml_runs_unit_tests_before_deploy` -- unit tests step before Version bump
- `test_release_yml_runs_integration_tests_before_deploy` -- integration tests step before Version bump
- `test_release_yml_triggers_on_release_creation` -- triggers on release types: [created]

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| README v2 examples are syntactically valid Java that compiles against current v2 API | QLTY-03 | Code snippets in markdown cannot be extracted and compiled by the JUnit test framework without a custom parser | Copy Quick Start code block from README into a test project with 0.2.0 dependency; verify it compiles and runs against a local ChromaDB instance |
| `make release-check` passes with non-SNAPSHOT version | QLTY-04 | Requires temporarily modifying pom.xml version which would be a destructive change | Set pom.xml version to `0.2.0`, run `make release-check`, verify all checks pass |

---

## Validation Execution Results

**Run date:** 2026-03-20
**Test count:** 39
**Passed:** 39
**Failed:** 0
**Skipped:** 0
**Runtime:** 0.034 seconds

**Smoke test:** `make release-check` correctly fails with "FAIL: Version 0.2.0-SNAPSHOT contains -SNAPSHOT" (expected behavior for pre-release state).

---

## Validation Sign-Off

- [x] All tasks have automated verify commands
- [x] Sampling continuity: all tasks covered by single test class (0 gaps)
- [x] Wave 0 deliverables created and validated
- [x] No watch-mode flags
- [x] Feedback latency < 5s (39 tests in 0.034s)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated
