package tech.amikos.chromadb.reranking.cohere;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.reranking.RerankResult;
import tech.amikos.chromadb.reranking.RerankingFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.amikos.chromadb.Constants.JSON;

/**
 * Reranking function using the Cohere v2 rerank API.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RerankingFunction reranker = new CohereRerankingFunction(WithParam.apiKey("your-key"));
 * List<RerankResult> results = reranker.rerank("query", documents);
 * }</pre>
 */
public class CohereRerankingFunction implements RerankingFunction {

    static String DEFAULT_BASE_API = "https://api.cohere.com/v2/rerank";
    public static final String DEFAULT_MODEL_NAME = "rerank-v3.5";
    public static final String COHERE_API_KEY_ENV = "COHERE_API_KEY";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<String, Object>();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a Cohere reranking function with the given parameters.
     *
     * @param params configuration parameters (at minimum, WithParam.apiKey)
     * @throws EFException if required parameters are missing
     */
    public CohereRerankingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> documents) throws EFException {
        String model = modelName();
        validateInputs(query, documents, model);
        String baseApi = configParams.containsKey(Constants.EF_PARAMS_BASE_API)
                ? configParams.get(Constants.EF_PARAMS_BASE_API).toString()
                : DEFAULT_BASE_API;
        RerankRequest rerankRequest = new RerankRequest(model, query, documents);

        Request request = new Request.Builder()
                .url(baseApi)
                .post(RequestBody.create(rerankRequest.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + requireApiKey(model))
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful()) {
                String body = responseBody != null ? responseBody.string() : "";
                throw new EFException("Cohere rerank failed: HTTP " + response.code() + " - " + body);
            }

            if (responseBody == null) {
                throw new EFException("Cohere rerank failed (model: " + model + "): response body was empty");
            }

            String responseData = responseBody.string();
            if (responseData.trim().isEmpty()) {
                throw new EFException("Cohere rerank failed (model: " + model + "): response body was empty");
            }
            RerankResponse rerankResponse = gson.fromJson(responseData, RerankResponse.class);
            if (rerankResponse == null || rerankResponse.results == null) {
                throw new EFException("Cohere rerank failed (model: " + model + "): response did not contain results");
            }

            List<RerankResult> results = new ArrayList<RerankResult>();
            for (RerankResponse.Result r : rerankResponse.results) {
                results.add(RerankResult.of(r.index, r.relevance_score));
            }

            Collections.sort(results, new Comparator<RerankResult>() {
                @Override
                public int compare(RerankResult a, RerankResult b) {
                    return Double.compare(b.getScore(), a.getScore());
                }
            });

            return results;
        } catch (EFException e) {
            throw e;
        } catch (IOException e) {
            throw new EFException("Cohere rerank failed: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new EFException("Cohere rerank failed: " + e.getMessage(), e);
        }
    }

    private String modelName() {
        Object model = configParams.get(Constants.EF_PARAMS_MODEL);
        return model != null ? model.toString() : DEFAULT_MODEL_NAME;
    }

    private String requireApiKey(String model) throws EFException {
        Object apiKey = configParams.get(Constants.EF_PARAMS_API_KEY);
        String normalized = apiKey == null ? null : apiKey.toString().trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new EFException("Cohere rerank failed (model: " + model + "): API key must not be null or empty");
        }
        return normalized;
    }

    private void validateInputs(String query, List<String> documents, String model) throws EFException {
        if (query == null) {
            throw new EFException("Cohere rerank failed (model: " + model + "): query must not be null");
        }
        if (documents == null) {
            throw new EFException("Cohere rerank failed (model: " + model + "): documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new EFException("Cohere rerank failed (model: " + model + "): documents must not be empty");
        }
    }
}
