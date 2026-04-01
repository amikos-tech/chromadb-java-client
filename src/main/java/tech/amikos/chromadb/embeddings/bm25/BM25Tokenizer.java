package tech.amikos.chromadb.embeddings.bm25;

import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BM25-compatible tokenizer pipeline matching the Go client:
 * lowercase, regex split on non-alphanumeric, stop word filter, max-length filter, Snowball stem.
 */
public class BM25Tokenizer {

    private final Set<String> stopWords;
    private final int tokenMaxLength;
    private final englishStemmer stemmer;

    /**
     * Creates a tokenizer with default stop words and token max length of 100.
     */
    public BM25Tokenizer() {
        this(BM25StopWords.DEFAULT, 100);
    }

    /**
     * Creates a tokenizer with custom stop words and token max length.
     *
     * @param stopWords      set of stop words to filter
     * @param tokenMaxLength maximum allowed token length
     */
    public BM25Tokenizer(Set<String> stopWords, int tokenMaxLength) {
        this.stopWords = stopWords;
        this.tokenMaxLength = tokenMaxLength;
        this.stemmer = new englishStemmer();
    }

    /**
     * Tokenizes text through the BM25 pipeline: lowercase, split, filter, stem.
     *
     * @param text the input text
     * @return list of stemmed tokens (not deduplicated -- TF counting happens in BM25)
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Lowercase
        String lower = text.toLowerCase(Locale.ROOT);

        // 2. Replace non-alphanumeric with space
        String cleaned = lower.replaceAll("[^a-zA-Z0-9]+", " ");

        // 3. Split on whitespace
        String trimmed = cleaned.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = trimmed.split("\\s+");

        // 4-6. Filter stop words, max length, and stem
        List<String> result = new ArrayList<>();
        for (String token : parts) {
            if (token.isEmpty()) {
                continue;
            }
            if (stopWords.contains(token)) {
                continue;
            }
            if (token.length() > tokenMaxLength) {
                continue;
            }
            // Stem
            stemmer.setCurrent(token);
            stemmer.stem();
            result.add(stemmer.getCurrent());
        }
        return result;
    }
}
