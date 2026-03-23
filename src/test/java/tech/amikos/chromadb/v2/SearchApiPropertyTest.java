package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.*;

/**
 * Property-based tests for Search API types using QuickTheories.
 * Validates bounds, roundtrip invariants, and numerical stability.
 */
public class SearchApiPropertyTest {

    // --- SparseVector properties ---

    @Test
    public void sparseVectorRoundtripPreservesData() {
        // For any valid length, SparseVector.of(indices, values) preserves them exactly
        qt().forAll(
                integers().between(1, 100)
        ).checkAssert(len -> {
            int[] indices = new int[len];
            float[] values = new float[len];
            for (int i = 0; i < len; i++) {
                indices[i] = i * 3;
                values[i] = (float) (i * 0.1);
            }
            SparseVector sv = SparseVector.of(indices, values);
            assertArrayEquals(indices, sv.getIndices());
            assertArrayEquals(values, sv.getValues(), 0.0f);
        });
    }

    @Test
    public void sparseVectorImmutabilityProperty() {
        // Mutating the array returned by getIndices() never affects the SparseVector
        qt().forAll(integers().between(1, 50)).checkAssert(len -> {
            int[] indices = new int[len];
            float[] values = new float[len];
            for (int i = 0; i < len; i++) {
                indices[i] = i;
                values[i] = i;
            }
            SparseVector sv = SparseVector.of(indices, values);
            int[] got = sv.getIndices();
            got[0] = -999;
            assertEquals(0, sv.getIndices()[0]);
        });
    }

    // --- Knn immutability properties ---

    @Test
    public void knnFluentChainProducesNewInstances() {
        // Every fluent method on Knn produces a distinct object
        qt().forAll(integers().between(1, 100)).checkAssert(limit -> {
            Knn base = Knn.queryText("test");
            Knn withLimit = base.limit(limit);
            assertNotSame(base, withLimit);
            assertNull(base.getLimit());
            assertEquals(Integer.valueOf(limit), withLimit.getLimit());
        });
    }

    @Test
    public void knnEmbeddingDefensiveCopyProperty() {
        // For any float array, modifying the input or output never changes Knn state
        qt().forAll(integers().between(1, 20)).checkAssert(len -> {
            float[] original = new float[len];
            for (int i = 0; i < len; i++) original[i] = i * 1.5f;
            Knn knn = Knn.queryEmbedding(original);
            original[0] = -999f;
            float[] q = (float[]) knn.getQuery();
            assertEquals(0f, q[0], 0.001f);
            q[0] = -888f;
            float[] q2 = (float[]) knn.getQuery();
            assertEquals(0f, q2[0], 0.001f);
        });
    }

    // --- Score precision property ---

    @Test
    public void scoreRoundtripPreservesDoublePrecision() {
        // For any Double score, the SearchResultRow preserves it exactly (no Float narrowing)
        qt().forAll(doubles().between(-1e15, 1e15)).checkAssert(score -> {
            SearchResultRowImpl row = new SearchResultRowImpl(
                    "id1", "doc", null, null, null, score);
            assertEquals(score, row.getScore(), 0.0);
        });
    }

    // --- GroupBy validation properties ---

    @Test
    public void groupByMinKNeverExceedsMaxK() {
        // For any valid offset (0-99), minK = 1+offset, maxK = minK + offset2 ensures minK <= maxK
        qt().forAll(integers().between(1, 50), integers().between(0, 50))
                .checkAssert((minK, extra) -> {
                    int maxK = minK + extra;
                    GroupBy gb = GroupBy.builder().key("k").minK(minK).maxK(maxK).build();
                    assertEquals(Integer.valueOf(minK), gb.getMinK());
                    assertEquals(Integer.valueOf(maxK), gb.getMaxK());
                });
    }

    @Test
    public void groupByMinKExceedingMaxKAlwaysFails() {
        // For any maxK in [1,99] and gap in [1,100], minK = maxK + gap > maxK, so build() always throws
        qt().forAll(integers().between(1, 99), integers().between(1, 100))
                .checkAssert((maxK, gap) -> {
                    int minK = maxK + gap;
                    try {
                        GroupBy.builder().key("k").minK(minK).maxK(maxK).build();
                        fail("Should throw for minK=" + minK + " > maxK=" + maxK);
                    } catch (IllegalArgumentException e) {
                        // expected
                    }
                });
    }

    // --- Select equality property ---

    @Test
    public void selectKeyEquality() {
        // Select.key(x).equals(Select.key(x)) for any non-blank string
        // Prefix with "k" to guarantee non-blank
        qt().forAll(strings().basicLatinAlphabet().ofLengthBetween(0, 49))
                .checkAssert(suffix -> {
                    String key = "k" + suffix;
                    assertEquals(Select.key(key), Select.key(key));
                    assertEquals(Select.key(key).hashCode(), Select.key(key).hashCode());
                });
    }

    // --- Search builder mutual exclusivity property ---

    @Test
    public void searchBuilderExactlyOneRankAlwaysRequired() {
        // build() always fails without knn or rrf
        qt().forAll(integers().between(1, 100)).checkAssert(limit -> {
            try {
                Search.builder().limit(limit).build();
                fail("Should throw without rank");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("neither"));
            }
        });
    }
}
