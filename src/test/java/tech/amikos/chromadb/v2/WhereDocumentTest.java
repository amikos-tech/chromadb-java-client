package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WhereDocumentTest {

    @Test
    public void testTextFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.contains("text"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.notContains("text"); } });
    }

    @Test
    public void testRegexFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.regex("\\bAI\\b"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.notRegex("\\bAI\\b"); } });
    }

    @Test
    public void testLogicalCombinatorsThrowUnsupportedOperationException() {
        final WhereDocument first = stubWhereDocument();
        final WhereDocument second = stubWhereDocument();
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.and(first, second); } });
        assertNotImplemented(new Runnable() { @Override public void run() { WhereDocument.or(first, second); } });
    }

    @Test
    public void testInstanceLogicalCombinatorsDelegateToStaticFactories() {
        final WhereDocument first = stubWhereDocument();
        final WhereDocument second = stubWhereDocument();
        assertNotImplemented(new Runnable() { @Override public void run() { first.and(second); } });
        assertNotImplemented(new Runnable() { @Override public void run() { first.or(second); } });
    }

    @Test
    public void testFromMapFactoryCopiesInput() {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("$contains", "hello");

        WhereDocument fromMap = WhereDocument.fromMap(source);
        source.put("$contains", "changed");

        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$contains", "hello");
        assertEquals(expected, fromMap.toMap());
    }

    @Test
    public void testFromMapFactoryRejectsNull() {
        try {
            WhereDocument.fromMap(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static WhereDocument stubWhereDocument() {
        return new WhereDocument() {
            @Override
            public Map<String, Object> toMap() {
                return Collections.<String, Object>emptyMap();
            }
        };
    }

    private static void assertNotImplemented(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
