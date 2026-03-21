package tech.amikos.chromadb.v2;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Package-private immutable implementation of {@link QueryResultRow}.
 *
 * <p>Delegates base {@link ResultRow} behaviour to a composed {@link ResultRowImpl}.
 * {@link #getDistance()} returns {@code null} when {@link Include#DISTANCES} was not requested.
 */
final class QueryResultRowImpl implements QueryResultRow {

    private final ResultRowImpl base;
    private final Float distance;

    QueryResultRowImpl(String id, String document, Map<String, Object> metadata,
                       float[] embedding, String uri, Float distance) {
        this.base = new ResultRowImpl(id, document, metadata, embedding, uri);
        this.distance = distance;
    }

    @Override
    public String getId() {
        return base.getId();
    }

    @Override
    public String getDocument() {
        return base.getDocument();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return base.getMetadata();
    }

    @Override
    public float[] getEmbedding() {
        return base.getEmbedding();
    }

    @Override
    public String getUri() {
        return base.getUri();
    }

    @Override
    public Float getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QueryResultRowImpl)) return false;
        QueryResultRowImpl other = (QueryResultRowImpl) obj;
        return base.equals(other.base) && Objects.equals(distance, other.distance);
    }

    @Override
    public int hashCode() {
        return 31 * base.hashCode() + Objects.hashCode(distance);
    }

    @Override
    public String toString() {
        return "QueryResultRow{id=" + getId()
                + ", document=" + getDocument()
                + ", metadata=" + getMetadata()
                + ", embedding=" + Arrays.toString(getEmbedding())
                + ", uri=" + getUri()
                + ", distance=" + distance + "}";
    }
}
