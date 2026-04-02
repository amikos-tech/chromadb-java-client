package tech.amikos.chromadb.embeddings.chromacloudsplade;

import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.SparseVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for the Chroma Cloud Splade sparse embedding API.
 */
public class CreateSparseEmbeddingResponse {

    private List<SparseResult> results;

    /**
     * A single sparse embedding result from the API.
     */
    public static class SparseResult {
        private List<Integer> indices;
        private List<Float> values;

        public List<Integer> getIndices() {
            return indices;
        }

        public List<Float> getValues() {
            return values;
        }
    }

    public List<SparseResult> getResults() {
        return results;
    }

    /**
     * Converts the API response into a list of {@link SparseVector} instances.
     */
    public List<SparseVector> toSparseVectors() {
        List<SparseVector> vectors = new ArrayList<>();
        if (results == null) {
            return vectors;
        }
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            SparseResult r = results.get(resultIndex);
            if (r == null) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed: result at index " + resultIndex + " was null");
            }
            if (r.indices == null) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed: result at index " + resultIndex
                                + " has no indices");
            }
            if (r.values == null) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed: result at index " + resultIndex
                                + " has no values");
            }
            if (r.indices.size() != r.values.size()) {
                throw new ChromaException(
                        "Chroma Cloud Splade embedding failed: result at index " + resultIndex
                                + " has mismatched indices and values sizes");
            }
            int[] idx = new int[r.indices.size()];
            float[] vals = new float[r.values.size()];
            for (int i = 0; i < r.indices.size(); i++) {
                Integer index = r.indices.get(i);
                Float value = r.values.get(i);
                if (index == null) {
                    throw new ChromaException(
                            "Chroma Cloud Splade embedding failed: result at index " + resultIndex
                                    + " has null index at position " + i);
                }
                if (value == null) {
                    throw new ChromaException(
                            "Chroma Cloud Splade embedding failed: result at index " + resultIndex
                                    + " has null value at position " + i);
                }
                idx[i] = index;
                vals[i] = value;
            }
            vectors.add(SparseVector.of(idx, vals));
        }
        return vectors;
    }
}
