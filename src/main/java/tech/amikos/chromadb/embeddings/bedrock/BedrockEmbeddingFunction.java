package tech.amikos.chromadb.embeddings.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.v2.ChromaException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding function that uses the AWS Bedrock Runtime SDK to generate embeddings.
 *
 * <p>Requires the {@code software.amazon.awssdk:bedrockruntime} dependency on the classpath.
 * The dependency is declared as optional in the POM; users must add it explicitly.</p>
 *
 * <p>Authentication uses the AWS default credential chain (environment variables, IAM role, etc.).
 * No API key is needed at construction time.</p>
 */
public class BedrockEmbeddingFunction implements EmbeddingFunction {

    public static final String DEFAULT_MODEL_NAME = "amazon.titan-embed-text-v2:0";
    public static final String AWS_REGION_ENV = "AWS_REGION";
    static final String CONFIG_KEY_REGION = "awsRegion";
    private static final String DEFAULT_REGION = "us-east-1";

    private final Map<String, Object> configParams = new HashMap<String, Object>();
    private volatile BedrockRuntimeClient bedrockClient;
    private final Gson gson = new Gson();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a custom WithParam that sets the AWS region.
     *
     * @param region AWS region string (e.g. "us-east-1", "eu-west-1")
     * @return a WithParam that configures the region
     */
    public static WithParam region(String region) {
        return new WithRegion(region);
    }

    /**
     * Creates a BedrockEmbeddingFunction with default settings.
     * Uses the default model and us-east-1 region.
     *
     * @throws EFException if parameter application fails
     */
    public BedrockEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
    }

    /**
     * Creates a BedrockEmbeddingFunction with the given parameters.
     *
     * @param params configuration parameters (model, region, etc.)
     * @throws EFException if parameter application fails
     */
    public BedrockEmbeddingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    private BedrockRuntimeClient getClient() {
        if (bedrockClient == null) {
            synchronized (this) {
                if (bedrockClient == null) {
                    String regionStr = configParams.containsKey(CONFIG_KEY_REGION)
                            ? configParams.get(CONFIG_KEY_REGION).toString()
                            : System.getenv(AWS_REGION_ENV) != null
                            ? System.getenv(AWS_REGION_ENV)
                            : DEFAULT_REGION;
                    bedrockClient = BedrockRuntimeClient.builder()
                            .region(Region.of(regionStr))
                            .build();
                }
            }
        }
        return bedrockClient;
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        String modelName = modelName();
        if (query == null) {
            throw new ChromaException(
                    "Bedrock embedding failed (model: " + modelName + "): query must not be null");
        }
        return embedDocuments(Collections.singletonList(query)).get(0);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        String modelName = modelName();
        if (documents == null) {
            throw new ChromaException(
                    "Bedrock embedding failed (model: " + modelName + "): documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException(
                    "Bedrock embedding failed (model: " + modelName + "): documents must not be empty");
        }
        for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
            if (documents.get(docIndex) == null) {
                throw new ChromaException(
                        "Bedrock embedding failed (model: " + modelName
                                + "): document at index " + docIndex + " must not be null");
            }
        }
        BedrockRuntimeClient client = getClient();
        try {
            List<Embedding> results = new ArrayList<Embedding>();
            for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
                String doc = documents.get(docIndex);
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("inputText", doc);
                requestBody.addProperty("dimensions", 1024);
                requestBody.addProperty("normalize", true);

                InvokeModelRequest request = InvokeModelRequest.builder()
                        .modelId(modelName)
                        .body(SdkBytes.fromString(requestBody.toString(), StandardCharsets.UTF_8))
                        .contentType("application/json")
                        .accept("application/json")
                        .build();

                InvokeModelResponse response = client.invokeModel(request);
                String responseJson = response.body().asUtf8String();
                JsonObject responseObj = JsonParser.parseString(responseJson).getAsJsonObject();
                JsonArray embeddingArray = responseObj.getAsJsonArray("embedding");

                float[] floatArray = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    floatArray[i] = embeddingArray.get(i).getAsFloat();
                }
                results.add(new Embedding(floatArray));
            }
            if (results.size() != documents.size()) {
                throw new ChromaException(
                        "Bedrock embedding failed (model: " + modelName + "): "
                                + "expected " + documents.size() + " embeddings, got " + results.size()
                );
            }
            return results;
        } catch (ChromaException e) {
            throw e;
        } catch (Exception e) {
            throw new EFException("Bedrock embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }

    private String modelName() {
        Object model = configParams.get(Constants.EF_PARAMS_MODEL);
        return model != null ? model.toString() : DEFAULT_MODEL_NAME;
    }

    /**
     * Inner WithParam subclass for configuring the AWS region.
     */
    private static class WithRegion extends WithParam {
        private final String region;

        WithRegion(String region) {
            this.region = region;
        }

        @Override
        public void apply(Map<String, Object> params) {
            params.put(CONFIG_KEY_REGION, region);
        }
    }
}
