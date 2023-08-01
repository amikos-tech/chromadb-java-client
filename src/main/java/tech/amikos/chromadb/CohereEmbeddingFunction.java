package tech.amikos.chromadb;

import tech.amikos.cohere.CohereClient;
import tech.amikos.cohere.CreateEmbeddingRequest;
import tech.amikos.cohere.CreateEmbeddingResponse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CohereEmbeddingFunction implements EmbeddingFunction {

    private final String cohereAPIKey;

    public CohereEmbeddingFunction(String cohereAPIKey) {
        this.cohereAPIKey = cohereAPIKey;

    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents) {
        CohereClient client = new CohereClient(this.cohereAPIKey);
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(documents.toArray(new String[0])));
        return response.getEmbeddings();
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        CohereClient client = new CohereClient(this.cohereAPIKey);
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(documents.toArray(new String[0])).model(model));
        return response.getEmbeddings();
    }
}
