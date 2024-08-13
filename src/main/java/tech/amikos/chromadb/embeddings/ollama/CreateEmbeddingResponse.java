package tech.amikos.chromadb.embeddings.ollama;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.List;

public class CreateEmbeddingResponse {

    @SerializedName("model")
    public String model;

    @SerializedName("embeddings")
    public List<List<Float>> embeddings;

    // create getters for all fields

    public String getModel() {
        return model;
    }


    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


}
