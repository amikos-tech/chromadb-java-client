package tech.amikos.chromadb.embeddings.bm25;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for BM25Tokenizer pipeline.
 */
public class TestBM25Tokenizer {

    private final BM25Tokenizer tokenizer = new BM25Tokenizer();

    @Test
    public void testStopWordFiltering() {
        List<String> tokens = tokenizer.tokenize("The quick brown fox");
        // "the" is a stop word and should be filtered
        assertFalse("'the' should be filtered as a stop word",
                tokens.contains("the"));
        assertTrue("Result should contain stemmed tokens", tokens.size() > 0);
    }

    @Test
    public void testLowercaseAndStrip() {
        List<String> tokens = tokenizer.tokenize("Hello!!! World???");
        // All tokens should be lowercase, non-alphanumeric stripped
        for (String token : tokens) {
            assertEquals("Tokens should be lowercase", token.toLowerCase(), token);
            assertFalse("Tokens should not contain '!'", token.contains("!"));
            assertFalse("Tokens should not contain '?'", token.contains("?"));
        }
        assertTrue("Should produce tokens", tokens.size() > 0);
    }

    @Test
    public void testEmptyString() {
        List<String> tokens = tokenizer.tokenize("");
        assertTrue("Empty string should produce empty list", tokens.isEmpty());
    }

    @Test
    public void testNullString() {
        List<String> tokens = tokenizer.tokenize(null);
        assertTrue("Null string should produce empty list", tokens.isEmpty());
    }

    @Test
    public void testStemming() {
        List<String> tokens = tokenizer.tokenize("running runs");
        // Both "running" and "runs" should stem to "run" (Snowball English)
        assertEquals("Both words should produce tokens", 2, tokens.size());
        assertEquals("First token should be stemmed to 'run'", "run", tokens.get(0));
        assertEquals("Second token should be stemmed to 'run'", "run", tokens.get(1));
    }

    @Test
    public void testMaxTokenLength() {
        // Create tokenizer with maxLength=5
        BM25Tokenizer shortTokenizer = new BM25Tokenizer(
                Collections.<String>emptySet(), 5);
        List<String> tokens = shortTokenizer.tokenize("abcdef short");
        // "abcdef" (6 chars) should be filtered, "short" (5 chars) should remain
        assertEquals("Only token within max length should remain", 1, tokens.size());
    }

    @Test
    public void testOnlyStopWords() {
        List<String> tokens = tokenizer.tokenize("the a an");
        assertTrue("All-stop-word input should produce empty list", tokens.isEmpty());
    }

    @Test
    public void testStopWordsCount() {
        assertEquals("Default stop words should have 179 entries", 179, BM25StopWords.DEFAULT.size());
        assertTrue("Should contain 'ourselves'", BM25StopWords.DEFAULT.contains("ourselves"));
        assertTrue("Should contain 'the'", BM25StopWords.DEFAULT.contains("the"));
    }
}
