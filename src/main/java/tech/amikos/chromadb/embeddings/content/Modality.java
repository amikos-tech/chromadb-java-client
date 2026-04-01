package tech.amikos.chromadb.embeddings.content;

import java.util.Locale;

/**
 * Content modality for multimodal embedding support.
 */
public enum Modality {
    TEXT("text"),
    IMAGE("image"),
    AUDIO("audio"),
    VIDEO("video");

    private final String value;

    Modality(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Modality fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Modality modality : values()) {
            if (modality.value.equals(normalized)) {
                return modality;
            }
        }
        throw new IllegalArgumentException("Unknown modality: " + value);
    }
}
