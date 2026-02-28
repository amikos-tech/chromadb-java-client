package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Document content filter DSL.
 *
 * <pre>{@code
 * WhereDocument.contains("machine learning")
 * WhereDocument.and(WhereDocument.contains("AI"), WhereDocument.notContains("deprecated"))
 * }</pre>
 */
public abstract class WhereDocument {

    WhereDocument() {}

    public static WhereDocument contains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument notContains(String text) { throw new UnsupportedOperationException("Not yet implemented"); }

    public static WhereDocument and(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }
    public static WhereDocument or(WhereDocument... conditions) { throw new UnsupportedOperationException("Not yet implemented"); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();
}
