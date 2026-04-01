package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.bedrock.BedrockEmbeddingFunction;

import java.lang.reflect.Field;

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
}
