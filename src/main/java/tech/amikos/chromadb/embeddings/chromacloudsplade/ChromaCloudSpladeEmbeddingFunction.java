package tech.amikos.chromadb.embeddings.chromacloudsplade;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.SparseEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.SparseVector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.amikos.chromadb.Constants.JSON;

/**
 * Sparse embedding function that calls the Chroma Cloud Splade API.
 *
 * <p>Implements {@link SparseEmbeddingFunction} and produces {@link SparseVector}
 * representations via the remote Chroma Cloud sparse embedding endpoint.</p>
 */
public class ChromaCloudSpladeEmbeddingFunction implements SparseEmbeddingFunction {

    static String DEFAULT_BASE_API = "https://api.trychroma.com/api/v2/embed/splade";
    public static final String DEFAULT_MODEL_NAME = "splade";
    public static final String CHROMA_API_KEY_ENV = "CHROMA_API_KEY";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<String, Object>();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a ChromaCloudSpladeEmbeddingFunction using the CHROMA_API_KEY environment variable.
     *
     * @throws EFException if the environment variable is not set
     */
    public ChromaCloudSpladeEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(CHROMA_API_KEY_ENV).apply(this.configParams);
    }

    /**
     * Creates a ChromaCloudSpladeEmbeddingFunction with the given parameters.
     *
     * @param params configuration parameters (apiKey, model, baseAPI, etc.)
     * @throws EFException if parameter application fails
     */
    public ChromaCloudSpladeEmbeddingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    private CreateSparseEmbeddingResponse callApi(CreateSparseEmbeddingRequest req) throws EFException {
        String baseApi = configParams.get(Constants.EF_PARAMS_BASE_API).toString();
        String modelName = modelName();
        String apiKey = requireApiKey(modelName);

        Request request = new Request.Builder()
                .url(baseApi)
                .post(RequestBody.create(req.toJson(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful()) {
                String body = responseBody != null ? responseBody.string() : "";
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed (model: " + modelName + "): "
                                + response.code() + " " + response.message()
                                + (body.isEmpty() ? "" : " - " + body)
                );
            }
            if (responseBody == null) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed (model: " + modelName + "): response body was empty"
                );
            }
            String responseData = responseBody.string();
            if (responseData.trim().isEmpty()) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed (model: " + modelName + "): response body was empty"
                );
            }
            CreateSparseEmbeddingResponse parsed = gson.fromJson(responseData, CreateSparseEmbeddingResponse.class);
            if (parsed == null) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed (model: " + modelName + "): response could not be parsed"
                );
            }
            return parsed;
        } catch (ChromaException e) {
            throw e;
        } catch (IOException e) {
            throw new EFException("Chroma Cloud Splade embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SparseVector embedQuery(String query) throws EFException {
        if (query == null) {
            throw new ChromaException(
                    "Chroma Cloud Splade embedding failed (model: "
                            + configParams.get(Constants.EF_PARAMS_MODEL) + "): query must not be null");
        }
        CreateSparseEmbeddingRequest req = new CreateSparseEmbeddingRequest()
                .model(configParams.get(Constants.EF_PARAMS_MODEL).toString())
                .texts(Collections.singletonList(query));
        CreateSparseEmbeddingResponse response = callApi(req);
        List<SparseVector> vectors = response.toSparseVectors();
        if (vectors.isEmpty()) {
            throw new ChromaException("Chroma Cloud Splade returned no results");
        }
        return vectors.get(0);
    }

    @Override
    public List<SparseVector> embedDocuments(List<String> documents) throws EFException {
        if (documents == null) {
            throw new ChromaException(
                    "Chroma Cloud Splade embedding failed (model: "
                            + configParams.get(Constants.EF_PARAMS_MODEL) + "): documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException(
                    "Chroma Cloud Splade embedding failed (model: "
                            + configParams.get(Constants.EF_PARAMS_MODEL) + "): documents must not be empty");
        }
        CreateSparseEmbeddingRequest req = new CreateSparseEmbeddingRequest()
                .model(configParams.get(Constants.EF_PARAMS_MODEL).toString())
                .texts(documents);
        CreateSparseEmbeddingResponse response = callApi(req);
        List<SparseVector> result = response.toSparseVectors();
        if (result.size() != documents.size()) {
            throw new ChromaException(
                    "Chroma Cloud Splade embedding failed (model: "
                            + configParams.get(Constants.EF_PARAMS_MODEL) + "): "
                            + "expected " + documents.size() + " embeddings, got " + result.size()
            );
        }
        return result;
    }

    private String modelName() {
        Object model = configParams.get(Constants.EF_PARAMS_MODEL);
        return model != null ? model.toString() : DEFAULT_MODEL_NAME;
    }

    private String requireApiKey(String modelName) {
        Object apiKey = configParams.get(Constants.EF_PARAMS_API_KEY);
        String normalized = apiKey == null ? null : apiKey.toString().trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new ChromaException(
                    "Chroma Cloud Splade embedding failed (model: " + modelName + "): API key must not be null or empty");
        }
        return normalized;
    }
}
