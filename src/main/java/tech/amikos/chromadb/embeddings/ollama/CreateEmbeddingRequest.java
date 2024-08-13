package tech.amikos.chromadb.embeddings.ollama;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class CreateEmbeddingRequest {
    @SerializedName("model")
    private String model = "embed-english-v2.0";

    @SerializedName("input")
    private String[] input;



    //create fluent api for all fields
    public CreateEmbeddingRequest model(String model) {
        this.model = model;
        return this;
    }

    public CreateEmbeddingRequest input(String[] input) {
        this.input = input;
        return this;
    }


    public String json() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson customGson = gsonBuilder.create();
        return customGson.toJson(this);
    }

    public String toString() {
        return json();
    }
}
