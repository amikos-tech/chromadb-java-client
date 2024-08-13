package tech.amikos.chromadb.embeddings.hf;


import tech.amikos.chromadb.EmbeddingFunction;

import java.util.List;

public class HuggingFaceEmbeddingFunction implements EmbeddingFunction {
    private final String hfAPIKey;
    private final HuggingFaceClient client;
    public HuggingFaceEmbeddingFunction(String hfAPIKey) {
        this.hfAPIKey = hfAPIKey;
         this.client = new HuggingFaceClient(this.hfAPIKey);
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents) {

        CreateEmbeddingResponse response = this.client.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings();
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        client.modelId(model);
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings();
    }
}
