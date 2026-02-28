package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Document content filter DSL.
 *
 * <pre>{@code
 * WhereDocument.contains("machine learning")
 * WhereDocument.regex("\\bAI\\b")
 * WhereDocument.and(WhereDocument.contains("AI"), WhereDocument.notContains("deprecated"))
 * WhereDocument.contains("AI").and(WhereDocument.notContains("deprecated"))
 * }</pre>
 */
public abstract class WhereDocument {

    WhereDocument() {}

    public static WhereDocument contains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notContains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument regex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notRegex(String pattern) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument and(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument or(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /** Chain: {@code WhereDocument.contains("AI").and(WhereDocument.notContains("old"))} */
    public WhereDocument and(WhereDocument other) { return WhereDocument.and(this, other); }

    /** Chain: {@code WhereDocument.contains("AI").or(WhereDocument.contains("ML"))} */
    public WhereDocument or(WhereDocument other) { return WhereDocument.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
