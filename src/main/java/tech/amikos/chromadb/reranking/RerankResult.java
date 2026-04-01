package tech.amikos.chromadb.reranking;

/**
 * Immutable value type representing a single reranking result.
 *
 * <p>Each result contains the original document index and its relevance score.</p>
 */
public final class RerankResult {

    private final int index;
    private final double score;

    private RerankResult(int index, double score) {
        this.index = index;
        this.score = score;
    }

    /**
     * Creates a new rerank result.
     *
     * @param index the original index of the document in the input list
     * @param score the relevance score assigned by the reranker
     * @return a new RerankResult
     */
    public static RerankResult of(int index, double score) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        return new RerankResult(index, score);
    }

    /**
     * Returns the original index of the document in the input list.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the relevance score assigned by the reranker.
     */
    public double getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RerankResult that = (RerankResult) o;
        return index == that.index && Double.compare(that.score, score) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(score);
        return 31 * index + (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return "RerankResult{index=" + index + ", score=" + score + "}";
    }
}
