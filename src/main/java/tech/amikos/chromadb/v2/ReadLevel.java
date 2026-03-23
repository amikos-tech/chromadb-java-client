package tech.amikos.chromadb.v2;

import java.util.Locale;

/**
 * Controls which data sources the search engine reads from when processing a search request.
 *
 * <p>Use {@link #INDEX_AND_WAL} for the most up-to-date results (includes recently written
 * records in the WAL). Use {@link #INDEX_ONLY} for faster but potentially stale results.</p>
 */
public enum ReadLevel {

    /** Read from both the persisted index and the write-ahead log (most up-to-date). */
    INDEX_AND_WAL("index_and_wal"),

    /** Read from the persisted index only (faster, potentially stale). */
    INDEX_ONLY("index_only");

    private final String value;

    ReadLevel(String value) {
        this.value = value;
    }

    /**
     * Returns the wire format value for this read level.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the {@code ReadLevel} matching the given wire format string.
     *
     * @param value the string value to look up; must not be null
     * @return matching {@code ReadLevel}
     * @throws IllegalArgumentException if {@code value} is null or does not match any constant
     */
    public static ReadLevel fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ReadLevel level : values()) {
            if (level.value.equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown read level value: " + value);
    }
}
