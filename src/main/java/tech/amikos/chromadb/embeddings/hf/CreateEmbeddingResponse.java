package tech.amikos.chromadb.embeddings.hf;

import com.google.gson.Gson;

import java.util.List;

public class CreateEmbeddingResponse {
    public List<List<Float>> embeddings;

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public CreateEmbeddingResponse(List<List<Float>> embeddings) {
        this.embeddings = embeddings;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
