package tech.amikos.chromadb.embeddings.ollama;

import com.google.gson.Gson;
import okhttp3.*;
import tech.amikos.chromadb.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static tech.amikos.chromadb.Constants.JSON;

public class OllamaEmbeddingFunction implements EmbeddingFunction {
    public final static String DEFAULT_BASE_API = "http://localhost:11434/api/embed";
    public final static String DEFAULT_MODEL_NAME = "nomic-embed-text";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<String, Object> configParams = new HashMap<>();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    public OllamaEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
    }

    public OllamaEmbeddingFunction(WithParam... params) throws EFException {
        // apply defaults

        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        for (WithParam param : params) {
            param.apply(this.configParams);
        }
    }

    private CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) throws EFException {
        Request request = new Request.Builder()
                .url(this.configParams.get(Constants.EF_PARAMS_BASE_API).toString())
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", Constants.HTTP_AGENT)
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
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest()
                        .model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString())
                        .input(new String[]{query})
        );
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest()
                        .model(this.configParams.get(Constants.EF_PARAMS_MODEL).toString())
                        .input(documents.toArray(new String[0]))
        );
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }
}
