package tech.amikos.chromadb.v2;

/**
 * Immutable snapshot of collection indexing progress.
 *
 * <p>Returned by {@link Collection#indexingStatus()}. All fields are server-authoritative.
 * No computed convenience properties are exposed — callers derive any derived metrics
 * from the four primitive values.</p>
 */
public final class IndexingStatus {

    private static final double PROGRESS_EPSILON = 1e-6;

    private final long numIndexedOps;
    private final long numUnindexedOps;
    private final long totalOps;
    private final double opIndexingProgress;

    private IndexingStatus(long numIndexedOps, long numUnindexedOps, long totalOps, double opIndexingProgress) {
        this.numIndexedOps = numIndexedOps;
        this.numUnindexedOps = numUnindexedOps;
        this.totalOps = totalOps;
        this.opIndexingProgress = opIndexingProgress;
    }

    /**
     * Creates an {@code IndexingStatus} snapshot.
     *
     * @param numIndexedOps      number of operations that have been indexed; must not be negative
     * @param numUnindexedOps    number of operations not yet indexed; must not be negative
     * @param totalOps           total number of operations; must not be negative
     * @param opIndexingProgress fraction of operations indexed (0.0–1.0)
     * @return new immutable snapshot
     * @throws IllegalArgumentException if any count is negative or progress is outside [0.0, 1.0]
     */
    public static IndexingStatus of(long numIndexedOps, long numUnindexedOps, long totalOps, double opIndexingProgress) {
        if (numIndexedOps < 0) {
            throw new IllegalArgumentException("numIndexedOps must not be negative: " + numIndexedOps);
        }
        if (numUnindexedOps < 0) {
            throw new IllegalArgumentException("numUnindexedOps must not be negative: " + numUnindexedOps);
        }
        if (totalOps < 0) {
            throw new IllegalArgumentException("totalOps must not be negative: " + totalOps);
        }
        if (Double.isNaN(opIndexingProgress) || opIndexingProgress < 0.0 || opIndexingProgress > 1.0 + PROGRESS_EPSILON) {
            throw new IllegalArgumentException("opIndexingProgress must be in [0.0, 1.0]: " + opIndexingProgress);
        }
        return new IndexingStatus(numIndexedOps, numUnindexedOps, totalOps, opIndexingProgress);
    }

    /** Returns the number of operations that have been indexed. */
    public long getNumIndexedOps() {
        return numIndexedOps;
    }

    /** Returns the number of operations not yet indexed. */
    public long getNumUnindexedOps() {
        return numUnindexedOps;
    }

    /** Returns the total number of operations. */
    public long getTotalOps() {
        return totalOps;
    }

    /** Returns the fraction of operations that have been indexed (0.0–1.0). */
    public double getOpIndexingProgress() {
        return opIndexingProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexingStatus)) return false;
        IndexingStatus that = (IndexingStatus) o;
        return numIndexedOps == that.numIndexedOps
                && numUnindexedOps == that.numUnindexedOps
                && totalOps == that.totalOps
                && Double.compare(opIndexingProgress, that.opIndexingProgress) == 0;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(numIndexedOps);
        result = 31 * result + Long.hashCode(numUnindexedOps);
        result = 31 * result + Long.hashCode(totalOps);
        result = 31 * result + Long.hashCode(Double.doubleToLongBits(opIndexingProgress));
        return result;
    }

    @Override
    public String toString() {
        return "IndexingStatus{"
                + "numIndexedOps=" + numIndexedOps
                + ", numUnindexedOps=" + numUnindexedOps
                + ", totalOps=" + totalOps
                + ", opIndexingProgress=" + opIndexingProgress
                + '}';
    }
}
