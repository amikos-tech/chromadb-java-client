package tech.amikos.chromadb.embeddings.ollama;

import com.google.gson.Gson;
import okhttp3.*;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OllamaEmbeddingFunction implements EmbeddingFunction {
    private final static URL DEFAULT_EMBED_ENDPOINT;
    private final static String DEFAULT_MODEL = "nomic-embed-text";

    static {
        try {
            DEFAULT_EMBED_ENDPOINT = new URL("http://localhost:11434/api/embed");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final URL embedApiUrl;
    private OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private String defaultModel = "nomic-embed-text";

    /**
     * Default constructor
     */
    public OllamaEmbeddingFunction() {
        this(DEFAULT_EMBED_ENDPOINT, DEFAULT_MODEL);
    }


    /**
     * The model defaults to "nomic-embed-text"
     *
     * @param embedApiUrl - The embedding endpoint URL
     */
    public OllamaEmbeddingFunction(URL embedApiUrl) {
        this(embedApiUrl, DEFAULT_MODEL);
    }

    /**
     * The embedding URL defaults to "http://localhost:11434/api/embed"
     *
     * @param defaultModel - the default model to use for embedding requests without a specific model
     */
    public OllamaEmbeddingFunction(String defaultModel) {
        this(DEFAULT_EMBED_ENDPOINT, defaultModel);
    }


    /**
     * @param embedApiUrl  - The embedding endpoint URL
     * @param defaultModel - the default model to use for embedding requests without a specific model
     */
    public OllamaEmbeddingFunction(URL embedApiUrl, String defaultModel) {
        this.embedApiUrl = embedApiUrl;
        this.defaultModel = defaultModel;
    }

    private CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) throws EFException {
        Request request = new Request.Builder()
                .url(this.embedApiUrl)
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "chroma-java-client")
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
        CreateEmbeddingResponse response = createEmbedding(new CreateEmbeddingRequest().model(defaultModel).input(new String[]{query}));
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        CreateEmbeddingResponse response = createEmbedding(new CreateEmbeddingRequest().model(defaultModel).input(documents.toArray(new String[0])));
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }
}
