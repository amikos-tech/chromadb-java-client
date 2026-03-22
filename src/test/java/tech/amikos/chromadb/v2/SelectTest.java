package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class SelectTest {

    @Test
    public void testStandardConstants() {
        assertEquals("#document", Select.DOCUMENT.getKey());
        assertEquals("#score", Select.SCORE.getKey());
        assertEquals("#embedding", Select.EMBEDDING.getKey());
        assertEquals("#metadata", Select.METADATA.getKey());
        assertEquals("#id", Select.ID.getKey());
    }

    @Test
    public void testKeyFactory() {
        assertEquals("title", Select.key("title").getKey());
        assertEquals("category", Select.key("category").getKey());
        // No "#" prefix added for custom keys
        assertFalse("custom key should not start with #", Select.key("title").getKey().startsWith("#"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyNullThrows() {
        Select.key(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyBlankThrows() {
        Select.key("   ");
    }

    @Test
    public void testAllReturnsAllFiveConstants() {
        Select[] all = Select.all();
        assertEquals("Select.all() should return 5 elements", 5, all.length);
        Set<String> keys = new HashSet<String>();
        for (Select s : all) {
            keys.add(s.getKey());
        }
        assertTrue("Should contain #id", keys.contains("#id"));
        assertTrue("Should contain #document", keys.contains("#document"));
        assertTrue("Should contain #embedding", keys.contains("#embedding"));
        assertTrue("Should contain #metadata", keys.contains("#metadata"));
        assertTrue("Should contain #score", keys.contains("#score"));
    }

    @Test
    public void testEqualsOnSameKey() {
        Select s1 = Select.key("title");
        Select s2 = Select.key("title");
        assertEquals("Same key should be equal", s1, s2);
        assertEquals("Same hashCode for same key", s1.hashCode(), s2.hashCode());

        // Select.DOCUMENT equals a Select with key "#document"
        Select docByKey = Select.key("#document");
        assertEquals("DOCUMENT constant equals key('#document')", Select.DOCUMENT, docByKey);
    }

    @Test
    public void testNotEqualOnDifferentKey() {
        assertNotEquals("DOCUMENT should not equal SCORE", Select.DOCUMENT, Select.SCORE);
        assertNotEquals("Different custom keys should not be equal", Select.key("a"), Select.key("b"));
    }
}
