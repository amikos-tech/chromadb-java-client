package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.fail;

public class WhereTest {

    @Test
    public void testEqualityFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", true); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", false); } });
    }

    @Test
    public void testComparisonFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gt("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gt("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gte("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gte("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lt("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lt("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lte("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lte("k", 1.0f); } });
    }

    @Test
    public void testSetFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", "a", "b"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", 1, 2); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", 1.0f, 2.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", "a", "b"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", 1, 2); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", 1.0f, 2.0f); } });
    }

    @Test
    public void testLogicalCombinatorsThrowUnsupportedOperationException() {
        final Where first = stubWhere();
        final Where second = stubWhere();
        assertNotImplemented(new Runnable() { @Override public void run() { Where.and(first, second); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.or(first, second); } });
    }

    @Test
    public void testInstanceLogicalCombinatorsDelegateToStaticFactories() {
        final Where first = stubWhere();
        final Where second = stubWhere();
        assertNotImplemented(new Runnable() { @Override public void run() { first.and(second); } });
        assertNotImplemented(new Runnable() { @Override public void run() { first.or(second); } });
    }

    private static Where stubWhere() {
        return new Where() {
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
