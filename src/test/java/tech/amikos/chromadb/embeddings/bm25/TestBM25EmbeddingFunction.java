package tech.amikos.chromadb.embeddings.bm25;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.v2.SparseVector;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for BM25EmbeddingFunction.
 */
public class TestBM25EmbeddingFunction {

    private final BM25EmbeddingFunction ef = new BM25EmbeddingFunction();

    @Test
    public void testEmbedQueryReturnsVector() throws EFException {
        SparseVector result = ef.embedQuery("the quick brown fox jumps over the lazy dog");
        assertNotNull("Result should not be null", result);
        assertTrue("Should have indices", result.getIndices().length > 0);
        assertTrue("Should have values", result.getValues().length > 0);
        assertEquals("Indices and values should have same length",
                result.getIndices().length, result.getValues().length);
    }

    @Test
    public void testIndicesSorted() throws EFException {
        SparseVector result = ef.embedQuery("the quick brown fox jumps over the lazy dog");
        int[] indices = result.getIndices();
        for (int i = 1; i < indices.length; i++) {
            assertTrue("Indices should be sorted ascending: " + indices[i - 1] + " <= " + indices[i],
                    indices[i - 1] <= indices[i]);
        }
    }

    @Test
    public void testEmbedDocuments() throws EFException {
        List<SparseVector> results = ef.embedDocuments(Arrays.asList("hello world", "foo bar"));
        assertEquals("Should return 2 vectors", 2, results.size());
        assertNotNull("First vector should not be null", results.get(0));
        assertNotNull("Second vector should not be null", results.get(1));
    }

    @Test
    public void testEmptyTextReturnsEmptySparse() throws EFException {
        SparseVector result = ef.embedQuery("");
        assertEquals("Empty text should produce 0 indices", 0, result.getIndices().length);
        assertEquals("Empty text should produce 0 values", 0, result.getValues().length);
    }

    @Test
    public void testNullTextReturnsEmptySparse() throws EFException {
        SparseVector result = ef.embedQuery(null);
        assertEquals("Null text should produce 0 indices", 0, result.getIndices().length);
    }

    @Test
    public void testDeterministic() throws EFException {
        String input = "deterministic test input";
        SparseVector result1 = ef.embedQuery(input);
        SparseVector result2 = ef.embedQuery(input);
        assertArrayEquals("Same input should produce same indices", result1.getIndices(), result2.getIndices());
        assertArrayEquals("Same input should produce same values", result1.getValues(), result2.getValues(), 0.0001f);
    }

    @Test
    public void testCustomAvgDocLen() throws EFException {
        BM25EmbeddingFunction customEf = new BM25EmbeddingFunction(new BM25Tokenizer(), 100.0f);
        SparseVector defaultResult = ef.embedQuery("hello world example text for testing");
        SparseVector customResult = customEf.embedQuery("hello world example text for testing");

        // Same indices (same tokens) but different scores due to different avgDocLen
        assertArrayEquals("Should have same indices",
                defaultResult.getIndices(), customResult.getIndices());

        // Values should differ because avgDocLen changes BM25 normalization
        boolean anyDiffer = false;
        float[] defaultValues = defaultResult.getValues();
        float[] customValues = customResult.getValues();
        for (int i = 0; i < defaultValues.length; i++) {
            if (Math.abs(defaultValues[i] - customValues[i]) > 0.0001f) {
                anyDiffer = true;
                break;
            }
        }
        assertTrue("Different avgDocLen should produce different scores", anyDiffer);
    }

    @Test
    public void testPositiveValues() throws EFException {
        SparseVector result = ef.embedQuery("test document with multiple words");
        float[] values = result.getValues();
        for (float v : values) {
            assertTrue("BM25 scores should be positive: " + v, v > 0);
        }
    }
}
