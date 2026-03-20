package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Nyquist validation tests for Phase 4: Compatibility and Test Matrix.
 *
 * These tests verify the behavioral requirements of QLTY-01 and QLTY-02
 * by inspecting actual project artifacts (pom.xml, Makefile, CI workflow)
 * and Java class structures.
 */
public class Phase04ValidationTest {

    // === QLTY-01: Version Matrix Infrastructure ===

    @Test
    public void testMakefileHasChromaMatrixVersionsWithThreePinnedVersions() throws IOException {
        String makefile = readProjectFile("Makefile");
        assertTrue(
            "Makefile must define CHROMA_MATRIX_VERSIONS with 1.0.0 1.3.7 1.5.5",
            makefile.contains("CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5")
        );
    }

    @Test
    public void testMakefileTestMatrixTargetUsesFailFastLoop() throws IOException {
        String makefile = readProjectFile("Makefile");
        assertTrue(
            "Makefile test-matrix must use set -e for fail-fast loop",
            makefile.contains("set -e; for v in $(CHROMA_MATRIX_VERSIONS)")
        );
        assertTrue(
            "Makefile test-matrix must propagate exit codes",
            makefile.contains("|| exit $$?")
        );
    }

    @Test
    public void testMakefileTestMatrixTargetPassesChromaVersionToMaven() throws IOException {
        String makefile = readProjectFile("Makefile");
        assertTrue(
            "Makefile test-matrix must pass CHROMA_VERSION env to Maven integration test",
            makefile.contains("CHROMA_VERSION=$$v $(MAVEN) --batch-mode -Pintegration test")
        );
    }

    @Test
    public void testCiWorkflowHasThreeJobs() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        assertTrue("CI workflow must have unit-tests job", workflow.contains("unit-tests:"));
        assertTrue("CI workflow must have integration-tests job", workflow.contains("integration-tests:"));
        assertTrue("CI workflow must have v2-integration-test job", workflow.contains("v2-integration-test:"));
    }

    @Test
    public void testCiWorkflowIntegrationMatrixHasThreeChromaVersions() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        assertTrue(
            "CI workflow must define chroma-version matrix with 1.0.0, 1.3.7, 1.5.5",
            workflow.contains("chroma-version: ['1.0.0', '1.3.7', '1.5.5']")
        );
    }

    @Test
    public void testCiWorkflowHasNoFailFast() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        assertTrue(
            "CI workflow must use fail-fast: false so all cells complete",
            workflow.contains("fail-fast: false")
        );
        assertFalse(
            "CI workflow must not use continue-on-error (all cells required)",
            workflow.contains("continue-on-error")
        );
    }

    @Test
    public void testCiWorkflowUsesTemurinDistribution() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        assertTrue(
            "CI workflow must use temurin distribution (not adopt)",
            workflow.contains("distribution: 'temurin'")
        );
        assertFalse(
            "CI workflow must not use deprecated adopt distribution",
            workflow.contains("distribution: 'adopt'")
        );
    }

    @Test
    public void testCiWorkflowUsesV4Actions() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        assertTrue(
            "CI workflow must use actions/checkout@v4",
            workflow.contains("actions/checkout@v4")
        );
        assertTrue(
            "CI workflow must use actions/setup-java@v4",
            workflow.contains("actions/setup-java@v4")
        );
        assertFalse(
            "CI workflow must not use deprecated v3 checkout",
            workflow.contains("actions/checkout@v3")
        );
        assertFalse(
            "CI workflow must not use deprecated v3 setup-java",
            workflow.contains("actions/setup-java@v3")
        );
    }

    @Test
    public void testCiWorkflowIntegrationMatrixIncludesJdk11And17ForLatest() throws IOException {
        String workflow = readProjectFile(".github/workflows/integration-test.yml");
        // JDK 11 and 17 are added via include with chroma-version 1.5.5
        assertTrue(
            "CI workflow must include JDK 11 with chroma-version 1.5.5",
            workflow.contains("java: '11'")
        );
        assertTrue(
            "CI workflow must include JDK 17 with chroma-version 1.5.5",
            workflow.contains("java: '17'")
        );
    }

    @Test
    public void testContainerStartupFailureThrowsAssertionErrorNotAssume() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java"
        );
        assertTrue(
            "Container startup failure must throw AssertionError for fail-fast behavior",
            source.contains("throw new AssertionError(")
        );
        // Must NOT use Assume.assumeTrue for startup failure
        assertFalse(
            "Container startup failure must NOT use Assume.assumeTrue (would silently skip)",
            source.contains("Assume.assumeTrue") && source.contains("CHROMA_STARTUP_FAILURE")
                && source.contains("Skipping integration tests")
        );
    }

    @Test
    public void testAssumeMinVersionHelperExists() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java"
        );
        assertTrue(
            "AbstractChromaIntegrationTest must have assumeMinVersion() method",
            source.contains("protected static void assumeMinVersion(String minVersion)")
        );
        assertTrue(
            "assumeMinVersion must use Assume.assumeTrue for skip (not fail)",
            source.contains("Assume.assumeTrue(") && source.contains("compareVersions(configuredChromaVersion(), minVersion) >= 0")
        );
    }

    // === QLTY-02: Compatibility Guardrails ===

    @Test
    public void testAnimalSnifferPluginConfiguredInPom() throws IOException {
        String pom = readProjectFile("pom.xml");
        assertTrue(
            "pom.xml must contain animal-sniffer-maven-plugin",
            pom.contains("animal-sniffer-maven-plugin")
        );
        assertTrue(
            "pom.xml must use java18 signature artifact for Java 1.8 API checking",
            pom.contains("<artifactId>java18</artifactId>")
        );
        assertTrue(
            "pom.xml must have check-java-1.8-compat execution ID",
            pom.contains("<id>check-java-1.8-compat</id>")
        );
    }

    @Test
    public void testAnimalSnifferBoundToCompilePhase() throws IOException {
        String pom = readProjectFile("pom.xml");
        // After the gap fix, animal-sniffer is explicitly bound to compile phase
        assertTrue(
            "pom.xml animal-sniffer execution must be bound to compile phase",
            pom.contains("<phase>compile</phase>")
        );
    }

    @Test
    public void testPublicInterfaceCompatTestCoversAllTenTypes() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java"
        );
        // All 10 expected method count constants must be present
        String[] expectedConstants = {
            "EXPECTED_CLIENT_METHOD_COUNT",
            "EXPECTED_COLLECTION_METHOD_COUNT",
            "EXPECTED_ADD_BUILDER_METHOD_COUNT",
            "EXPECTED_QUERY_BUILDER_METHOD_COUNT",
            "EXPECTED_GET_BUILDER_METHOD_COUNT",
            "EXPECTED_UPDATE_BUILDER_METHOD_COUNT",
            "EXPECTED_UPSERT_BUILDER_METHOD_COUNT",
            "EXPECTED_DELETE_BUILDER_METHOD_COUNT",
            "EXPECTED_BUILDER_METHOD_COUNT",
            "EXPECTED_CLOUD_BUILDER_METHOD_COUNT"
        };
        for (String constant : expectedConstants) {
            assertTrue(
                "PublicInterfaceCompatibilityTest must define " + constant,
                source.contains(constant)
            );
        }
    }

    @Test
    public void testPublicInterfaceCompatTestUsesGetDeclaredMethods() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java"
        );
        assertTrue(
            "PublicInterfaceCompatibilityTest must use getDeclaredMethods for count assertions",
            source.contains("getDeclaredMethods()")
        );
    }

    @Test
    public void testPublicInterfaceCompatTestPreservesOriginalTenTests() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/PublicInterfaceCompatibilityTest.java"
        );
        // All 10 original test method names must exist
        String[] originalTests = {
            "testClientGetCollectionWithEmbeddingFunctionIsDefaultMethod",
            "testCollectionGetSchemaIsDefaultMethod",
            "testAddBuilderHasIdGeneratorMethod",
            "testUpsertBuilderHasIdGeneratorMethod",
            "testChromaClientBuilderHasSslCertMethod",
            "testChromaClientBuilderHasInsecureMethod",
            "testChromaClientBuilderHasHttpClientMethod",
            "testChromaClientBuilderHasLoggerMethod",
            "testChromaClientBuilderHasEnvMethods",
            "testCloudBuilderHasLoggerMethod"
        };
        for (String testName : originalTests) {
            assertTrue(
                "PublicInterfaceCompatibilityTest must preserve original test: " + testName,
                source.contains(testName)
            );
        }
    }

    @Test
    public void testClientInterfaceHasExpectedPublicMethods() {
        // Verify key Client interface methods exist with correct signatures
        Class<?> clientClass = Client.class;
        assertTrue("Client must be an interface", clientClass.isInterface());

        assertMethodExists(clientClass, "heartbeat");
        assertMethodExists(clientClass, "version");
        assertMethodExists(clientClass, "reset");
        assertMethodExists(clientClass, "createCollection", String.class);
        assertMethodExists(clientClass, "getCollection", String.class);
        assertMethodExists(clientClass, "deleteCollection", String.class);
        assertMethodExists(clientClass, "listCollections");
        assertMethodExists(clientClass, "countCollections");
        assertMethodExists(clientClass, "close");
    }

    @Test
    public void testCollectionInterfaceHasExpectedOperationBuilders() {
        // Verify that Collection returns proper builder types for operations
        Class<?> collectionClass = Collection.class;
        assertTrue("Collection must be an interface", collectionClass.isInterface());

        try {
            assertEquals(Collection.AddBuilder.class,
                collectionClass.getMethod("add").getReturnType());
            assertEquals(Collection.QueryBuilder.class,
                collectionClass.getMethod("query").getReturnType());
            assertEquals(Collection.GetBuilder.class,
                collectionClass.getMethod("get").getReturnType());
            assertEquals(Collection.UpdateBuilder.class,
                collectionClass.getMethod("update").getReturnType());
            assertEquals(Collection.UpsertBuilder.class,
                collectionClass.getMethod("upsert").getReturnType());
            assertEquals(Collection.DeleteBuilder.class,
                collectionClass.getMethod("delete").getReturnType());
        } catch (NoSuchMethodException e) {
            fail("Collection interface is missing an expected operation builder method: " + e.getMessage());
        }
    }

    @Test
    public void testDefaultChromaVersionIs155() throws IOException {
        String source = readProjectFile(
            "src/test/java/tech/amikos/chromadb/v2/AbstractChromaIntegrationTest.java"
        );
        assertTrue(
            "DEFAULT_CHROMA_VERSION must be 1.5.5 (latest pinned)",
            source.contains("DEFAULT_CHROMA_VERSION = \"1.5.5\"")
        );
    }

    // === Helpers ===

    private static String readProjectFile(String relativePath) throws IOException {
        // Discover project root from the current classloader resource path
        String projectRoot = System.getProperty("user.dir");
        File file = new File(projectRoot, relativePath);
        assertTrue("File must exist: " + file.getAbsolutePath(), file.exists());

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private static void assertMethodExists(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            Method m = clazz.getMethod(name, paramTypes);
            assertNotNull("Method " + name + " should exist on " + clazz.getSimpleName(), m);
        } catch (NoSuchMethodException e) {
            fail(clazz.getSimpleName() + " is missing expected public method: " + name);
        }
    }
}
