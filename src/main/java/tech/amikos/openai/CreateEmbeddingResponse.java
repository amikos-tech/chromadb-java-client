package tech.amikos.openai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateEmbeddingResponse {

    @SerializedName("object")
    private String object;

    @SerializedName("data")
    private List<EmbeddingData> data;

    @SerializedName("model")
    private String model;

    @SerializedName("usage")
    private Usage usage;

    public List<EmbeddingData> getData() {
        return data;
    }

    public static class EmbeddingData {
        @SerializedName("object")
        private String object;

        @SerializedName("index")
        private int index;

        @SerializedName("embedding")
        private List<Float> embedding;

        public List<Float> getEmbedding() {
            return embedding;
        }
    }


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


    private static class Usage {
        @SerializedName("prompt_tokens")
        private Integer promptTokens;
        @SerializedName("total_tokens")
        private Integer totalTokens;
    }
}
