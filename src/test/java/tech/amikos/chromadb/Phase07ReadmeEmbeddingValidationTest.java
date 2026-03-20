package tech.amikos.chromadb;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Phase 07 Nyquist validation tests — README Embedding Examples.
 *
 * Validates the four broken constructor calls and missing WithParam imports
 * addressed in Phase 7:
 * - EMB-01: v2 OpenAI README example uses WithParam constructor
 * - EMB-01: v2 Cohere README example uses WithParam constructor
 * - EMB-01: v1 appendix OpenAI README example uses WithParam constructor
 * - EMB-01: v1 appendix Cohere README example uses WithParam constructor
 * - QLTY-03: All embedding examples that use WithParam include the import
 *
 * These tests read project root files without modifying any implementation.
 */
public class Phase07ReadmeEmbeddingValidationTest {

    private static String readme;
    private static Path projectRoot;

    @BeforeClass
    public static void loadFiles() throws IOException {
        projectRoot = ProjectFileTestHelper.findProjectRoot();
        readme = ProjectFileTestHelper.readFile(projectRoot.resolve("README.md"));
    }

    // -----------------------------------------------------------------------
    // EMB-01: v2 OpenAI example must use WithParam constructor
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v2_openai_example_uses_withparam() {
        assertTrue(
            "README v2 OpenAI example must use new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"text-embedding-3-small\"))",
            readme.contains("new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"text-embedding-3-small\"))")
        );
        assertFalse(
            "README must NOT contain broken constructor new OpenAIEmbeddingFunction(apiKey, \"text-embedding-3-small\")",
            readme.contains("new OpenAIEmbeddingFunction(apiKey, \"text-embedding-3-small\")")
        );
    }

    // -----------------------------------------------------------------------
    // EMB-01: v2 Cohere example must use WithParam constructor
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v2_cohere_example_uses_withparam() {
        assertTrue(
            "README v2 Cohere example must use new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"embed-english-v2.0\"))",
            readme.contains("new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"embed-english-v2.0\"))")
        );
        assertFalse(
            "README v2 section must NOT contain broken constructor CohereEmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);",
            readme.contains("CohereEmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);")
        );
    }

    // -----------------------------------------------------------------------
    // EMB-01: v1 appendix OpenAI example must use WithParam constructor
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v1_openai_example_uses_withparam() {
        assertTrue(
            "README v1 appendix OpenAI example must use EmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"text-embedding-3-small\"))",
            readme.contains("EmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model(\"text-embedding-3-small\"))")
        );
        assertFalse(
            "README must NOT contain broken v1 constructor EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, \"text-embedding-3-small\")",
            readme.contains("EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, \"text-embedding-3-small\")")
        );
    }

    // -----------------------------------------------------------------------
    // EMB-01: v1 appendix Cohere example must use WithParam constructor
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v1_cohere_example_uses_withparam() {
        assertTrue(
            "README v1 appendix Cohere example must use EmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey))",
            readme.contains("EmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey))")
        );
        assertFalse(
            "README must NOT contain broken v1 constructor EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);",
            readme.contains("EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);")
        );
    }

    // -----------------------------------------------------------------------
    // QLTY-03: v2 embedding examples must include WithParam import (at least 4)
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v2_withparam_examples_have_import() {
        int count = countOccurrences(readme, "import tech.amikos.chromadb.embeddings.WithParam;");
        assertTrue(
            "README must contain at least 4 WithParam imports (v2 OpenAI, v2 Cohere, v2 HF, v2 HFEI) but found: " + count,
            count >= 4
        );
    }

    // -----------------------------------------------------------------------
    // QLTY-03: v1 appendix embedding examples must include WithParam import (at least 8)
    // -----------------------------------------------------------------------

    @Test
    public void test_readme_v1_withparam_examples_have_import() {
        int count = countOccurrences(readme, "import tech.amikos.chromadb.embeddings.WithParam;");
        assertTrue(
            "README must contain at least 8 WithParam imports (4 v2 + 4 v1) but found: " + count,
            count >= 8
        );
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
