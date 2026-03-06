package tech.amikos.chromadb.v2;

import java.util.Map;
import java.util.UUID;

/**
 * Generates random UUID v4 IDs. Ignores document and metadata inputs.
 *
 * <p>Thread-safe. Use the {@link #INSTANCE} singleton.</p>
 */
public final class UuidIdGenerator implements IdGenerator {

    public static final UuidIdGenerator INSTANCE = new UuidIdGenerator();

    private UuidIdGenerator() {
    }

    @Override
    public String generate(String document, Map<String, Object> metadata) {
        return UUID.randomUUID().toString();
    }
}
