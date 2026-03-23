package tech.amikos.chromadb.v2;

import java.util.Objects;

/**
 * Field projection descriptor for Search API results.
 *
 * <p>Predefined constants cover the standard Chroma search output fields. Custom metadata key
 * projections are created via {@link #key(String)}.</p>
 */
public final class Select {

    /** Projects the document text field. */
    public static final Select DOCUMENT = new Select("#document");

    /** Projects the relevance score field. */
    public static final Select SCORE = new Select("#score");

    /** Projects the embedding vector field. */
    public static final Select EMBEDDING = new Select("#embedding");

    /** Projects the metadata map field. */
    public static final Select METADATA = new Select("#metadata");

    /** Projects the record ID field. */
    public static final Select ID = new Select("#id");

    private final String key;

    private Select(String key) {
        this.key = key;
    }

    /**
     * Creates a {@code Select} for a custom metadata field name.
     *
     * <p>The field name is used verbatim (no {@code #} prefix is added). This is suitable for
     * projecting specific metadata keys from the search result.</p>
     *
     * @param fieldName non-null, non-blank metadata key name
     * @return a {@code Select} for the given field name
     * @throws IllegalArgumentException if {@code fieldName} is null or blank
     */
    public static Select key(String fieldName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        if (fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("fieldName must not be blank");
        }
        return new Select(fieldName);
    }

    /**
     * Returns all standard field projections: ID, DOCUMENT, EMBEDDING, METADATA, SCORE.
     */
    public static Select[] all() {
        return new Select[]{ID, DOCUMENT, EMBEDDING, METADATA, SCORE};
    }

    /**
     * Returns the field key string.
     */
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Select)) return false;
        Select that = (Select) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "Select(" + key + ')';
    }
}
