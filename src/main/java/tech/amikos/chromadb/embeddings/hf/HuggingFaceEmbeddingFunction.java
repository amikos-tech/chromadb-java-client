package tech.amikos.chromadb.embeddings.hf;


import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static tech.amikos.chromadb.Constants.JSON;

public class HuggingFaceEmbeddingFunction implements EmbeddingFunction {
    public static final String DEFAULT_MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";
    public static final String DEFAULT_BASE_API = "https://api-inference.huggingface.co/pipeline/feature-extraction/";
    public static final String HF_API_KEY_ENV = "HF_API_KEY";
    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, Object> configParams = new HashMap<>();
    private static final Gson gson = new Gson();

    private static final List<WithParam> defaults = Arrays.asList(
            WithParam.baseAPI(DEFAULT_BASE_API),
            WithParam.defaultModel(DEFAULT_MODEL_NAME)
    );

    public HuggingFaceEmbeddingFunction() throws EFException {
        for (WithParam param : defaults) {
            param.apply(this.configParams);
        }
        WithParam.apiKeyFromEnv(HF_API_KEY_ENV).apply(this.configParams);
    }

    public HuggingFaceEmbeddingFunction(WithParam... params) throws EFException {
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
                .url(this.configParams.get(Constants.EF_PARAMS_BASE_API).toString() + this.configParams.get(Constants.EF_PARAMS_MODEL).toString())
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", Constants.HTTP_AGENT)
                .addHeader("Authorization", "Bearer " + configParams.get(Constants.EF_PARAMS_API_KEY).toString())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();

            List parsedResponse = gson.fromJson(responseData, List.class);

            return new CreateEmbeddingResponse(parsedResponse);
        } catch (IOException e) {
            throw new EFException(e);
        }
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingResponse response = this.createEmbedding(new CreateEmbeddingRequest().inputs(new String[]{query}));
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(@NotNull List<String> documents) throws EFException {
        CreateEmbeddingResponse response = this.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings().stream().map(Embedding::fromList).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        CreateEmbeddingResponse response = this.createEmbedding(new CreateEmbeddingRequest().inputs(documents));
        return response.getEmbeddings().stream().map(Embedding::fromList).collect(Collectors.toList());
    }
}
