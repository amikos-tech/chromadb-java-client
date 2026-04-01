package tech.amikos.chromadb.embeddings.chromacloudsplade;

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
        for (SparseResult r : results) {
            int[] idx = new int[r.indices.size()];
            float[] vals = new float[r.values.size()];
            for (int i = 0; i < r.indices.size(); i++) {
                idx[i] = r.indices.get(i);
                vals[i] = r.values.get(i);
            }
            vectors.add(SparseVector.of(idx, vals));
        }
        return vectors;
    }
}
