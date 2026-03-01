package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Document content filter DSL.
 *
 * <p><strong>Current status:</strong> static factory methods are placeholders and currently throw
 * {@link UnsupportedOperationException}. Use {@link #fromMap(Map)} until the fluent DSL is
 * implemented.</p>
 *
 * <p>For inline document filtering inside {@link Where} clauses, see
 * {@link Where#documentContains(String)} and {@link Where#documentNotContains(String)}. Note that
 * inline {@code #document} filters are Cloud-oriented and may be rejected by local Chroma
 * deployments; this {@code WhereDocument} API remains the local-compatible path.</p>
 */
public abstract class WhereDocument {

    WhereDocument() {}

    /**
     * Builds a {@code WhereDocument} from a raw map payload.
     *
     * @param map non-null Chroma where_document JSON structure
     * @return immutable where_document wrapper around the provided map shape
     * @throws IllegalArgumentException if {@code map} is null
     */
    public static WhereDocument fromMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("map must not be null");
        }
        return new MapWhereDocument(map);
    }

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

    private static Map<String, Object> immutableMapCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(source.size());
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), immutableValueCopy(entry.getValue()));
        }
        return Collections.<String, Object>unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static Object immutableValueCopy(Object value) {
        if (value instanceof Map<?, ?>) {
            return immutableMapCopy((Map<String, Object>) value);
        }
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            List<Object> copy = new ArrayList<Object>(list.size());
            for (Object item : list) {
                copy.add(immutableValueCopy(item));
            }
            return Collections.<Object>unmodifiableList(copy);
        }
        return value;
    }

    private static final class MapWhereDocument extends WhereDocument {
        private final Map<String, Object> map;

        private MapWhereDocument(Map<String, Object> map) {
            this.map = immutableMapCopy(map);
        }

        @Override
        public Map<String, Object> toMap() {
            return map;
        }
    }
}
