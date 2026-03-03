package tech.amikos.chromadb.v2;

import org.junit.Test;
import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EmbeddingFunctionResolverTest {

    @Test
    public void testResolveNullSpecReturnsNull() {
        assertNull(EmbeddingFunctionResolver.resolve(null));
    }

    @Test
    public void testResolveHuggingFaceAlias() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("api_type", "HF_API");
        config.put("api_key", "dummy");

        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("hf")
                .config(config)
                .build();

        EmbeddingFunction ef = EmbeddingFunctionResolver.resolve(spec);
        assertNotNull(ef);
        assertTrue(ef instanceof HuggingFaceEmbeddingFunction);
    }

    @Test
    public void testResolveOllamaConfigAliases() {
        Map<String, Object> cfg = new LinkedHashMap<String, Object>();
        cfg.put("base_api", "http://localhost:11434/api/embed");
        cfg.put("model_name", "nomic-embed-text");

        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("ollama")
                .config(cfg)
                .build();

        EmbeddingFunction ef = EmbeddingFunctionResolver.resolve(spec);
        assertNotNull(ef);
        assertTrue(ef instanceof OllamaEmbeddingFunction);
    }

    @Test
    public void testResolveWrapsHuggingFaceApiTypeValidationAsChromaException() {
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("huggingface")
                .config(Collections.<String, Object>singletonMap("api_type", "INVALID"))
                .build();

        try {
            EmbeddingFunctionResolver.resolve(spec);
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("huggingface"));
            assertNotNull(e.getCause());
            return;
        }
        throw new AssertionError("Expected ChromaException");
    }

    @Test
    public void testResolveRejectsNonStringConfigValue() {
        Map<String, Object> cfg = new LinkedHashMap<String, Object>();
        cfg.put("api_key", Integer.valueOf(12345));
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("openai")
                .config(cfg)
                .build();

        try {
            EmbeddingFunctionResolver.resolve(spec);
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("openai"));
            assertTrue(e.getMessage().contains("api_key must be a string"));
            return;
        }
        throw new AssertionError("Expected ChromaException");
    }

    @Test
    public void testResolveUnknownProviderFailsFastWithGuidance() {
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("consistent_hash")
                .config(Collections.<String, Object>emptyMap())
                .build();
        try {
            EmbeddingFunctionResolver.resolve(spec);
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("consistent_hash"));
            assertTrue(e.getMessage().contains("queryEmbeddings"));
            return;
        }
        throw new AssertionError("Expected ChromaException");
    }

    @Test
    public void testResolveRejectsUnsupportedType() {
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("custom")
                .name("openai")
                .config(Collections.<String, Object>emptyMap())
                .build();
        try {
            EmbeddingFunctionResolver.resolve(spec);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Unsupported embedding function type 'custom'"));
            assertTrue(e.getMessage().contains("queryEmbeddings"));
        }
    }

    @Test
    public void testResolveOpenAIApiKeyEnvVarMapsToApiKeySlot() throws Exception {
        Map<String, Object> cfg = new LinkedHashMap<String, Object>();
        cfg.put("api_key_env_var", "PATH");
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("openai")
                .config(cfg)
                .build();

        EmbeddingFunction ef = EmbeddingFunctionResolver.resolve(spec);
        assertNotNull(ef);
        assertTrue(ef instanceof OpenAIEmbeddingFunction);

        java.lang.reflect.Field configField = OpenAIEmbeddingFunction.class.getDeclaredField("configParams");
        configField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> configParams = (Map<String, Object>) configField.get(ef);

        assertTrue(configParams.containsKey(Constants.EF_PARAMS_API_KEY));
        assertNotNull(configParams.get(Constants.EF_PARAMS_API_KEY));
    }
}
