package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Type-safe metadata filter DSL.
 *
 * <pre>{@code
 * Where.eq("type", "article")
 * Where.and(Where.eq("type", "article"), Where.gt("views", 1000))
 * Where.eq("status", "active").and(Where.lt("age", 30))
 * }</pre>
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

    // --- Logical combinators ---

    public static Where and(Where... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static Where or(Where... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /** Chain: {@code Where.eq("a", 1).and(Where.eq("b", 2))} */
    public Where and(Where other) { return and(this, other); }
    public Where or(Where other) { return or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
