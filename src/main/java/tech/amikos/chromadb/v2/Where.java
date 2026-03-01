package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe filter DSL for metadata, ID, and document conditions.
 *
 * <p><strong>Current status:</strong> ID/document filters and logical combinators are implemented.
 * The remaining metadata factory methods are placeholders and currently throw
 * {@link UnsupportedOperationException}.</p>
 */
public abstract class Where {

    static final String KEY_ID = "#id";
    private static final String KEY_DOCUMENT = "#document";

    private static final String OP_IN = "$in";
    private static final String OP_NIN = "$nin";
    private static final String OP_CONTAINS = "$contains";
    private static final String OP_NOT_CONTAINS = "$not_contains";
    private static final String OP_AND = "$and";
    private static final String OP_OR = "$or";

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

    // --- ID operations ---

    /**
     * Builds an inline ID inclusion filter for {@code where}.
     *
     * <p>Can be used as a standalone clause or combined inside {@link #and(Where...)} /
     * {@link #or(Where...)}.</p>
     *
     * <p><strong>Compatibility:</strong> this maps to a {@code #id} filter in {@code where}. Support
     * and semantics may vary by Chroma deployment and operation.</p>
     *
     * @param ids one or more non-blank IDs; leading/trailing whitespace is trimmed
     * @return where clause equivalent to {@code {"#id":{"$in":[...]}}}
     * @throws IllegalArgumentException if {@code ids} is null, empty, or contains null/blank elements
     */
    public static Where idIn(String... ids) { return stringSetCondition(KEY_ID, OP_IN, "ids", ids); }

    /**
     * Builds an inline ID exclusion filter for {@code where}.
     *
     * <p>Can be used as a standalone clause or combined inside {@link #and(Where...)} /
     * {@link #or(Where...)}.</p>
     *
     * <p><strong>Compatibility:</strong> this maps to a {@code #id} filter in {@code where}. Support
     * and semantics may vary by Chroma deployment and operation.</p>
     *
     * @param ids one or more non-blank IDs; leading/trailing whitespace is trimmed
     * @return where clause equivalent to {@code {"#id":{"$nin":[...]}}}
     * @throws IllegalArgumentException if {@code ids} is null, empty, or contains null/blank elements
     */
    public static Where idNotIn(String... ids) { return stringSetCondition(KEY_ID, OP_NIN, "ids", ids); }

    // --- Inline document operators ---

    /**
     * Builds an inline document content inclusion filter for {@code where}.
     *
     * <p>Can be used as a standalone clause or combined inside {@link #and(Where...)} /
     * {@link #or(Where...)}.</p>
     *
     * <p><strong>Compatibility:</strong> inline {@code #document} filters in {@code where} are a
     * Chroma Cloud capability and are rejected by local Chroma deployments. For local deployments,
     * use collection builder {@code whereDocument(...)} methods with a custom
     * {@link WhereDocument} implementation that overrides {@link WhereDocument#toMap()}.</p>
     *
     * @param text non-blank document text fragment to match; leading/trailing whitespace is trimmed
     * @return where clause equivalent to {@code {"#document":{"$contains":"..."}}}
     * @throws IllegalArgumentException if {@code text} is null or blank
     */
    public static Where documentContains(String text) {
        return stringCondition(KEY_DOCUMENT, OP_CONTAINS, "text", text);
    }

    /**
     * Builds an inline document content exclusion filter for {@code where}.
     *
     * <p>Can be used as a standalone clause or combined inside {@link #and(Where...)} /
     * {@link #or(Where...)}.</p>
     *
     * <p><strong>Compatibility:</strong> inline {@code #document} filters in {@code where} are a
     * Chroma Cloud capability and are rejected by local Chroma deployments. For local deployments,
     * use collection builder {@code whereDocument(...)} methods with a custom
     * {@link WhereDocument} implementation that overrides {@link WhereDocument#toMap()}.</p>
     *
     * @param text non-blank document text fragment to exclude; leading/trailing whitespace is trimmed
     * @return where clause equivalent to {@code {"#document":{"$not_contains":"..."}}}
     * @throws IllegalArgumentException if {@code text} is null or blank
     */
    public static Where documentNotContains(String text) {
        return stringCondition(KEY_DOCUMENT, OP_NOT_CONTAINS, "text", text);
    }

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

    /**
     * Logical conjunction of child clauses.
     *
     * @param conditions two or more non-null where clauses
     * @return where clause equivalent to {@code {"$and":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, has fewer than two clauses,
     *                                  contains null entries, or contains a clause with null {@code toMap()} output
     */
    public static Where and(Where... conditions) { return logicalCondition(OP_AND, conditions); }

    /**
     * Logical disjunction of child clauses.
     *
     * @param conditions two or more non-null where clauses
     * @return where clause equivalent to {@code {"$or":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, has fewer than two clauses,
     *                                  contains null entries, or contains a clause with null {@code toMap()} output
     */
    public static Where or(Where... conditions) { return logicalCondition(OP_OR, conditions); }

    /**
     * Chain combinator equivalent to {@code Where.and(this, other)}.
     *
     * @throws IllegalArgumentException if {@code other} is null
     */
    public Where and(Where other) { return Where.and(this, other); }

    /**
     * Chain combinator equivalent to {@code Where.or(this, other)}.
     *
     * @throws IllegalArgumentException if {@code other} is null
     */
    public Where or(Where other) { return Where.or(this, other); }

    /** Serialize to the Chroma filter JSON structure. */
    public abstract Map<String, Object> toMap();

    private static Where stringSetCondition(String key, String operator, String fieldName, String... values) {
        requireNonNullArgument(values, fieldName);
        if (values.length == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }

        List<String> operands = new ArrayList<String>(values.length);
        for (int i = 0; i < values.length; i++) {
            operands.add(requireNonBlank(values[i], fieldName + "[" + i + "]"));
        }

        return operatorCondition(key, operator, Collections.<String>unmodifiableList(operands));
    }

    private static Where stringCondition(String key, String operator, String fieldName, String value) {
        return operatorCondition(key, operator, requireNonBlank(value, fieldName));
    }

    private static Where operatorCondition(String key, String operator, Object operand) {
        Map<String, Object> operatorMap = new LinkedHashMap<String, Object>();
        operatorMap.put(operator, operand);

        Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
        conditionMap.put(key, Collections.<String, Object>unmodifiableMap(operatorMap));

        return new MapWhere(conditionMap);
    }

    private static Where logicalCondition(String operator, Where... conditions) {
        requireNonNullArgument(conditions, "conditions");
        if (conditions.length < 2) {
            throw new IllegalArgumentException("conditions must contain at least 2 clauses");
        }

        List<Map<String, Object>> clauses = new ArrayList<Map<String, Object>>(conditions.length);
        for (int i = 0; i < conditions.length; i++) {
            Where condition = conditions[i];
            if (condition == null) {
                throw new IllegalArgumentException("conditions[" + i + "] must not be null");
            }
            Map<String, Object> conditionMap = condition.toMap();
            if (conditionMap == null) {
                throw new IllegalArgumentException("conditions[" + i + "].toMap() must not return null");
            }
            clauses.add(immutableMapCopy(conditionMap));
        }

        Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
        conditionMap.put(operator, Collections.<Map<String, Object>>unmodifiableList(clauses));

        return new MapWhere(conditionMap);
    }

    private static String requireNonBlank(String value, String fieldName) {
        requireNonNullArgument(value, fieldName);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static void requireNonNullArgument(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

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

    private static final class MapWhere extends Where {
        private final Map<String, Object> map;

        private MapWhere(Map<String, Object> map) {
            this.map = immutableMapCopy(map);
        }

        @Override
        public Map<String, Object> toMap() {
            return map;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MapWhere)) {
                return false;
            }
            MapWhere other = (MapWhere) obj;
            return map.equals(other.map);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        @Override
        public String toString() {
            return "Where" + map.toString();
        }
    }
}
