package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Type-safe metadata filter DSL.
 *
 * <p><strong>Current status:</strong> static factory methods are placeholders and currently throw
 * {@link UnsupportedOperationException}. Use custom {@link Where} implementations that override
 * {@link #toMap()} until the fluent DSL is implemented.</p>
 */
public abstract class Where {

    Where() {}

    // --- Equality ---

    public static Where eq(String key, String value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where eq(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where eq(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where eq(String key, boolean value) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static Where ne(String key, String value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where ne(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where ne(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where ne(String key, boolean value) { throw new UnsupportedOperationException("Not yet implemented"); }

    // --- Comparison ---

    public static Where gt(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where gt(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where gte(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where gte(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where lt(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where lt(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where lte(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where lte(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }

    // --- Set operations ---

    public static Where in(String key, String... values) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where in(String key, int... values) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where in(String key, float... values) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where nin(String key, String... values) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where nin(String key, int... values) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where nin(String key, float... values) { throw new UnsupportedOperationException("Not yet implemented"); }

    // --- Array metadata operators ---

    public static Where contains(String key, String value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where contains(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where contains(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where contains(String key, boolean value) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static Where notContains(String key, String value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where notContains(String key, int value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where notContains(String key, float value) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where notContains(String key, boolean value) { throw new UnsupportedOperationException("Not yet implemented"); }

    // --- Logical combinators ---

    public static Where and(Where... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where or(Where... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /**
     * Chain combinator equivalent to {@code Where.and(this, other)}.
     *
     * @throws UnsupportedOperationException in the current placeholder implementation
     */
    public Where and(Where other) { return Where.and(this, other); }

    /**
     * Chain combinator equivalent to {@code Where.or(this, other)}.
     *
     * @throws UnsupportedOperationException in the current placeholder implementation
     */
    public Where or(Where other) { return Where.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
