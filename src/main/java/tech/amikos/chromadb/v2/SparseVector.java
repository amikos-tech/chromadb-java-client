package tech.amikos.chromadb.v2;

import java.util.Arrays;

/**
 * Immutable sparse vector value type holding integer indices and float values.
 *
 * <p>Indices and values arrays must be the same length. Defensive copies are applied on
 * construction and on every getter call to prevent aliasing.</p>
 */
public final class SparseVector {

    private final int[] indices;
    private final float[] values;

    private SparseVector(int[] indices, float[] values) {
        this.indices = Arrays.copyOf(indices, indices.length);
        this.values = Arrays.copyOf(values, values.length);
    }

    /**
     * Creates a new {@code SparseVector} from the given indices and values arrays.
     *
     * @param indices non-null array of index positions; must have the same length as {@code values}
     * @param values  non-null array of float values; must have the same length as {@code indices}
     * @return an immutable sparse vector
     * @throws IllegalArgumentException if either array is null or if they have different lengths
     */
    public static SparseVector of(int[] indices, float[] values) {
        if (indices == null) {
            throw new IllegalArgumentException("indices must not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        if (indices.length != values.length) {
            throw new IllegalArgumentException(
                    "indices and values must have the same length, got "
                            + indices.length + " and " + values.length);
        }
        return new SparseVector(indices, values);
    }

    /**
     * Returns a defensive copy of the index positions.
     */
    public int[] getIndices() {
        return Arrays.copyOf(indices, indices.length);
    }

    /**
     * Returns a defensive copy of the float values.
     */
    public float[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseVector)) return false;
        SparseVector that = (SparseVector) o;
        return Arrays.equals(indices, that.indices) && Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(indices);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString() {
        return "SparseVector{indices=" + Arrays.toString(indices)
                + ", values=" + Arrays.toString(values) + '}';
    }
}
