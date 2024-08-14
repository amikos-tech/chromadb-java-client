package tech.amikos.chromadb.embeddings.cohere;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import tech.amikos.chromadb.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static tech.amikos.chromadb.Constants.JSON;

public class CohereEmbeddingFunction implements EmbeddingFunction {
    public static final String DEFAULT_MODEL_NAME = "embed-english-v2.0";
    public static final String DEFAULT_BASE_API = "https://api.cohere.ai/v1/";
    public static final String COHERE_API_KEY_ENV = "COHERE_API_KEY";

    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, Object> configParams = new HashMap<>();
    private static final Gson gson = new Gson();


    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),

            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    public CohereEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(COHERE_API_KEY_ENV).apply(this.configParams);
    }

    public CohereEmbeddingFunction(WithParam... params) throws EFException {
        // apply defaults
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }


    public CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) throws EFException {
        Request request = new Request.Builder()
                .url(this.configParams.get(Constants.EF_PARAMS_BASE_API).toString() + "embed")
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Client-Name", Constants.HTTP_AGENT)
                .addHeader("User-Agent", Constants.HTTP_AGENT)
                .addHeader("Authorization", "Bearer " + configParams.get(Constants.EF_PARAMS_API_KEY).toString())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();

            return gson.fromJson(responseData, CreateEmbeddingResponse.class);
        } catch (IOException e) {
            System.out.println(e.getClass());
            throw new EFException(e);
        }
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest()
                        .model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString())
                        .inputType("search_query")
                        .texts(new String[]{query})
        );
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(@NotNull List<String> documents) throws EFException {
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest()
                        .model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString())
                        .inputType("search_document")
                        .texts(documents.toArray(new String[0]))
        );
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }
}
