package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class EmbeddingFunctionConfig {
    @SerializedName("name")
    private final String name;

    @SerializedName("config")
    private final Map<String, Object> config;

    public EmbeddingFunctionConfig(String name, Map<String, Object> config) {
        this.name = name;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public static EmbeddingFunctionConfig legacy() {
        return new EmbeddingFunctionConfig("legacy", null);
    }

    public static EmbeddingFunctionConfig custom(String name, Map<String, Object> config) {
        return new EmbeddingFunctionConfig(name, config);
    }
}