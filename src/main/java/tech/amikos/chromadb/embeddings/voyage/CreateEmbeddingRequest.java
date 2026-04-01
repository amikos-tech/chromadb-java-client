package tech.amikos.chromadb.embeddings.voyage;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Request body for the Voyage AI embeddings API.
 */
public class CreateEmbeddingRequest {

    @SerializedName("input")
    private List<String> input;

    @SerializedName("model")
    private String model;

    @SerializedName("input_type")
    private String inputType;

    public CreateEmbeddingRequest() {
    }

    public CreateEmbeddingRequest input(List<String> input) {
        this.input = input;
        return this;
    }

    public CreateEmbeddingRequest model(String model) {
        this.model = model;
        return this;
    }

    public CreateEmbeddingRequest inputType(String inputType) {
        this.inputType = inputType;
        return this;
    }

    public List<String> getInput() {
        return input;
    }

    public String getModel() {
        return model;
    }

    public String getInputType() {
        return inputType;
    }

    public String json() {
        return new Gson().toJson(this);
    }
}
