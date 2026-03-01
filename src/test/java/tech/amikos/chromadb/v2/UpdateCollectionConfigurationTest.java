package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateCollectionConfigurationTest {

    @Test
    public void testBuilderSetsHnswFields() {
        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .hnswNumThreads(4)
                .hnswBatchSize(500)
                .hnswSyncThreshold(1000)
                .hnswResizeFactor(1.5)
                .build();

        assertEquals(Integer.valueOf(200), cfg.getHnswSearchEf());
        assertEquals(Integer.valueOf(4), cfg.getHnswNumThreads());
        assertEquals(Integer.valueOf(500), cfg.getHnswBatchSize());
        assertEquals(Integer.valueOf(1000), cfg.getHnswSyncThreshold());
        assertEquals(Double.valueOf(1.5), cfg.getHnswResizeFactor());
    }

    @Test
    public void testBuilderSetsSpannFields() {
        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .spannSearchNprobe(32)
                .spannEfSearch(64)
                .build();

        assertEquals(Integer.valueOf(32), cfg.getSpannSearchNprobe());
        assertEquals(Integer.valueOf(64), cfg.getSpannEfSearch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildRejectsEmpty() {
        UpdateCollectionConfiguration.builder().build();
    }

    @Test
    public void testBuildRejectsMixedHnswAndSpann() {
        try {
            UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .spannEfSearch(64)
                .build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot update both"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    @Test
    public void testEqualsHashCodeAndToString() {
        UpdateCollectionConfiguration a = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .hnswBatchSize(500)
                .build();
        UpdateCollectionConfiguration b = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .hnswBatchSize(500)
                .build();
        UpdateCollectionConfiguration c = UpdateCollectionConfiguration.builder()
                .spannEfSearch(64)
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("hnsw={"));
        assertTrue(c.toString().contains("spann={"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswSearchEfRejectsNonPositive() {
        UpdateCollectionConfiguration.builder().hnswSearchEf(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpannSearchNprobeRejectsNonPositive() {
        UpdateCollectionConfiguration.builder().spannSearchNprobe(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswResizeFactorRejectsNaN() {
        UpdateCollectionConfiguration.builder().hnswResizeFactor(Double.NaN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswResizeFactorRejectsInfinity() {
        UpdateCollectionConfiguration.builder().hnswResizeFactor(Double.POSITIVE_INFINITY);
    }
}
