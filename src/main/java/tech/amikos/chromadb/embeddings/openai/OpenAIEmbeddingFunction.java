package tech.amikos.chromadb.embeddings.openai;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;

import java.util.List;
import java.util.stream.Collectors;

public class OpenAIEmbeddingFunction implements EmbeddingFunction {

    public static final String DEFAULT_MODEL_NAME = "text-embedding-ada-002";

    private final String modelName;

    private final OpenAIClient client;

    public OpenAIEmbeddingFunction(String openAIAPIKey) {
        this(openAIAPIKey, DEFAULT_MODEL_NAME, null);
    }

    public OpenAIEmbeddingFunction(String openAIAPIKey, String modelName) {
        this(openAIAPIKey, modelName, null);
    }

    public OpenAIEmbeddingFunction(String openAIAPIKey, String modelName, String apiEndpoint) {
        this.modelName = modelName;
        this.client = new OpenAIClient();
        this.client.apiKey(openAIAPIKey).baseUrl(apiEndpoint);
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(query));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return new Embedding(response.getData().get(0).getEmbedding());
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> new Embedding(emb.getEmbedding())).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(documents));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> new Embedding(emb.getEmbedding())).collect(Collectors.toList());
    }


    public static EFBuilder Instance() {
        return new EFBuilder();
    }

    public static class EFBuilder {
        private String openAIAPIKey;
        private String modelName = DEFAULT_MODEL_NAME;
        private String apiEndpoint = null;

        public OpenAIEmbeddingFunction build() {
            return new OpenAIEmbeddingFunction(openAIAPIKey, modelName, apiEndpoint);
        }

        public EFBuilder withOpenAIAPIKey(String openAIAPIKey) {
            this.openAIAPIKey = openAIAPIKey;
            return this;
        }

        public EFBuilder withModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public EFBuilder withApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

    }
}
