package tech.amikos.cohere;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.List;

public class CreateEmbeddingResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("texts")
    public String[] texts;

    @SerializedName("embeddings")
    public List<List<Float>> embeddings;

    @SerializedName("meta")
    public LinkedTreeMap<String, Object> meta;

    // create getters for all fields

    public String getId() {
        return id;
    }

    public String[] getTexts() {
        return texts;
    }

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public LinkedTreeMap<String, Object> getMeta() {
        return meta;
    }


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


}
