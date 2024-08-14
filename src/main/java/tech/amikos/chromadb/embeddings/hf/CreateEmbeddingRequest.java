package tech.amikos.chromadb.embeddings.hf;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class CreateEmbeddingRequest {

    @SerializedName("inputs")
    private String[] inputs;
    @SerializedName("options")
    private HashMap<String, Object> options;

    public CreateEmbeddingRequest inputs(String[] inputs) {
        this.inputs = inputs;
        return this;
    }

    public CreateEmbeddingRequest options(HashMap<String, Object> options) {
        this.options = options;
        return this;
    }

    public String[] getInputs() {
        return inputs;
    }

    public HashMap<String, Object> getOptions() {
        return options;
    }

    public String toString() {
        return this.json();
    }

    public String json() {
        return new Gson().toJson(this);
    }
}
