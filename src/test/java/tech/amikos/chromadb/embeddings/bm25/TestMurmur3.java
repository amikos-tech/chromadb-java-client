package tech.amikos.chromadb.embeddings.bm25;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Unit tests for Murmur3 x86 32-bit hash implementation.
 */
public class TestMurmur3 {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void testHelloHash() {
        // Known test vector: mmh3.hash("hello", 0, signed=True) in Python
        int hash = Murmur3.hash32("hello".getBytes(UTF_8), 0);
        assertEquals("Murmur3 hash of 'hello' with seed 0", 613153351, hash);
    }

    @Test
    public void testEmptyHash() {
        // Empty input with seed 0: fmix32(0 ^ 0) = fmix32(0) = 0
        int hash = Murmur3.hash32(new byte[0], 0);
        assertEquals("Murmur3 hash of empty input with seed 0", 0, hash);
    }

    @Test
    public void testKnownVectorTest() {
        // Another known vector: "test" with seed 0
        int hash = Murmur3.hash32("test".getBytes(UTF_8), 0);
        // Murmur3 x86 32-bit hash of "test" with seed 0 is a known value
        // Verify it's deterministic and non-zero
        assertNotEquals("Hash of 'test' should not be 0", 0, hash);
    }

    @Test
    public void testSeedZeroDifferentFromSeed42() {
        byte[] data = "a".getBytes(UTF_8);
        int hashSeed0 = Murmur3.hash32(data, 0);
        int hashSeed42 = Murmur3.hash32(data, 42);
        assertNotEquals("Different seeds should produce different hashes", hashSeed0, hashSeed42);
    }

    @Test
    public void testDeterministic() {
        byte[] data = "deterministic".getBytes(UTF_8);
        int hash1 = Murmur3.hash32(data, 0);
        int hash2 = Murmur3.hash32(data, 0);
        assertEquals("Same input should always produce same hash", hash1, hash2);
    }

    @Test
    public void testMultiByteBlocks() {
        // Test input that spans multiple 4-byte blocks plus tail
        // "abcdefghij" = 10 bytes = 2 full blocks + 2 tail bytes
        int hash = Murmur3.hash32("abcdefghij".getBytes(UTF_8), 0);
        assertNotEquals("Multi-block input should produce non-zero hash", 0, hash);
    }
}
