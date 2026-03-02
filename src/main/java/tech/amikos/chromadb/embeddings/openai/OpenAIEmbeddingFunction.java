package tech.amikos.chromadb.embeddings.openai;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static tech.amikos.chromadb.Constants.JSON;

public class OpenAIEmbeddingFunction implements EmbeddingFunction {

    public static final String DEFAULT_MODEL_NAME = "text-embedding-ada-002";
    public static final String DEFAULT_BASE_API = "https://api.openai.com/v1/embeddings";
    public static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<>();
    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );


    public OpenAIEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(OPENAI_API_KEY_ENV).apply(this.configParams);
    }

    public OpenAIEmbeddingFunction(WithParam... params) throws EFException {
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
                .url(this.configParams.get(Constants.EF_PARAMS_BASE_API).toString())
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + configParams.get(Constants.EF_PARAMS_API_KEY).toString())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();

            return gson.fromJson(responseData, CreateEmbeddingResponse.class);
        } catch (IOException e) {
            throw new EFException(e);
        }
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString());
        req.input(new CreateEmbeddingRequest.Input(query));
        CreateEmbeddingResponse response = this.createEmbedding(req);
        return new Embedding(response.getData().get(0).getEmbedding());
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString());
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.createEmbedding(req);
        return response.getData().stream().map(emb -> new Embedding(emb.getEmbedding())).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }

    @Override
    public List<Embedding> embedQueries(List<String> queries) throws EFException {
        return embedDocuments(queries);
    }

    @Override
    public List<Embedding> embedQueries(String[] queries) throws EFException {
        return embedQueries(Arrays.asList(queries));
    }
}
