package tech.amikos.chromadb.v2;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Package-private immutable implementation of {@link SearchResultRow}.
 *
 * <p>Delegates base {@link ResultRow} behaviour to a composed {@link ResultRowImpl}.
 * {@link #getScore()} returns {@code null} when {@link Select#SCORE} was not projected.
 */
final class SearchResultRowImpl implements SearchResultRow {

    private final ResultRowImpl base;
    private final Float score;

    SearchResultRowImpl(String id, String document, Map<String, Object> metadata,
                        float[] embedding, String uri, Float score) {
        this.base = new ResultRowImpl(id, document, metadata, embedding, uri);
        this.score = score;
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
    public Float getScore() {
        return score;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SearchResultRowImpl)) return false;
        SearchResultRowImpl other = (SearchResultRowImpl) obj;
        return base.equals(other.base) && Objects.equals(score, other.score);
    }

    @Override
    public int hashCode() {
        return 31 * base.hashCode() + Objects.hashCode(score);
    }

    @Override
    public String toString() {
        return "SearchResultRow{id=" + getId()
                + ", document=" + getDocument()
                + ", metadata=" + getMetadata()
                + ", embedding=" + Arrays.toString(getEmbedding())
                + ", uri=" + getUri()
                + ", score=" + score + "}";
    }
}
