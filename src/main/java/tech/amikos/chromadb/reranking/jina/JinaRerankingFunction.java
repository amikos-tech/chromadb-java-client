package tech.amikos.chromadb.reranking.jina;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
 * Reranking function using the Jina v1 rerank API.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RerankingFunction reranker = new JinaRerankingFunction(WithParam.apiKey("your-key"));
 * List<RerankResult> results = reranker.rerank("query", documents);
 * }</pre>
 */
public class JinaRerankingFunction implements RerankingFunction {

    static String DEFAULT_BASE_API = "https://api.jina.ai/v1/rerank";
    public static final String DEFAULT_MODEL_NAME = "jina-reranker-v2-base-multilingual";
    public static final String JINA_API_KEY_ENV = "JINA_API_KEY";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<String, Object>();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    /**
     * Creates a Jina reranking function with the given parameters.
     *
     * @param params configuration parameters (at minimum, WithParam.apiKey)
     * @throws EFException if required parameters are missing
     */
    public JinaRerankingFunction(WithParam... params) throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> documents) throws EFException {
        String baseApi = configParams.containsKey(Constants.EF_PARAMS_BASE_API)
                ? configParams.get(Constants.EF_PARAMS_BASE_API).toString()
                : DEFAULT_BASE_API;

        String model = configParams.get(Constants.EF_PARAMS_MODEL).toString();
        RerankRequest rerankRequest = new RerankRequest(model, query, documents);

        Request request = new Request.Builder()
                .url(baseApi)
                .post(RequestBody.create(rerankRequest.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + configParams.get(Constants.EF_PARAMS_API_KEY).toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new EFException("Jina rerank failed: HTTP " + response.code() + " - " + body);
            }

            String responseData = response.body().string();
            RerankResponse rerankResponse = gson.fromJson(responseData, RerankResponse.class);

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
            throw new EFException(e);
        }
    }
}
