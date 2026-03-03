package tech.amikos.chromadb.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable embedding-function descriptor used in collection configuration/schema.
 */
public final class EmbeddingFunctionSpec {

    private final String type;
    private final String name;
    private final Map<String, Object> config;

    private EmbeddingFunctionSpec(Builder builder) {
        this.type = normalizeNullable(builder.type);
        this.name = requireNonBlank("name", builder.name);
        this.config = builder.config == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.config));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns raw provider config values.
     *
     * <p>Security note: this map may include sensitive values (for example API keys). Avoid
     * logging or serializing it directly.</p>
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    public boolean isKnownType() {
        return type != null && "known".equals(type.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingFunctionSpec)) return false;
        EmbeddingFunctionSpec that = (EmbeddingFunctionSpec) o;
        return Objects.equals(type, that.type)
                && Objects.equals(name, that.name)
                && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, config);
    }

    @Override
    public String toString() {
        return "EmbeddingFunctionSpec{"
                + "type='" + type + '\''
                + ", name='" + name + '\''
                + ", config=" + redactConfig(config)
                + '}';
    }

    public static final class Builder {
        private String type;
        private String name;
        private Map<String, Object> config;

        Builder() {}

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder config(Map<String, Object> config) {
            this.config = config == null ? null : new LinkedHashMap<String, Object>(config);
            return this;
        }

        public EmbeddingFunctionSpec build() {
            return new EmbeddingFunctionSpec(this);
        }
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

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Map<String, Object> redactConfig(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> redacted = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (isSensitiveKey(key)) {
                redacted.put(key, "***");
            } else {
                redacted.put(key, entry.getValue());
            }
        }
        return redacted;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("api_key")
                || normalized.contains("apikey")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("password");
    }
}
