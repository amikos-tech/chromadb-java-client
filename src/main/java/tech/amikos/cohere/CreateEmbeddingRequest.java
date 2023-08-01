package tech.amikos.cohere;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class CreateEmbeddingRequest {
    @SerializedName("model")
    private String model = "embed-english-v2.0";

    @SerializedName("texts")
    private String[] texts;

    @SerializedName("truncate")
    private TruncateMode truncateMode = TruncateMode.END;
    @SerializedName("compress")
    private Boolean compress;

    @SerializedName("compression_codebook")
    private String compressionCodebook;

    public enum TruncateMode {
        NONE,
        START,
        END,
    }

    public static class TruncateModeSerializer implements JsonSerializer<TruncateMode> {
        @Override
        public JsonElement serialize(TruncateMode src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString().toUpperCase());
        }
    }

    //create fluent api for all fields
    public CreateEmbeddingRequest model(String model) {
        this.model = model;
        return this;
    }

    public CreateEmbeddingRequest texts(String[] texts) {
        this.texts = texts;
        return this;
    }

    public CreateEmbeddingRequest truncateMode(TruncateMode truncateMode) {
        this.truncateMode = truncateMode;
        return this;
    }

    public CreateEmbeddingRequest compress(Boolean compress) {
        this.compress = compress;
        return this;
    }

    public CreateEmbeddingRequest compressionCodebook(String compressionCodebook) {
        this.compressionCodebook = compressionCodebook;
        return this;
    }

    public String json() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(TruncateMode.class, new TruncateModeSerializer());
        Gson customGson = gsonBuilder.create();
        return customGson.toJson(this);
    }

    public String toString() {
        return json();
    }
}
