package tech.amikos.chromadb.embeddings.chromacloudsplade;

import com.google.gson.Gson;
import okhttp3.MediaType;

import java.util.List;

/**
 * Request DTO for the Chroma Cloud Splade sparse embedding API.
 */
public class CreateSparseEmbeddingRequest {

    private String model;
    private List<String> texts;

    public CreateSparseEmbeddingRequest model(String model) {
        this.model = model;
        return this;
    }

    public CreateSparseEmbeddingRequest texts(List<String> texts) {
        this.texts = texts;
        return this;
    }

    /**
     * Serializes this request to a JSON MediaType body.
     */
    public okhttp3.MediaType json() {
        return MediaType.parse("application/json; charset=utf-8");
    }

    /**
     * Returns the JSON string representation of this request.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }
}
