package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.v2.SparseVector;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSparseEmbeddingFunction {

    @Test
    public void testEmbedQueryReturnsVector() throws Exception {
        SparseEmbeddingFunction ef = new SparseEmbeddingFunction() {
            @Override
            public SparseVector embedQuery(String query) {
                return SparseVector.of(new int[]{1, 2}, new float[]{0.5f, 0.8f});
            }

            @Override
            public List<SparseVector> embedDocuments(List<String> documents) {
                return null;
            }
        };

        SparseVector result = ef.embedQuery("test");
        assertNotNull(result);
        assertArrayEquals(new int[]{1, 2}, result.getIndices());
        assertArrayEquals(new float[]{0.5f, 0.8f}, result.getValues(), 0.001f);
    }

    @Test
    public void testEmbedDocumentsReturnsList() throws Exception {
        SparseEmbeddingFunction ef = new SparseEmbeddingFunction() {
            @Override
            public SparseVector embedQuery(String query) {
                return SparseVector.of(new int[]{0}, new float[]{1.0f});
            }

            @Override
            public List<SparseVector> embedDocuments(List<String> documents) {
                SparseVector[] results = new SparseVector[documents.size()];
                for (int i = 0; i < documents.size(); i++) {
                    results[i] = SparseVector.of(new int[]{i}, new float[]{1.0f});
                }
                return Arrays.asList(results);
            }
        };

        List<SparseVector> results = ef.embedDocuments(Arrays.asList("a", "b"));
        assertEquals(2, results.size());
    }
}
