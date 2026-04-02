package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.bedrock.BedrockEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for BedrockEmbeddingFunction.
 *
 * <p>These tests verify construction and configuration behavior.
 * Actual API calls require AWS credentials and are not tested here.</p>
 */
public class TestBedrockEmbeddingFunction {

    @Test
    public void testConstructWithDefaults() throws EFException {
        // AWS SDK uses default credential chain, no API key needed at construction
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction();
        assertNotNull(ef);
    }

    @Test
    public void testDefaultModelName() throws Exception {
        Field field = BedrockEmbeddingFunction.class.getDeclaredField("DEFAULT_MODEL_NAME");
        field.setAccessible(true);
        assertEquals("amazon.titan-embed-text-v2:0", field.get(null));
    }

    @Test
    public void testCustomRegion() throws EFException {
        // Construction with custom region should not throw
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction(
                BedrockEmbeddingFunction.region("eu-west-1")
        );
        assertNotNull(ef);
    }

    @Test
    public void testCustomModel() throws EFException {
        // Construction with custom model should not throw
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction(
                WithParam.model("cohere.embed-english-v3")
        );
        assertNotNull(ef);
    }

    @Test
    public void testAwsRegionEnvConstant() {
        assertEquals("AWS_REGION", BedrockEmbeddingFunction.AWS_REGION_ENV);
    }

    @Test
    public void testEmbedQueryRejectsNullWithConfiguredModel() throws EFException {
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction(
                WithParam.model("custom-bedrock-model")
        );

        try {
            ef.embedQuery(null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-bedrock-model"));
            assertTrue(e.getMessage().contains("query must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNullListWithConfiguredModel() throws EFException {
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction(
                WithParam.model("custom-bedrock-model")
        );

        try {
            ef.embedDocuments((List<String>) null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-bedrock-model"));
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNullElementWithIndex() throws EFException {
        BedrockEmbeddingFunction ef = new BedrockEmbeddingFunction(
                WithParam.model("custom-bedrock-model")
        );

        try {
            ef.embedDocuments(Arrays.asList("doc1", null));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-bedrock-model"));
            assertTrue(e.getMessage().contains("document at index 1 must not be null"));
        }
    }
}
