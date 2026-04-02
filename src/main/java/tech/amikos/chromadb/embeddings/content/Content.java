package tech.amikos.chromadb.embeddings.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable multimodal content value type for content-based embedding functions.
 *
 * <p>A {@code Content} contains one or more {@link Part} objects (text, image, audio, video)
 * and an optional {@link Intent} hint for embedding providers that distinguish between
 * document and query embeddings.</p>
 *
 * <p>Use {@link #text(String)} for simple text content, or {@link #builder()} for
 * multi-part content with explicit intent.</p>
 */
public final class Content {

    private final List<Part> parts;
    private final Intent intent;

    private Content(List<Part> parts, Intent intent) {
        this.parts = Collections.unmodifiableList(new ArrayList<Part>(parts));
        this.intent = intent;
    }

    /**
     * Creates a text-only content with a single text part and no intent.
     *
     * @param text the text content; must not be null
     * @return a new Content with a single text part
     */
    public static Content text(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return new Content(Collections.singletonList(Part.text(text)), null);
    }

    /**
     * Returns a new builder for constructing multi-part content.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an unmodifiable list of content parts.
     */
    public List<Part> getParts() {
        return parts;
    }

    /**
     * Returns the embedding intent hint, or {@code null} if not specified.
     */
    public Intent getIntent() {
        return intent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Content)) return false;
        Content content = (Content) o;
        if (!parts.equals(content.parts)) return false;
        return intent == content.intent;
    }

    @Override
    public int hashCode() {
        int result = parts.hashCode();
        result = 31 * result + (intent != null ? intent.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Content{parts=" + parts + ", intent=" + intent + '}';
    }

    /**
     * Builder for constructing {@link Content} instances with multiple parts and optional intent.
     */
    public static final class Builder {

        private final List<Part> parts = new ArrayList<Part>();
        private Intent intent;

        private Builder() {
        }

        /**
         * Adds a part to this content.
         *
         * @param part the part to add; must not be null
         * @return this builder
         */
        public Builder part(Part part) {
            if (part == null) {
                throw new IllegalArgumentException("part must not be null");
            }
            parts.add(part);
            return this;
        }

        /**
         * Sets the embedding intent hint.
         *
         * @param intent the intent; may be null
         * @return this builder
         */
        public Builder intent(Intent intent) {
            this.intent = intent;
            return this;
        }

        /**
         * Builds the content.
         *
         * @return a new Content instance
         * @throws IllegalArgumentException if no parts have been added
         */
        public Content build() {
            if (parts.isEmpty()) {
                throw new IllegalArgumentException("Content must have at least one part");
            }
            return new Content(parts, intent);
        }
    }
}
