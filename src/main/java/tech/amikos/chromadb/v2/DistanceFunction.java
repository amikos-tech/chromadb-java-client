package tech.amikos.chromadb.v2;

import java.util.Locale;

/** HNSW distance metric. */
public enum DistanceFunction {
    COSINE("cosine"),
    L2("l2"),
    IP("ip");

    private final String value;

    DistanceFunction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DistanceFunction fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DistanceFunction distanceFunction : values()) {
            if (distanceFunction.value.equals(normalized)) {
                return distanceFunction;
            }
        }
        throw new IllegalArgumentException("Unknown distance function: " + value);
    }
}
