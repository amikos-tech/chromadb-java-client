package tech.amikos.chromadb.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Package-private immutable implementation of {@link ResultRow}.
 *
 * <p>All fields may be {@code null} when the corresponding {@link Include} value was not requested.
 * Embeddings are defensively copied on construction and on every {@link #getEmbedding()} call.
 * Metadata maps are stored as unmodifiable copies.
 */
final class ResultRowImpl implements ResultRow {

    private final String id;
    private final String document;
    private final Map<String, Object> metadata;
    private final float[] embedding;
    private final String uri;

    ResultRowImpl(String id, String document, Map<String, Object> metadata,
                  float[] embedding, String uri) {
        this.id = id;
        this.document = document;
        this.metadata = metadata == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
        this.embedding = embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
        this.uri = uri;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDocument() {
        return document;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public float[] getEmbedding() {
        return embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResultRowImpl)) return false;
        ResultRowImpl other = (ResultRowImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(document, other.document)
                && Objects.equals(metadata, other.metadata)
                && Arrays.equals(embedding, other.embedding)
                && Objects.equals(uri, other.uri);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(document);
        result = 31 * result + Objects.hashCode(metadata);
        result = 31 * result + Arrays.hashCode(embedding);
        result = 31 * result + Objects.hashCode(uri);
        return result;
    }

    @Override
    public String toString() {
        return "ResultRow{id=" + id
                + ", document=" + document
                + ", metadata=" + metadata
                + ", embedding=" + Arrays.toString(embedding)
                + ", uri=" + uri + "}";
    }
}
