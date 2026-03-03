package tech.amikos.chromadb.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Collection schema with default and per-key value-type index settings. */
public final class Schema {

    public static final String DOCUMENT_KEY = "#document";
    public static final String EMBEDDING_KEY = "#embedding";

    private final ValueTypes defaults;
    private final Map<String, ValueTypes> keys;
    private final Cmek cmek;

    private Schema(Builder builder) {
        this.defaults = builder.defaults;
        this.keys = builder.keys == null
                ? Collections.<String, ValueTypes>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, ValueTypes>(builder.keys));
        this.cmek = builder.cmek;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ValueTypes getDefaults() {
        return defaults;
    }

    public Map<String, ValueTypes> getKeys() {
        return keys;
    }

    public Cmek getCmek() {
        return cmek;
    }

    public ValueTypes getKey(String key) {
        return keys.get(key);
    }

    /**
     * Returns embedding-function spec from the default embedding vector index key, if present.
     */
    public EmbeddingFunctionSpec getDefaultEmbeddingFunctionSpec() {
        ValueTypes valueTypes = keys.get(EMBEDDING_KEY);
        if (valueTypes == null || valueTypes.getFloatList() == null) {
            return null;
        }
        VectorIndexType vectorIndex = valueTypes.getFloatList().getVectorIndex();
        if (vectorIndex == null || vectorIndex.getConfig() == null) {
            return null;
        }
        return vectorIndex.getConfig().getEmbeddingFunction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Schema)) return false;
        Schema schema = (Schema) o;
        return Objects.equals(defaults, schema.defaults)
                && Objects.equals(keys, schema.keys)
                && Objects.equals(cmek, schema.cmek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaults, keys, cmek);
    }

    @Override
    public String toString() {
        return "Schema{" + "defaults=" + defaults + ", keys=" + keys + ", cmek=" + cmek + '}';
    }

    public static final class Builder {
        private ValueTypes defaults;
        private Map<String, ValueTypes> keys;
        private Cmek cmek;

        Builder() {}

        public Builder defaults(ValueTypes defaults) {
            this.defaults = defaults;
            return this;
        }

        public Builder keys(Map<String, ValueTypes> keys) {
            if (keys == null) {
                this.keys = null;
                return this;
            }
            LinkedHashMap<String, ValueTypes> validated = new LinkedHashMap<String, ValueTypes>();
            for (Map.Entry<String, ValueTypes> entry : keys.entrySet()) {
                String normalizedKey = requireNonBlank("key", entry.getKey());
                ValueTypes valueTypes = Objects.requireNonNull(
                        entry.getValue(),
                        "valueTypes for key '" + normalizedKey + "'"
                );
                validated.put(normalizedKey, valueTypes);
            }
            this.keys = validated;
            return this;
        }

        public Builder key(String key, ValueTypes valueTypes) {
            String normalizedKey = requireNonBlank("key", key);
            ValueTypes normalizedValueTypes = Objects.requireNonNull(valueTypes, "valueTypes");
            if (this.keys == null) {
                this.keys = new LinkedHashMap<String, ValueTypes>();
            }
            this.keys.put(normalizedKey, normalizedValueTypes);
            return this;
        }

        public Builder cmek(Cmek cmek) {
            this.cmek = cmek;
            return this;
        }

        public Schema build() {
            return new Schema(this);
        }

        private static String requireNonBlank(String fieldName, String value) {
            if (value == null) {
                throw new NullPointerException(fieldName);
            }
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalized;
        }
    }
}
