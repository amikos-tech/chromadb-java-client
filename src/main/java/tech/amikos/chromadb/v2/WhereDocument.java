package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Document content filter DSL.
 *
 * <p><strong>Current status:</strong> static factory methods are placeholders and currently throw
 * {@link UnsupportedOperationException}. Use custom {@link WhereDocument} implementations that
 * override {@link #toMap()} until the fluent DSL is implemented.</p>
 */
public abstract class WhereDocument {

    WhereDocument() {}

    public static WhereDocument contains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notContains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument regex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notRegex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument and(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument or(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /** Chain combinator equivalent to {@code WhereDocument.and(this, other)}. */
    public WhereDocument and(WhereDocument other) { return WhereDocument.and(this, other); }

    /** Chain combinator equivalent to {@code WhereDocument.or(this, other)}. */
    public WhereDocument or(WhereDocument other) { return WhereDocument.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
