package tech.amikos.chromadb.v2;

import java.util.Locale;

/** Supported SPANN quantization modes. */
public enum SpannQuantization {
    NONE("none"),
    // "rabit" spelling intentionally matches upstream wire format.
    FOUR_BIT_RABIT_Q_WITH_U_SEARCH("four_bit_rabit_q_with_u_search");

    private final String value;

    SpannQuantization(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SpannQuantization fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("quantize must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("none".equals(normalized)) {
            return NONE;
        }
        if ("four_bit_rabit_q_with_u_search".equals(normalized)
                || "four_bit_rabbit_q_with_u_search".equals(normalized)) {
            return FOUR_BIT_RABIT_Q_WITH_U_SEARCH;
        }
        throw new IllegalArgumentException("unsupported SPANN quantize value: " + value);
    }
}
