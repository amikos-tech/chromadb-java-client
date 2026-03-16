package tech.amikos.chromadb.v2;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Generates deterministic IDs by SHA-256 hashing the document content.
 *
 * <p>Produces a 64-character lowercase hex string. The same document always
 * produces the same ID, enabling content-addressable deduplication. Within a
 * single add/upsert batch, duplicate generated IDs are rejected by the client.
 * For cross-batch deduplication behavior, use {@code upsert(...)}.</p>
 *
 * <p>Requires a non-null document; throws {@link IllegalArgumentException} if
 * the document is null. Metadata is ignored.</p>
 *
 * <p>Thread-safe. Use the {@link #INSTANCE} singleton.</p>
 */
public final class Sha256IdGenerator implements IdGenerator {

    public static final Sha256IdGenerator INSTANCE = new Sha256IdGenerator();

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256IdGenerator() {
    }

    @Override
    public String generate(String document, Map<String, Object> metadata) {
        if (document == null) {
            throw new IllegalArgumentException("Sha256IdGenerator requires a non-null document");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(document.getBytes(UTF_8));
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
}
