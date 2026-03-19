package tech.amikos.chromadb.v2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates deterministic IDs by SHA-256 hashing the document content (or metadata when
 * document is null).
 *
 * <p>Produces a 64-character lowercase hex string. The same document always
 * produces the same ID, enabling content-addressable deduplication. Within a
 * single add/upsert batch, duplicate generated IDs are rejected by the client.
 * For cross-batch deduplication behavior, use {@code upsert(...)}.</p>
 *
 * <p>When document is non-null, only the document is hashed and metadata is ignored.
 * When document is null and metadata is non-null, the metadata is serialized to a
 * deterministic string (sorted keys) and hashed. When both document and metadata are
 * null, throws {@link IllegalArgumentException}.</p>
 *
 * <p>Thread-safe. Use the {@link #INSTANCE} singleton.</p>
 */
public final class Sha256IdGenerator implements IdGenerator {

    public static final Sha256IdGenerator INSTANCE = new Sha256IdGenerator();

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256IdGenerator() {
    }

    @Override
    public String generate(String document, Map<String, Object> metadata) {
        if (document == null && metadata == null) {
            throw new IllegalArgumentException(
                    "Sha256IdGenerator requires a non-null document or metadata"
            );
        }
        String content = document != null ? document : serializeMetadata(metadata);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return hexEncode(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String hexEncode(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX[v >>> 4];
            chars[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(chars);
    }

    /**
     * Deterministic metadata serialization for SHA-256 hashing.
     *
     * <p>Format: sorted key=value pairs joined by semicolons.
     * Keys are sorted lexicographically via {@link java.util.TreeMap}.
     * Values use {@code Object.toString()} (null values serialize as literal "null").
     * This format is stable and documented; do not change without a version migration plan.</p>
     *
     * @param metadata metadata map (may be null or empty)
     * @return deterministic string representation, empty string for null or empty maps
     */
    static String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        Map<String, Object> sorted = new TreeMap<String, Object>(metadata);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(entry.getKey()).append('=');
            sb.append(entry.getValue() == null ? "null" : entry.getValue().toString());
        }
        return sb.toString();
    }
}
