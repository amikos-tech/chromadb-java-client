package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class IdGeneratorTest {

    // --- UUID ---

    @Test
    public void testUuidGeneratesValidFormat() {
        String id = UuidIdGenerator.INSTANCE.generate("doc", null);
        assertTrue("Expected UUID format, got: " + id,
                Pattern.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}", id));
    }

    @Test
    public void testUuidUniqueness() {
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            ids.add(UuidIdGenerator.INSTANCE.generate(null, null));
        }
        assertEquals(1000, ids.size());
    }

    @Test
    public void testUuidAcceptsNullDocumentAndMetadata() {
        String id = UuidIdGenerator.INSTANCE.generate(null, null);
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    @Test
    public void testUuidSingleton() {
        assertSame(UuidIdGenerator.INSTANCE, UuidIdGenerator.INSTANCE);
    }

    // --- ULID ---

    @Test
    public void testUlidLength() {
        String id = UlidIdGenerator.INSTANCE.generate("doc", null);
        assertEquals(26, id.length());
    }

    @Test
    public void testUlidValidCrockfordCharacters() {
        String id = UlidIdGenerator.INSTANCE.generate("doc", null);
        // Crockford base32 excludes I, L, O, U
        assertTrue("Invalid Crockford chars in: " + id,
                Pattern.matches("[0-9A-HJKMNP-TV-Z]{26}", id));
    }

    @Test
    public void testUlidDeterministicEncode() {
        // Known timestamp and random bytes for reproducible output
        long timestamp = 1234567890123L;
        byte[] random = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};

        String result1 = UlidIdGenerator.encode(timestamp, random);
        String result2 = UlidIdGenerator.encode(timestamp, random);
        assertEquals(result1, result2);
        assertEquals(26, result1.length());
    }

    @Test
    public void testUlidKnownEncode() {
        // timestamp = 0, random = all zeros → all '0' characters
        String allZeros = UlidIdGenerator.encode(0L, new byte[10]);
        assertEquals("00000000000000000000000000", allZeros);

        // timestamp = 0, random = all 0xFF → max random portion
        byte[] allOnes = new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        String maxRandom = UlidIdGenerator.encode(0L, allOnes);
        assertEquals("0000000000ZZZZZZZZZZZZZZZZ", maxRandom);
    }

    @Test
    public void testUlidUniqueness() {
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            ids.add(UlidIdGenerator.INSTANCE.generate(null, null));
        }
        assertEquals(1000, ids.size());
    }

    @Test
    public void testUlidLexicographicSortOrder() throws InterruptedException {
        String id1 = UlidIdGenerator.INSTANCE.generate(null, null);
        Thread.sleep(2); // ensure different millisecond
        String id2 = UlidIdGenerator.INSTANCE.generate(null, null);
        assertTrue("Expected id1 < id2, got id1=" + id1 + " id2=" + id2,
                id1.compareTo(id2) < 0);
    }

    @Test
    public void testUlidAcceptsNullDocumentAndMetadata() {
        String id = UlidIdGenerator.INSTANCE.generate(null, null);
        assertNotNull(id);
        assertEquals(26, id.length());
    }

    @Test
    public void testUlidSingleton() {
        assertSame(UlidIdGenerator.INSTANCE, UlidIdGenerator.INSTANCE);
    }

    // --- SHA-256 ---

    @Test
    public void testSha256KnownHash() {
        String id = Sha256IdGenerator.INSTANCE.generate("hello", null);
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", id);
    }

    @Test
    public void testSha256Deterministic() {
        String id1 = Sha256IdGenerator.INSTANCE.generate("test doc", null);
        String id2 = Sha256IdGenerator.INSTANCE.generate("test doc", null);
        assertEquals(id1, id2);
    }

    @Test
    public void testSha256DifferentDocsDifferentIds() {
        String id1 = Sha256IdGenerator.INSTANCE.generate("doc A", null);
        String id2 = Sha256IdGenerator.INSTANCE.generate("doc B", null);
        assertNotEquals(id1, id2);
    }

    @Test
    public void testSha256HexLength() {
        String id = Sha256IdGenerator.INSTANCE.generate("any document", null);
        assertEquals(64, id.length());
        assertTrue("Expected hex chars, got: " + id,
                Pattern.matches("[0-9a-f]{64}", id));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSha256ThrowsOnNullDocument() {
        Sha256IdGenerator.INSTANCE.generate(null, null);
    }

    @Test
    public void testSha256AcceptsEmptyString() {
        String id = Sha256IdGenerator.INSTANCE.generate("", null);
        assertNotNull(id);
        assertEquals(64, id.length());
    }

    @Test
    public void testSha256IgnoresMetadata() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("key", "value");
        String withMeta = Sha256IdGenerator.INSTANCE.generate("same", meta);
        String withoutMeta = Sha256IdGenerator.INSTANCE.generate("same", null);
        assertEquals(withMeta, withoutMeta);
    }

    @Test
    public void testSha256Singleton() {
        assertSame(Sha256IdGenerator.INSTANCE, Sha256IdGenerator.INSTANCE);
    }

    // --- Lambda usage ---

    @Test
    public void testFunctionalInterfaceLambda() {
        IdGenerator custom = new IdGenerator() {
            @Override
            public String generate(String document, Map<String, Object> metadata) {
                return "custom-" + (document != null ? document.hashCode() : "null");
            }
        };
        String id = custom.generate("test", null);
        assertTrue(id.startsWith("custom-"));
    }
}
