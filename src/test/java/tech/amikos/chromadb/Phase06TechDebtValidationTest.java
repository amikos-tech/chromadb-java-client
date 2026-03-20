package tech.amikos.chromadb;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 06 Nyquist validation tests — Tech Debt Cleanup.
 *
 * Validates the six audit items addressed in Phase 6:
 * - DOC-BUG-1: README v2 HuggingFace example uses WithParam constructor
 * - DOC-BUG-2: README Sha256IdGenerator description mentions metadata fallback
 * - ASSUME-WIRE: assumeMinVersion() is exercised by at least one integration test
 * - INFRA-1: release.yml has no branches filter
 * - INFRA-2: release.yml has release-check step before Publish package
 * - ND4J-BUMP: nd4j-native-platform version is 1.0.0-M2 (M2.1 requires Java 11)
 *
 * These tests read project root files and integration test sources
 * without modifying any implementation.
 */
public class Phase06TechDebtValidationTest {

    private static String readme;
    private static String releaseYml;
    private static String pomXml;
    private static Path projectRoot;

    @BeforeClass
    public static void loadFiles() throws IOException {
        projectRoot = ProjectFileTestHelper.findProjectRoot();

        readme = ProjectFileTestHelper.readFile(projectRoot.resolve("README.md"));
        releaseYml = ProjectFileTestHelper.readFile(projectRoot.resolve(".github/workflows/release.yml"));
        pomXml = ProjectFileTestHelper.readFile(projectRoot.resolve("pom.xml"));
    }

    // -----------------------------------------------------------------------
    // DOC-BUG-1: README v2 HuggingFace example must use WithParam constructor
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v2_hf_example_uses_withparam() {
        assertTrue(
            "README v2 HuggingFace example must use new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey))",
            readme.contains("new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey))")
        );
        assertFalse(
            "README must NOT contain bare-String constructor new HuggingFaceEmbeddingFunction(apiKey); anywhere (including v1 section)",
            readme.contains("new HuggingFaceEmbeddingFunction(apiKey);")
        );
    }

    // -----------------------------------------------------------------------
    // DOC-BUG-2: README Sha256IdGenerator description must mention metadata fallback
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_sha256_description_accurate() {
        assertTrue(
            "README Sha256IdGenerator description must say 'requires a non-null document or non-null metadata'",
            readme.contains("requires a non-null document or non-null metadata")
        );
        assertFalse(
            "README must NOT contain stale singular-only description 'requires non-null documents.'",
            readme.contains("requires non-null documents.")
        );
    }

    // -----------------------------------------------------------------------
    // ASSUME-WIRE: At least one integration test must call assumeMinVersion()
    // -----------------------------------------------------------------------

    @Test
    public void test_assume_min_version_has_callers() throws IOException {
        Path integrationTestDir = projectRoot.resolve(
            "src/test/java/tech/amikos/chromadb/v2"
        );
        assertTrue(
            "Integration test directory must exist: " + integrationTestDir,
            Files.exists(integrationTestDir)
        );

        List<Path> integrationTests = new ArrayList<Path>();
        for (Path p : Files.newDirectoryStream(integrationTestDir, "*IntegrationTest.java")) {
            String filename = p.getFileName().toString();
            if (!filename.equals("AbstractChromaIntegrationTest.java")) {
                integrationTests.add(p);
            }
        }

        boolean foundCaller = false;
        for (Path testFile : integrationTests) {
            String source = ProjectFileTestHelper.readFile(testFile);
            if (source.contains("assumeMinVersion(")) {
                foundCaller = true;
                break;
            }
        }

        assertTrue(
            "At least one integration test (excluding Abstract) must call assumeMinVersion()",
            foundCaller
        );
    }

    // -----------------------------------------------------------------------
    // INFRA-1: release.yml must not have a branches filter
    // -----------------------------------------------------------------------

    @Test
    public void test_release_yml_no_branches_filter() {
        assertFalse(
            "release.yml must NOT contain a 'branches:' filter (it narrows the release trigger incorrectly)",
            releaseYml.contains("branches:")
        );
        assertTrue(
            "release.yml must still have 'types: [created]' trigger type",
            releaseYml.contains("types: [created]")
        );
    }

    // -----------------------------------------------------------------------
    // INFRA-2: release.yml must have release-check step before Publish package
    // -----------------------------------------------------------------------

    @Test
    public void test_release_yml_has_release_check_step() {
        assertTrue(
            "release.yml must contain a 'make release-check' step",
            releaseYml.contains("make release-check")
        );
        int releaseCheckIdx = releaseYml.indexOf("make release-check");
        int publishIdx = releaseYml.indexOf("Publish package");
        assertTrue(
            "release.yml 'make release-check' step must appear before 'Publish package'",
            releaseCheckIdx >= 0 && publishIdx >= 0 && releaseCheckIdx < publishIdx
        );
    }

    // -----------------------------------------------------------------------
    // ND4J-BUMP: nd4j-native-platform version must be 1.0.0-M2 (Java 8 compat)
    // -----------------------------------------------------------------------

    @Test
    public void test_nd4j_version_is_m2() {
        assertTrue(
            "pom.xml must contain nd4j-native-platform dependency",
            pomXml.contains("nd4j-native-platform")
        );
        assertTrue(
            "pom.xml nd4j-native-platform version must be 1.0.0-M2",
            pomXml.contains("1.0.0-M2")
        );
        assertFalse(
            "pom.xml nd4j-native-platform must NOT be M2.1 (requires Java 11, breaks Java 8 compat)",
            pomXml.contains("1.0.0-M2.1")
        );
    }
}
