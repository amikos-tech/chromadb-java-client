package tech.amikos.chromadb.v2;

import org.junit.BeforeClass;
import org.junit.Test;

import tech.amikos.chromadb.ProjectFileTestHelper;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Phase 05 Nyquist validation tests.
 *
 * Validates documentation and release readiness requirements:
 * - QLTY-03: User can follow README examples for v2 auth, schema, lifecycle, and query workflows
 * - QLTY-04: Maintainer can produce Maven Central-ready artifacts through a repeatable release flow
 *
 * These tests read project root files (README.md, MIGRATION.md, CHANGELOG.md, Makefile, release.yml)
 * and assert behavioral content requirements without modifying any implementation.
 */
public class Phase05DocumentationReleaseReadinessTest {

    private static String readme;
    private static String migration;
    private static String changelog;
    private static String makefile;
    private static String releaseYml;
    private static Path projectRoot;

    @BeforeClass
    public static void loadFiles() throws IOException {
        projectRoot = ProjectFileTestHelper.findProjectRoot();

        readme = ProjectFileTestHelper.readFile(projectRoot.resolve("README.md"));
        migration = ProjectFileTestHelper.readFile(projectRoot.resolve("MIGRATION.md"));
        changelog = ProjectFileTestHelper.readFile(projectRoot.resolve("CHANGELOG.md"));
        makefile = ProjectFileTestHelper.readFile(projectRoot.resolve("Makefile"));
        releaseYml = ProjectFileTestHelper.readFile(projectRoot.resolve(".github/workflows/release.yml"));
    }

    // -----------------------------------------------------------------------
    // QLTY-03: README professional tone and v2-first structure
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_opens_with_professional_tone() {
        assertTrue("README should lead with 'Production-ready Java client'",
                readme.contains("Production-ready Java client for ChromaDB v2 API"));
    }

    @Test
    public void test_readme_contains_no_self_deprecating_language() {
        assertFalse("README must not contain 'very basic' self-deprecating text",
                readme.contains("very basic"));
        assertFalse("README must not contain 'naive implementation' self-deprecating text",
                readme.contains("naive implementation"));
    }

    @Test
    public void test_readme_contains_no_note_blockquote() {
        assertFalse("README must not contain the old NOTE blockquote",
                readme.contains("![NOTE]"));
    }

    @Test
    public void test_readme_has_quick_start_with_builder() {
        assertTrue("README must contain '## Quick Start' section",
                readme.contains("## Quick Start"));
        assertTrue("README Quick Start must use ChromaClient.builder()",
                readme.contains("ChromaClient.builder()"));
    }

    @Test
    public void test_readme_has_v2_auth_examples() {
        assertTrue("README must have '#### Basic Auth' section",
                readme.contains("#### Basic Auth"));
        assertTrue("README must have '#### Token Auth' section",
                readme.contains("#### Token Auth"));
        assertTrue("README must have '#### Chroma Token Auth' section",
                readme.contains("#### Chroma Token Auth"));
        // Auth examples must use factory methods
        assertTrue("README Basic Auth example must use BasicAuth.of(...)",
                readme.contains("BasicAuth.of("));
        assertTrue("README Token Auth example must use TokenAuth.of(...)",
                readme.contains("TokenAuth.of("));
        assertTrue("README Chroma Token Auth example must use ChromaTokenAuth.of(...)",
                readme.contains("ChromaTokenAuth.of("));
    }

    @Test
    public void test_readme_has_cloud_example() {
        assertTrue("README must have '### Cloud (Chroma Cloud)' section",
                readme.contains("### Cloud (Chroma Cloud)"));
        assertTrue("README Cloud section must use ChromaClient.cloud()",
                readme.contains("ChromaClient.cloud()"));
    }

    @Test
    public void test_readme_maven_dependency_references_020() {
        assertTrue("README Maven dependency must reference version 0.2.0",
                readme.contains("<version>0.2.0</version>"));
    }

    @Test
    public void test_readme_has_status_section_not_features_or_todo() {
        assertTrue("README must contain '## Status' section",
                readme.contains("## Status"));
        assertFalse("README must NOT contain standalone '## Features' section",
                readme.contains("## Features"));
        assertFalse("README must NOT contain '## TODO' section",
                readme.contains("## TODO"));
    }

    @Test
    public void test_readme_has_collapsed_v1_appendix() {
        assertTrue("README must have v1 appendix heading",
                readme.contains("## Appendix: v1 API Examples (Legacy)"));
        assertTrue("README must use <details> tag for v1 collapse",
                readme.contains("<details>"));
        assertTrue("README must have summary tag for expand",
                readme.contains("<summary>"));
    }

    @Test
    public void test_readme_has_collection_lifecycle_section() {
        assertTrue("README must have '### Collection Lifecycle' section",
                readme.contains("### Collection Lifecycle"));
    }

    @Test
    public void test_readme_has_querying_section() {
        assertTrue("README must have '### Querying' section",
                readme.contains("### Querying"));
    }

    @Test
    public void test_readme_has_upgrading_section_with_migration_link() {
        assertTrue("README must have '## Upgrading from 0.1.x' section",
                readme.contains("## Upgrading from 0.1.x"));
        assertTrue("README Upgrading section must link to MIGRATION.md",
                readme.contains("MIGRATION.md"));
    }

    @Test
    public void test_readme_has_no_internal_sections() {
        assertFalse("README must not contain 'Phase 1 Auth Hardening Regression Commands'",
                readme.contains("Phase 1 Auth Hardening Regression Commands"));
        assertFalse("README must not contain 'v2 Auth Contract (Maintainer Rule)'",
                readme.contains("v2 Auth Contract (Maintainer Rule)"));
        assertFalse("README must not contain 'Error Mapping-Change Governance'",
                readme.contains("Error Mapping-Change Governance"));
    }

    // -----------------------------------------------------------------------
    // QLTY-03: MIGRATION.md structure and content
    // -----------------------------------------------------------------------

    @Test
    public void test_migration_has_breaking_changes() {
        assertTrue("MIGRATION.md must contain '## Breaking Changes'",
                migration.contains("## Breaking Changes"));
    }

    @Test
    public void test_migration_has_v1_to_v2_mapping_table() {
        assertTrue("MIGRATION.md must contain '## v1 to v2 Mapping'",
                migration.contains("## v1 to v2 Mapping"));
        // Verify it has a table (pipe characters)
        assertTrue("MIGRATION.md mapping section must contain a markdown table",
                migration.contains("| v1 Pattern"));
    }

    @Test
    public void test_migration_has_before_after_examples() {
        assertTrue("MIGRATION.md must contain '## Before and After Examples'",
                migration.contains("## Before and After Examples"));
        // Must have at least connect, add, query examples
        assertTrue("MIGRATION.md must have Connecting example",
                migration.contains("### Connecting"));
        assertTrue("MIGRATION.md must have Adding Documents example",
                migration.contains("### Adding Documents"));
        assertTrue("MIGRATION.md must have Querying example",
                migration.contains("### Querying"));
    }

    @Test
    public void test_migration_references_v2_api() {
        assertTrue("MIGRATION.md must reference ChromaClient.builder()",
                migration.contains("ChromaClient.builder()"));
        assertTrue("MIGRATION.md must reference tech.amikos.chromadb.v2",
                migration.contains("tech.amikos.chromadb.v2"));
    }

    @Test
    public void test_migration_links_to_readme() {
        assertTrue("MIGRATION.md must link to README.md",
                migration.contains("README.md"));
    }

    // -----------------------------------------------------------------------
    // QLTY-03: CHANGELOG.md format and content
    // -----------------------------------------------------------------------

    @Test
    public void test_changelog_follows_keep_a_changelog_format() {
        assertTrue("CHANGELOG.md must reference Keep a Changelog",
                changelog.contains("Keep a Changelog"));
        assertTrue("CHANGELOG.md must reference Semantic Versioning",
                changelog.contains("Semantic Versioning"));
    }

    @Test
    public void test_changelog_has_unreleased_section() {
        assertTrue("CHANGELOG.md must contain '## [Unreleased]'",
                changelog.contains("## [Unreleased]"));
    }

    @Test
    public void test_changelog_has_020_entry() {
        assertTrue("CHANGELOG.md must contain '## [0.2.0]' entry",
                changelog.contains("## [0.2.0]"));
    }

    @Test
    public void test_changelog_has_required_subsections() {
        assertTrue("CHANGELOG.md must contain '### Added' section",
                changelog.contains("### Added"));
        assertTrue("CHANGELOG.md must contain '### Changed' section",
                changelog.contains("### Changed"));
        assertTrue("CHANGELOG.md must contain '### Removed' section",
                changelog.contains("### Removed"));
        assertTrue("CHANGELOG.md must contain '### Fixed' section",
                changelog.contains("### Fixed"));
    }

    @Test
    public void test_changelog_has_github_link_reference() {
        assertTrue("CHANGELOG.md must have link reference for [0.2.0]",
                changelog.contains("[0.2.0]: https://github.com/amikos-tech/chromadb-java-client/"));
    }

    // -----------------------------------------------------------------------
    // QLTY-03: Cross-file links
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_links_to_migration() {
        assertTrue("README must link to MIGRATION.md in Upgrading section",
                readme.contains("[MIGRATION.md](MIGRATION.md)"));
    }

    // -----------------------------------------------------------------------
    // QLTY-04: Makefile release-check target
    // -----------------------------------------------------------------------

    @Test
    public void test_makefile_has_release_check_target() {
        assertTrue("Makefile must contain 'release-check:' target",
                makefile.contains("release-check:"));
        assertTrue("Makefile must declare release-check as PHONY",
                makefile.contains(".PHONY: release-check"));
    }

    @Test
    public void test_release_check_validates_snapshot_version() {
        assertTrue("release-check must check for SNAPSHOT in version",
                makefile.contains("SNAPSHOT"));
    }

    @Test
    public void test_release_check_validates_changelog_entry() {
        assertTrue("release-check must validate CHANGELOG.md entry",
                makefile.contains("CHANGELOG.md"));
    }

    @Test
    public void test_release_check_validates_readme_version() {
        // The release-check target greps README.md for the current version
        assertTrue("release-check must validate README.md version reference",
                makefile.contains("README.md"));
    }

    @Test
    public void test_release_check_validates_no_stale_todo() {
        assertTrue("release-check must check for stale TODO section",
                makefile.contains("## TODO"));
    }

    // -----------------------------------------------------------------------
    // QLTY-04: Makefile release-dry-run target
    // -----------------------------------------------------------------------

    @Test
    public void test_makefile_has_release_dry_run_target() {
        assertTrue("Makefile must contain 'release-dry-run:' target",
                makefile.contains("release-dry-run:"));
        assertTrue("Makefile must declare release-dry-run as PHONY",
                makefile.contains(".PHONY: release-dry-run"));
    }

    @Test
    public void test_release_dry_run_uses_verify_phase() {
        assertTrue("release-dry-run must use 'clean verify' (not 'clean package')",
                makefile.contains("clean verify"));
    }

    @Test
    public void test_release_dry_run_skips_gpg() {
        assertTrue("release-dry-run must pass -Dgpg.skip=true",
                makefile.contains("-Dgpg.skip=true"));
    }

    @Test
    public void test_release_dry_run_calls_release_check() {
        assertTrue("release-dry-run must invoke release-check",
                makefile.contains("release-check"));
    }

    // -----------------------------------------------------------------------
    // QLTY-04: release.yml test-gated workflow
    // -----------------------------------------------------------------------

    @Test
    public void test_release_yml_uses_v4_actions() {
        assertTrue("release.yml must use actions/checkout@v4",
                releaseYml.contains("actions/checkout@v4"));
        assertTrue("release.yml must use actions/setup-java@v4",
                releaseYml.contains("actions/setup-java@v4"));
    }

    @Test
    public void test_release_yml_uses_temurin() {
        assertTrue("release.yml must use temurin distribution",
                releaseYml.contains("temurin"));
    }

    @Test
    public void test_release_yml_deploy_skips_tests_but_test_steps_exist() {
        // Deploy step should skip tests since dedicated test steps run earlier
        assertTrue("release.yml 'Publish package' step should use -DskipTests to avoid redundant test run",
                releaseYml.contains("-DskipTests"));
        // But dedicated test steps must still exist
        assertTrue("release.yml must have 'Run unit tests' step",
                releaseYml.contains("Run unit tests"));
        assertTrue("release.yml must have 'Run integration tests' step",
                releaseYml.contains("Run integration tests"));
    }

    @Test
    public void test_release_yml_runs_unit_tests_before_deploy() {
        assertTrue("release.yml must have 'Run unit tests' step",
                releaseYml.contains("Run unit tests"));
        // The test step must appear before "Version bump" in the file
        int unitTestIdx = releaseYml.indexOf("Run unit tests");
        int versionBumpIdx = releaseYml.indexOf("Version bump");
        assertTrue("release.yml unit tests must run before Version bump",
                unitTestIdx >= 0 && versionBumpIdx >= 0 && unitTestIdx < versionBumpIdx);
    }

    @Test
    public void test_release_yml_runs_integration_tests_before_deploy() {
        assertTrue("release.yml must have 'Run integration tests' step",
                releaseYml.contains("Run integration tests"));
        int integrationTestIdx = releaseYml.indexOf("Run integration tests");
        int versionBumpIdx = releaseYml.indexOf("Version bump");
        assertTrue("release.yml integration tests must run before Version bump",
                integrationTestIdx >= 0 && versionBumpIdx >= 0 && integrationTestIdx < versionBumpIdx);
    }

    @Test
    public void test_release_yml_triggers_on_release_creation() {
        assertTrue("release.yml must trigger on release creation",
                releaseYml.contains("types: [created]"));
    }
}
