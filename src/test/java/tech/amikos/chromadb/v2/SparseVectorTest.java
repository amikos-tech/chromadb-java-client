package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.*;

public class SparseVectorTest {

    @Test
    public void testOfCreatesImmutableVector() {
        int[] indices = {1, 5, 10};
        float[] values = {0.3f, 0.7f, 0.2f};
        SparseVector sv = SparseVector.of(indices, values);
        assertNotNull(sv);
        assertArrayEquals(new int[]{1, 5, 10}, sv.getIndices());
        assertArrayEquals(new float[]{0.3f, 0.7f, 0.2f}, sv.getValues(), 1e-6f);
    }

    @Test
    public void testDefensiveCopyOnConstruction() {
        int[] indices = {1, 5, 10};
        float[] values = {0.3f, 0.7f, 0.2f};
        SparseVector sv = SparseVector.of(indices, values);
        // Mutate the original arrays
        indices[0] = 99;
        values[0] = 9.9f;
        // SparseVector should not reflect the changes
        assertArrayEquals(new int[]{1, 5, 10}, sv.getIndices());
        assertArrayEquals(new float[]{0.3f, 0.7f, 0.2f}, sv.getValues(), 1e-6f);
    }

    @Test
    public void testDefensiveCopyOnGetters() {
        SparseVector sv = SparseVector.of(new int[]{1, 5, 10}, new float[]{0.3f, 0.7f, 0.2f});
        // Mutate the returned arrays
        int[] returnedIndices = sv.getIndices();
        float[] returnedValues = sv.getValues();
        returnedIndices[0] = 99;
        returnedValues[0] = 9.9f;
        // SparseVector should not reflect the changes on subsequent calls
        assertArrayEquals(new int[]{1, 5, 10}, sv.getIndices());
        assertArrayEquals(new float[]{0.3f, 0.7f, 0.2f}, sv.getValues(), 1e-6f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullIndicesThrows() {
        SparseVector.of(null, new float[]{0.1f});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullValuesThrows() {
        SparseVector.of(new int[]{1}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedLengthThrows() {
        SparseVector.of(new int[]{1, 2}, new float[]{0.1f});
    }

    @Test
    public void testEqualsAndHashCode() {
        SparseVector sv1 = SparseVector.of(new int[]{1, 5, 10}, new float[]{0.3f, 0.7f, 0.2f});
        SparseVector sv2 = SparseVector.of(new int[]{1, 5, 10}, new float[]{0.3f, 0.7f, 0.2f});
        SparseVector sv3 = SparseVector.of(new int[]{1, 5, 99}, new float[]{0.3f, 0.7f, 0.2f});

        assertEquals("Same data should be equal", sv1, sv2);
        assertEquals("Same data should have same hashCode", sv1.hashCode(), sv2.hashCode());
        assertNotEquals("Different data should not be equal", sv1, sv3);
    }

    @Test
    public void testToString() {
        SparseVector sv = SparseVector.of(new int[]{1, 5}, new float[]{0.3f, 0.7f});
        String str = sv.toString();
        assertNotNull(str);
        assertTrue("toString should contain indices", str.contains("1") && str.contains("5"));
        assertTrue("toString should contain values", str.contains("0.3") || str.contains("0.7"));
    }
}
