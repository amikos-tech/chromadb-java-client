package tech.amikos.chromadb.reranking;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestRerankResult {

    @Test
    public void testOfAndGetters() {
        RerankResult result = RerankResult.of(2, 0.85);
        assertEquals(2, result.getIndex());
        assertEquals(0.85, result.getScore(), 0.0001);
    }

    @Test
    public void testEquality() {
        RerankResult a = RerankResult.of(1, 0.5);
        RerankResult b = RerankResult.of(1, 0.5);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testInequality() {
        RerankResult a = RerankResult.of(1, 0.5);
        RerankResult b = RerankResult.of(2, 0.5);
        assertNotEquals(a, b);
    }

    @Test
    public void testInequalityByScore() {
        RerankResult a = RerankResult.of(1, 0.5);
        RerankResult b = RerankResult.of(1, 0.9);
        assertNotEquals(a, b);
    }

    @Test
    public void testToString() {
        RerankResult result = RerankResult.of(2, 0.85);
        String str = result.toString();
        assertTrue(str.contains("index=2"));
        assertTrue(str.contains("score=0.85"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIndexRejected() {
        RerankResult.of(-1, 0.1);
    }
}
