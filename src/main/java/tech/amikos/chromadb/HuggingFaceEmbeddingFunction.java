package tech.amikos.chromadb;


import tech.amikos.hf.CreateEmbeddingRequest;
import tech.amikos.hf.CreateEmbeddingResponse;
import tech.amikos.hf.HuggingFaceClient;

import java.util.List;

public class HuggingFaceEmbeddingFunction implements EmbeddingFunction {

    private final String hfAPIKey;

    public HuggingFaceEmbeddingFunction(String hfAPIKey) {
        this.hfAPIKey = hfAPIKey;

    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents) {
        HuggingFaceClient client = new HuggingFaceClient(this.hfAPIKey);
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings();
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        HuggingFaceClient client = new HuggingFaceClient(this.hfAPIKey);
        client.modelId(model);
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings();
    }
}
