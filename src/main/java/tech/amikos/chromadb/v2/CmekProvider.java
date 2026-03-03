package tech.amikos.chromadb.v2;

import java.util.Locale;

/** Supported customer-managed key providers. */
public enum CmekProvider {
    GCP("gcp");

    private final String value;

    CmekProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CmekProvider fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("cmek provider must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("gcp".equals(normalized)) {
            return GCP;
        }
        throw new IllegalArgumentException("unsupported CMEK provider: " + value);
    }
}
