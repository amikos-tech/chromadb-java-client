package tech.amikos.chromadb.embeddings.bm25;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.SparseEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.SparseVector;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BM25 sparse embedding function that produces {@link SparseVector} output.
 *
 * <p>Uses the same tokenization pipeline as the Go and Python Chroma clients
 * for cross-client index compatibility: lowercase, regex split, stop word filter,
 * Snowball English stemmer, Murmur3 hashing.</p>
 *
 * <p>Default BM25 parameters: K=1.2, B=0.75, avgDocLen=256.</p>
 */
public class BM25EmbeddingFunction implements SparseEmbeddingFunction {

    // Use Charset.forName for Java 8 compatibility (StandardCharsets requires API level check)
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    static final float K = 1.2f;
    static final float B = 0.75f;
    static final float DEFAULT_AVG_DOC_LEN = 256.0f;

    private final BM25Tokenizer tokenizer;
    private final float avgDocLen;

    /**
     * Creates a BM25 embedding function with default parameters.
     */
    public BM25EmbeddingFunction() {
        this(new BM25Tokenizer(), DEFAULT_AVG_DOC_LEN);
    }

    /**
     * Creates a BM25 embedding function with custom tokenizer and average document length.
     *
     * @param tokenizer the tokenizer pipeline to use
     * @param avgDocLen the expected average document length for BM25 normalization
     */
    public BM25EmbeddingFunction(BM25Tokenizer tokenizer, float avgDocLen) {
        if (tokenizer == null) {
            throw new IllegalArgumentException("tokenizer must not be null");
        }
        this.tokenizer = tokenizer;
        this.avgDocLen = avgDocLen;
    }

    @Override
    public SparseVector embedQuery(String query) throws EFException {
        if (query == null) {
            throw new ChromaException("BM25 embedding failed: query must not be null");
        }
        return embedSingle(query);
    }

    @Override
    public List<SparseVector> embedDocuments(List<String> documents) throws EFException {
        if (documents == null) {
            throw new ChromaException("BM25 embedding failed: documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException("BM25 embedding failed: documents must not be empty");
        }

        List<SparseVector> results = new ArrayList<SparseVector>(documents.size());
        for (String doc : documents) {
            results.add(embedSingle(doc));
        }
        return results;
    }

    private SparseVector embedSingle(String text) {
        if (text == null || text.isEmpty()) {
            return SparseVector.of(new int[0], new float[0]);
        }

        List<String> tokens = tokenizer.tokenize(text);
        if (tokens.isEmpty()) {
            return SparseVector.of(new int[0], new float[0]);
        }

        int docLen = tokens.size();

        // Count term frequencies
        Map<String, Integer> tf = new LinkedHashMap<>();
        for (String token : tokens) {
            Integer count = tf.get(token);
            tf.put(token, count == null ? 1 : count + 1);
        }

        // Compute BM25 scores per hashed index
        // Use TreeMap to get sorted indices automatically
        TreeMap<Integer, Float> scoreMap = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            String token = entry.getKey();
            int tfVal = entry.getValue();

            float score = (tfVal * (K + 1)) / (tfVal + K * (1 - B + B * docLen / avgDocLen));

            int idx = Murmur3.hash32(token.getBytes(UTF_8), 0);

            // Accumulate scores per index (collision: sum)
            Float existing = scoreMap.get(idx);
            if (existing != null) {
                scoreMap.put(idx, existing + score);
            } else {
                scoreMap.put(idx, score);
            }
        }

        // Build sorted arrays
        int[] indices = new int[scoreMap.size()];
        float[] values = new float[scoreMap.size()];
        int i = 0;
        for (Map.Entry<Integer, Float> entry : scoreMap.entrySet()) {
            indices[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }

        return SparseVector.of(indices, values);
    }
}
