package tech.amikos.chromadb;

import tech.amikos.openai.CreateEmbeddingRequest;
import tech.amikos.openai.CreateEmbeddingResponse;
import tech.amikos.openai.OpenAIClient;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OpenAIEmbeddingFunction implements EmbeddingFunction {

    private final String modelName;

    private final OpenAIClient client;

    public OpenAIEmbeddingFunction(String openAIAPIKey) {
       this(openAIAPIKey, "text-embedding-ada-002", null);
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
    public List<List<Float>> createEmbedding(List<String> documents) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(model);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }


    public static EFBuilder Instance() {
        return new EFBuilder();
    }

    public static class EFBuilder {
        private String openAIAPIKey;
        private String modelName = "text-embedding-ada-002";
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
