package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Document content filter DSL for the {@code where_document} API parameter.
 *
 * <p>All static factory methods produce typed filter clauses that serialize to the Chroma
 * {@code where_document} JSON shape. Use these methods directly with the collection builder
 * {@code whereDocument(...)} methods.</p>
 *
 * <p>For inline document filtering inside {@link Where} clauses, see
 * {@link Where#documentContains(String)} and {@link Where#documentNotContains(String)}. Note that
 * inline {@code #document} filters are Cloud-oriented and may be rejected by local Chroma
 * deployments; this {@code WhereDocument} API remains the local-compatible path.</p>
 */
public abstract class WhereDocument {

    private static final String OP_CONTAINS = "$contains";
    private static final String OP_NOT_CONTAINS = "$not_contains";
    private static final String OP_REGEX = "$regex";
    private static final String OP_NOT_REGEX = "$not_regex";
    private static final String OP_AND = "$and";
    private static final String OP_OR = "$or";

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

    /**
     * Filters documents that contain the specified text.
     *
     * <p>This is the {@code where_document} API parameter for local Chroma deployments.
     * For inline {@code #document} filters in {@code where} (Cloud-oriented), see
     * {@link Where#documentContains(String)}.</p>
     *
     * @param text non-blank document text fragment to match
     * @return where_document clause equivalent to {@code {"$contains": "..."}}
     * @throws IllegalArgumentException if text is null or blank
     */
    public static WhereDocument contains(String text) {
        return leafCondition(OP_CONTAINS, requireNonBlank(text, "text"));
    }

    /**
     * Filters documents that do not contain the specified text.
     *
     * <p>This is the {@code where_document} API parameter for local Chroma deployments.
     * For inline {@code #document} filters in {@code where} (Cloud-oriented), see
     * {@link Where#documentNotContains(String)}.</p>
     *
     * @param text non-blank document text fragment to exclude
     * @return where_document clause equivalent to {@code {"$not_contains": "..."}}
     * @throws IllegalArgumentException if text is null or blank
     */
    public static WhereDocument notContains(String text) {
        return leafCondition(OP_NOT_CONTAINS, requireNonBlank(text, "text"));
    }

    /**
     * Filters documents matching the specified regular expression pattern.
     *
     * @param pattern non-null regex pattern (empty string is a valid pattern)
     * @return where_document clause equivalent to {@code {"$regex": "..."}}
     * @throws IllegalArgumentException if pattern is null
     */
    public static WhereDocument regex(String pattern) {
        requireNonNull(pattern, "pattern");
        return leafCondition(OP_REGEX, pattern);
    }

    /**
     * Filters documents not matching the specified regular expression pattern.
     *
     * @param pattern non-null regex pattern (empty string is a valid pattern)
     * @return where_document clause equivalent to {@code {"$not_regex": "..."}}
     * @throws IllegalArgumentException if pattern is null
     */
    public static WhereDocument notRegex(String pattern) {
        requireNonNull(pattern, "pattern");
        return leafCondition(OP_NOT_REGEX, pattern);
    }

    /**
     * Logical conjunction of child document filter clauses.
     *
     * @param conditions one or more non-null where_document clauses
     * @return where_document clause equivalent to {@code {"$and":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, empty,
     *                                  or contains null entries
     */
    public static WhereDocument and(WhereDocument... conditions) {
        return logicalCondition(OP_AND, conditions);
    }

    /**
     * Logical disjunction of child document filter clauses.
     *
     * @param conditions one or more non-null where_document clauses
     * @return where_document clause equivalent to {@code {"$or":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, empty,
     *                                  or contains null entries
     */
    public static WhereDocument or(WhereDocument... conditions) {
        return logicalCondition(OP_OR, conditions);
    }

    /**
     * Chain combinator equivalent to {@code WhereDocument.and(this, other)}.
     *
     * @throws IllegalArgumentException if {@code other} is null
     */
    public WhereDocument and(WhereDocument other) { return WhereDocument.and(this, other); }

    /**
     * Chain combinator equivalent to {@code WhereDocument.or(this, other)}.
     *
     * @throws IllegalArgumentException if {@code other} is null
     */
    public WhereDocument or(WhereDocument other) { return WhereDocument.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();

    // --- Private helpers ---

    private static WhereDocument leafCondition(String operator, String value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(operator, value);
        return new MapWhereDocument(Collections.<String, Object>unmodifiableMap(map));
    }

    private static WhereDocument logicalCondition(String operator, WhereDocument... conditions) {
        requireNonNull(conditions, "conditions");
        if (conditions.length == 0) {
            throw new IllegalArgumentException("conditions must contain at least 1 clause");
        }
        List<Map<String, Object>> clauses = new ArrayList<Map<String, Object>>(conditions.length);
        for (int i = 0; i < conditions.length; i++) {
            WhereDocument c = conditions[i];
            if (c == null) {
                throw new IllegalArgumentException("conditions[" + i + "] must not be null");
            }
            Map<String, Object> m = c.toMap();
            if (m == null) {
                throw new IllegalArgumentException("conditions[" + i + "].toMap() must not return null");
            }
            clauses.add(m);
        }
        Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
        conditionMap.put(operator, Collections.<Map<String, Object>>unmodifiableList(clauses));
        return new MapWhereDocument(conditionMap);
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        requireNonNull(value, fieldName);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private static Map<String, Object> immutableMapCopy(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object rawKey = entry.getKey();
            if (!(rawKey instanceof String)) {
                String type = rawKey == null ? "null" : rawKey.getClass().getName();
                throw new IllegalArgumentException(
                        "whereDocument map keys must be String, but found key type " + type
                );
            }
            copy.put((String) rawKey, immutableValueCopy(entry.getValue()));
        }
        return Collections.<String, Object>unmodifiableMap(copy);
    }

    private static Object immutableValueCopy(Object value) {
        if (value instanceof Map<?, ?>) {
            return immutableMapCopy((Map<?, ?>) value);
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

        private MapWhereDocument(Map<?, ?> map) {
            this.map = immutableMapCopy(map);
        }

        @Override
        public Map<String, Object> toMap() {
            return map;
        }
    }
}
