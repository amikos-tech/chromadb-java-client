package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable server pre-flight capability information. */
public final class PreFlightInfo {

    private final int maxBatchSize;
    private final Boolean supportsBase64Encoding;

    /**
     * Creates pre-flight information.
     *
     * @param maxBatchSize maximum records accepted in a batch operation
     * @param supportsBase64Encoding whether the server supports base64 encoding; may be {@code null}
     *                               when unavailable on older servers
     * @throws IllegalArgumentException if {@code maxBatchSize < 0}
     */
    public PreFlightInfo(int maxBatchSize, Boolean supportsBase64Encoding) {
        if (maxBatchSize < 0) {
            throw new IllegalArgumentException("maxBatchSize must be >= 0");
        }
        this.maxBatchSize = maxBatchSize;
        this.supportsBase64Encoding = supportsBase64Encoding;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public Boolean getSupportsBase64Encoding() {
        return supportsBase64Encoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreFlightInfo)) return false;
        PreFlightInfo that = (PreFlightInfo) o;
        return maxBatchSize == that.maxBatchSize
                && Objects.equals(supportsBase64Encoding, that.supportsBase64Encoding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxBatchSize, supportsBase64Encoding);
    }

    @Override
    public String toString() {
        return "PreFlightInfo{"
                + "maxBatchSize=" + maxBatchSize
                + ", supportsBase64Encoding=" + supportsBase64Encoding
                + '}';
    }
}
