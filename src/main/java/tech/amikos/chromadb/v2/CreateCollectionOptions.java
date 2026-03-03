package tech.amikos.chromadb.v2;

import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Options for creating a collection. */
public final class CreateCollectionOptions {

    private final Map<String, Object> metadata;
    private final CollectionConfiguration configuration;
    private final Schema schema;
    private final EmbeddingFunction embeddingFunction;

    private CreateCollectionOptions(Builder builder) {
        this.metadata = builder.metadata != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata))
                : null;
        this.configuration = builder.configuration;
        this.schema = builder.schema;
        this.embeddingFunction = builder.embeddingFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Convenience: options with just metadata. */
    public static CreateCollectionOptions withMetadata(Map<String, Object> metadata) {
        return builder().metadata(metadata).build();
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public CollectionConfiguration getConfiguration() { return configuration; }

    /**
     * Returns the top-level schema payload sent during collection creation.
     */
    public Schema getSchema() { return schema; }

    /**
     * Returns an explicit runtime embedding function bound to the created collection instance.
     *
     * <p>This runtime object is used for local operations like {@code queryTexts(...)} and is
     * not serialized to server JSON.</p>
     */
    public EmbeddingFunction getEmbeddingFunction() { return embeddingFunction; }

    public static final class Builder {
        private Map<String, Object> metadata;
        private CollectionConfiguration configuration;
        private Schema schema;
        private EmbeddingFunction embeddingFunction;

        Builder() {}

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new LinkedHashMap<String, Object>(metadata) : null;
            return this;
        }
        public Builder configuration(CollectionConfiguration configuration) { this.configuration = configuration; return this; }

        /**
         * Sets top-level schema for create/get-or-create requests.
         */
        public Builder schema(Schema schema) { this.schema = schema; return this; }

        /**
         * Sets explicit runtime embedding function used by client-side text embedding flows.
         */
        public Builder embeddingFunction(EmbeddingFunction embeddingFunction) {
            this.embeddingFunction = embeddingFunction;
            return this;
        }

        public CreateCollectionOptions build() {
            return new CreateCollectionOptions(this);
        }
    }
}
