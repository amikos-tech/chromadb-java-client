package tech.amikos.chromadb;

import tech.amikos.openai.CreateEmbeddingRequest;
import tech.amikos.openai.CreateEmbeddingResponse;
import tech.amikos.openai.OpenAIClient;

import java.util.List;
import java.util.stream.Collectors;

public class OpenAIEmbeddingFunction implements EmbeddingFunction {

    private final String openAIAPIKey;
    private final String modelName;

    public OpenAIEmbeddingFunction(String openAIAPIKey) {
        this.openAIAPIKey = openAIAPIKey;
        this.modelName = "text-embedding-ada-002";
    }

    public OpenAIEmbeddingFunction(String openAIAPIKey, String modelName) {
        this.openAIAPIKey = openAIAPIKey;
        this.modelName = modelName;
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(this.openAIAPIKey)
                .createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(model);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(this.openAIAPIKey)
                .createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }
}
