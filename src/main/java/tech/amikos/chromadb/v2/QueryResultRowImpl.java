package tech.amikos.chromadb.v2;

import java.util.Map;

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
}
