package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Document content filter DSL.
 *
 * <p><strong>Current status:</strong> static factory methods are placeholders and currently throw
 * {@link UnsupportedOperationException}. Use custom {@link WhereDocument} implementations that
 * override {@link #toMap()} until the fluent DSL is implemented.</p>
 *
 * <p>For inline document filtering inside {@link Where} clauses, see
 * {@link Where#documentContains(String)} and {@link Where#documentNotContains(String)}. Note that
 * inline {@code #document} filters are Cloud-oriented and may be rejected by local Chroma
 * deployments; this {@code WhereDocument} API remains the local-compatible path.</p>
 */
public abstract class WhereDocument {

    WhereDocument() {}

    public static WhereDocument contains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notContains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument regex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notRegex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument and(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument or(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /**
     * Chain combinator equivalent to {@code WhereDocument.and(this, other)}.
     *
     * @throws UnsupportedOperationException in the current placeholder implementation
     */
    public WhereDocument and(WhereDocument other) { return WhereDocument.and(this, other); }

    /**
     * Chain combinator equivalent to {@code WhereDocument.or(this, other)}.
     *
     * @throws UnsupportedOperationException in the current placeholder implementation
     */
    public WhereDocument or(WhereDocument other) { return WhereDocument.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
