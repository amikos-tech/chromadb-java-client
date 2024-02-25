package tech.amikos.chromadb;

import java.util.HashMap;
import java.util.Map;

public class MetadataBuilder {
    private final Map<String, Object> metadata = new HashMap<String, Object>();

    private MetadataBuilder() {
    }

    public static MetadataBuilder create() {
        return new MetadataBuilder();
    }

    public MetadataBuilder forValue(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return metadata;
    }
}
