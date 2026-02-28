package tech.amikos.chromadb.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Options for creating a collection. */
public final class CreateCollectionOptions {

    private final Map<String, Object> metadata;
    private final CollectionConfiguration configuration;

    private CreateCollectionOptions(Builder builder) {
        this.metadata = builder.metadata != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata))
                : null;
        this.configuration = builder.configuration;
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

    public static final class Builder {
        private Map<String, Object> metadata;
        private CollectionConfiguration configuration;

        Builder() {}

        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public Builder configuration(CollectionConfiguration configuration) { this.configuration = configuration; return this; }

        public CreateCollectionOptions build() {
            return new CreateCollectionOptions(this);
        }
    }
}
