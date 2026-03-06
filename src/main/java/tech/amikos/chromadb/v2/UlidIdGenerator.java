package tech.amikos.chromadb.v2;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Generates ULID (Universally Unique Lexicographically Sortable Identifier) IDs.
 *
 * <p>Each ULID is a 26-character Crockford base32 string: 10 characters for the
 * 48-bit millisecond timestamp followed by 16 characters of 80-bit cryptographic
 * randomness. ULIDs are lexicographically sortable by creation time.</p>
 *
 * <p>Thread-safe. Use the {@link #INSTANCE} singleton.</p>
 */
public final class UlidIdGenerator implements IdGenerator {

    public static final UlidIdGenerator INSTANCE = new UlidIdGenerator();

    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidIdGenerator() {
    }

    @Override
    public String generate(String document, Map<String, Object> metadata) {
        long timestamp = System.currentTimeMillis();
        byte[] randomBytes = new byte[10]; // 80 bits
        RANDOM.nextBytes(randomBytes);
        return encode(timestamp, randomBytes);
    }

    /**
     * Encodes a timestamp and 10-byte random component into a 26-character ULID string.
     * Package-private for deterministic testing.
     */
    static String encode(long timestamp, byte[] randomBytes) {
        if (randomBytes == null) {
            throw new NullPointerException("randomBytes");
        }
        if (randomBytes.length != 10) {
            throw new IllegalArgumentException("randomBytes must be exactly 10 bytes");
        }
        char[] chars = new char[26];

        // Encode 48-bit timestamp into 10 Crockford base32 characters (most significant first)
        chars[0] = CROCKFORD[(int) ((timestamp >>> 45) & 0x1F)];
        chars[1] = CROCKFORD[(int) ((timestamp >>> 40) & 0x1F)];
        chars[2] = CROCKFORD[(int) ((timestamp >>> 35) & 0x1F)];
        chars[3] = CROCKFORD[(int) ((timestamp >>> 30) & 0x1F)];
        chars[4] = CROCKFORD[(int) ((timestamp >>> 25) & 0x1F)];
        chars[5] = CROCKFORD[(int) ((timestamp >>> 20) & 0x1F)];
        chars[6] = CROCKFORD[(int) ((timestamp >>> 15) & 0x1F)];
        chars[7] = CROCKFORD[(int) ((timestamp >>> 10) & 0x1F)];
        chars[8] = CROCKFORD[(int) ((timestamp >>> 5) & 0x1F)];
        chars[9] = CROCKFORD[(int) (timestamp & 0x1F)];

        // Encode 80-bit random into 16 Crockford base32 characters
        // We treat the 10 bytes as a big-endian 80-bit integer
        chars[10] = CROCKFORD[((randomBytes[0] & 0xFF) >>> 3)];
        chars[11] = CROCKFORD[((randomBytes[0] & 0x07) << 2) | ((randomBytes[1] & 0xFF) >>> 6)];
        chars[12] = CROCKFORD[((randomBytes[1] & 0x3E) >>> 1)];
        chars[13] = CROCKFORD[((randomBytes[1] & 0x01) << 4) | ((randomBytes[2] & 0xFF) >>> 4)];
        chars[14] = CROCKFORD[((randomBytes[2] & 0x0F) << 1) | ((randomBytes[3] & 0xFF) >>> 7)];
        chars[15] = CROCKFORD[((randomBytes[3] & 0x7C) >>> 2)];
        chars[16] = CROCKFORD[((randomBytes[3] & 0x03) << 3) | ((randomBytes[4] & 0xFF) >>> 5)];
        chars[17] = CROCKFORD[(randomBytes[4] & 0x1F)];
        chars[18] = CROCKFORD[((randomBytes[5] & 0xFF) >>> 3)];
        chars[19] = CROCKFORD[((randomBytes[5] & 0x07) << 2) | ((randomBytes[6] & 0xFF) >>> 6)];
        chars[20] = CROCKFORD[((randomBytes[6] & 0x3E) >>> 1)];
        chars[21] = CROCKFORD[((randomBytes[6] & 0x01) << 4) | ((randomBytes[7] & 0xFF) >>> 4)];
        chars[22] = CROCKFORD[((randomBytes[7] & 0x0F) << 1) | ((randomBytes[8] & 0xFF) >>> 7)];
        chars[23] = CROCKFORD[((randomBytes[8] & 0x7C) >>> 2)];
        chars[24] = CROCKFORD[((randomBytes[8] & 0x03) << 3) | ((randomBytes[9] & 0xFF) >>> 5)];
        chars[25] = CROCKFORD[(randomBytes[9] & 0x1F)];

        return new String(chars);
    }
}
