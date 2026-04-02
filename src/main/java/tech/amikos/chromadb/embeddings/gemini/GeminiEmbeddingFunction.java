package tech.amikos.chromadb.embeddings.gemini;

import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.v2.ChromaException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding function that uses the Google Gemini GenAI SDK to generate embeddings.
 *
 * <p>Requires the {@code com.google.genai:google-genai} dependency on the classpath.
 * The dependency is declared as optional in the POM; users must add it explicitly.</p>
 */
public class GeminiEmbeddingFunction implements EmbeddingFunction {

    public static final String DEFAULT_MODEL_NAME = "text-embedding-004";
    public static final String GEMINI_API_KEY_ENV = "GEMINI_API_KEY";

    private final Map<String, Object> configParams = new HashMap<String, Object>();
    private volatile Object genaiClient; // lazily initialized com.google.genai.Client

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a GeminiEmbeddingFunction using the GEMINI_API_KEY environment variable.
     *
     * @throws EFException if the environment variable is not set
     */
    public GeminiEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(GEMINI_API_KEY_ENV).apply(this.configParams);
    }

    /**
     * Creates a GeminiEmbeddingFunction with the given parameters.
     *
     * @param params configuration parameters (apiKey, model, etc.)
     * @throws EFException if parameter application fails
     */
    public GeminiEmbeddingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    private Object getClient() throws EFException {
        if (genaiClient == null) {
            synchronized (this) {
                if (genaiClient == null) {
                    Object apiKey = configParams.get(Constants.EF_PARAMS_API_KEY);
                    if (apiKey == null) {
                        throw new EFException("Gemini API key is required. Provide via WithParam.apiKey() or set " + GEMINI_API_KEY_ENV);
                    }
                    try {
                        // Use reflection-free direct SDK call
                        genaiClient = com.google.genai.Client.builder()
                                .apiKey(apiKey.toString())
                                .build();
                    } catch (Exception e) {
                        throw new EFException("Failed to initialize Gemini client: " + e.getMessage(), e);
                    }
                }
            }
        }
        return genaiClient;
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        String modelName = modelName();
        if (query == null) {
            throw new ChromaException(
                    "Gemini embedding failed (model: " + modelName + "): query must not be null");
        }
        return embedDocuments(Collections.singletonList(query)).get(0);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        String modelName = modelName();
        if (documents == null) {
            throw new ChromaException(
                    "Gemini embedding failed (model: " + modelName + "): documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException(
                    "Gemini embedding failed (model: " + modelName + "): documents must not be empty");
        }
        for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
            if (documents.get(docIndex) == null) {
                throw new ChromaException(
                        "Gemini embedding failed (model: " + modelName
                                + "): document at index " + docIndex + " must not be null");
            }
        }
        com.google.genai.Client client = (com.google.genai.Client) getClient();
        try {
            List<Embedding> results = new ArrayList<Embedding>();
            for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
                String doc = documents.get(docIndex);
                com.google.genai.types.EmbedContentResponse response = client.models.embedContent(
                        modelName,
                        doc,
                        null
                );
                results.add(toEmbedding(response, modelName));
            }
            if (results.size() != documents.size()) {
                throw new ChromaException(
                        "Gemini embedding failed (model: " + modelName + "): "
                                + "expected " + documents.size() + " embeddings, got " + results.size()
                );
            }
            return results;
        } catch (ChromaException e) {
            throw e;
        } catch (Exception e) {
            throw new EFException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        if (documents == null) {
            return embedDocuments((List<String>) null);
        }
        return embedDocuments(Arrays.asList(documents));
    }

    private String modelName() {
        Object model = configParams.get(Constants.EF_PARAMS_MODEL);
        return model != null ? model.toString() : DEFAULT_MODEL_NAME;
    }

    private Embedding toEmbedding(com.google.genai.types.EmbedContentResponse response, String modelName) {
        List<com.google.genai.types.ContentEmbedding> embeddings = response.embeddings().orElse(null);
        if (embeddings == null || embeddings.isEmpty()) {
            throw new ChromaException(
                    "Gemini embedding failed (model: " + modelName + "): Gemini returned no embeddings");
        }

        com.google.genai.types.ContentEmbedding contentEmbedding = embeddings.get(0);
        List<Float> values = contentEmbedding.values().orElse(null);
        if (values == null || values.isEmpty()) {
            throw new ChromaException(
                    "Gemini embedding failed (model: " + modelName + "): Gemini embedding has no values");
        }

        float[] floatArray = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            floatArray[i] = values.get(i);
        }
        return new Embedding(floatArray);
    }
}
