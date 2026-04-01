package tech.amikos.chromadb.embeddings.voyage;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.v2.ChromaException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.amikos.chromadb.Constants.JSON;

/**
 * Embedding function that uses the Voyage AI API to generate embeddings.
 *
 * <p>Uses OkHttp (already a compile dependency) to call the Voyage REST API.
 * No additional Maven dependencies are required.</p>
 */
public class VoyageEmbeddingFunction implements EmbeddingFunction {

    static String DEFAULT_BASE_API = "https://api.voyageai.com/v1/embeddings";
    public static final String DEFAULT_MODEL_NAME = "voyage-3.5";
    public static final String VOYAGE_API_KEY_ENV = "VOYAGE_API_KEY";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<String, Object>();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a VoyageEmbeddingFunction using the VOYAGE_API_KEY environment variable.
     *
     * @throws EFException if the environment variable is not set
     */
    public VoyageEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(VOYAGE_API_KEY_ENV).apply(this.configParams);
    }

    /**
     * Creates a VoyageEmbeddingFunction with the given parameters.
     *
     * @param params configuration parameters (apiKey, model, baseAPI, etc.)
     * @throws EFException if parameter application fails
     */
    public VoyageEmbeddingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    private CreateEmbeddingResponse callApi(CreateEmbeddingRequest req) throws EFException {
        String baseApi = configParams.get(Constants.EF_PARAMS_BASE_API).toString();
        Object apiKey = configParams.get(Constants.EF_PARAMS_API_KEY);

        Request request = new Request.Builder()
                .url(baseApi)
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + (apiKey != null ? apiKey.toString() : ""))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ChromaException(
                        "Voyage embedding failed (model: " + configParams.get(Constants.EF_PARAMS_MODEL) + "): "
                                + response.code() + " " + response.message()
                );
            }
            String responseData = response.body().string();
            return gson.fromJson(responseData, CreateEmbeddingResponse.class);
        } catch (ChromaException e) {
            throw e;
        } catch (IOException e) {
            throw new EFException("Voyage embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        if (query == null) {
            throw new ChromaException(
                    "Voyage embedding failed (model: " + configParams.get(Constants.EF_PARAMS_MODEL) + "): query must not be null");
        }
        CreateEmbeddingRequest req = new CreateEmbeddingRequest()
                .model(configParams.get(Constants.EF_PARAMS_MODEL).toString())
                .input(Collections.singletonList(query))
                .inputType("query");
        CreateEmbeddingResponse response = callApi(req);
        return response.toEmbeddings().get(0);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        if (documents == null) {
            throw new ChromaException(
                    "Voyage embedding failed (model: " + configParams.get(Constants.EF_PARAMS_MODEL) + "): documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException(
                    "Voyage embedding failed (model: " + configParams.get(Constants.EF_PARAMS_MODEL) + "): documents must not be empty");
        }
        String modelName = configParams.get(Constants.EF_PARAMS_MODEL).toString();
        CreateEmbeddingRequest req = new CreateEmbeddingRequest()
                .model(modelName)
                .input(documents)
                .inputType("document");
        CreateEmbeddingResponse response = callApi(req);
        List<Embedding> result = response.toEmbeddings();
        if (result.size() != documents.size()) {
            throw new ChromaException(
                    "Voyage embedding failed (model: " + modelName + "): "
                            + "expected " + documents.size() + " embeddings, got " + result.size()
            );
        }
        return result;
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }
}
