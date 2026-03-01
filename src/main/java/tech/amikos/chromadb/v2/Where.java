package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe filter DSL for metadata, ID, and document conditions.
 *
 * <p>Typed factory methods cover standard Chroma {@code where} operators for metadata, inline
 * ID/document filters, and logical combinators.
 *
 * <p><strong>Compatibility:</strong> this DSL serializes to Chroma {@code where} JSON shape.
 * Operation support/semantics may vary by Chroma deployment and version.</p>
 */
public abstract class Where {

    static final String KEY_ID = "#id";
    private static final String KEY_DOCUMENT = "#document";

    private static final String OP_IN = "$in";
    private static final String OP_NIN = "$nin";
    private static final String OP_EQ = "$eq";
    private static final String OP_NE = "$ne";
    private static final String OP_GT = "$gt";
    private static final String OP_GTE = "$gte";
    private static final String OP_LT = "$lt";
    private static final String OP_LTE = "$lte";
    private static final String OP_CONTAINS = "$contains";
    private static final String OP_NOT_CONTAINS = "$not_contains";
    private static final String OP_AND = "$and";
    private static final String OP_OR = "$or";

    Where() {}

    /**
     * Builds a {@code Where} from a raw map payload.
     *
     * <p>This is an escape hatch for filter shapes not yet covered by typed factory methods.</p>
     *
     * @param map non-null Chroma where JSON structure
     * @return immutable where wrapper around the provided map shape
     * @throws IllegalArgumentException if {@code map} is null
     */
    public static Where fromMap(Map<String, Object> map) {
        requireNonNullArgument(map, "map");
        return new MapWhere(map);
    }

    // --- Equality ---

    /** Metadata equality for string values (value may be empty but must not be null). */
    public static Where eq(String key, String value) { return metadataStringCondition(key, OP_EQ, value); }
    /** Metadata equality for integer values. */
    public static Where eq(String key, int value) { return metadataScalarCondition(key, OP_EQ, Integer.valueOf(value)); }
    /** Metadata equality for float values (must be finite). */
    public static Where eq(String key, float value) { return metadataScalarCondition(key, OP_EQ, Float.valueOf(value)); }
    /** Metadata equality for boolean values. */
    public static Where eq(String key, boolean value) { return metadataScalarCondition(key, OP_EQ, Boolean.valueOf(value)); }

    /** Metadata inequality for string values (value may be empty but must not be null). */
    public static Where ne(String key, String value) { return metadataStringCondition(key, OP_NE, value); }
    /** Metadata inequality for integer values. */
    public static Where ne(String key, int value) { return metadataScalarCondition(key, OP_NE, Integer.valueOf(value)); }
    /** Metadata inequality for float values (must be finite). */
    public static Where ne(String key, float value) { return metadataScalarCondition(key, OP_NE, Float.valueOf(value)); }
    /** Metadata inequality for boolean values. */
    public static Where ne(String key, boolean value) { return metadataScalarCondition(key, OP_NE, Boolean.valueOf(value)); }

    // --- Comparison ---

    /** Metadata greater-than for integer values. */
    public static Where gt(String key, int value) { return metadataScalarCondition(key, OP_GT, Integer.valueOf(value)); }
    /** Metadata greater-than for float values (must be finite). */
    public static Where gt(String key, float value) { return metadataScalarCondition(key, OP_GT, Float.valueOf(value)); }
    /** Metadata greater-than-or-equal for integer values. */
    public static Where gte(String key, int value) { return metadataScalarCondition(key, OP_GTE, Integer.valueOf(value)); }
    /** Metadata greater-than-or-equal for float values (must be finite). */
    public static Where gte(String key, float value) { return metadataScalarCondition(key, OP_GTE, Float.valueOf(value)); }
    /** Metadata less-than for integer values. */
    public static Where lt(String key, int value) { return metadataScalarCondition(key, OP_LT, Integer.valueOf(value)); }
    /** Metadata less-than for float values (must be finite). */
    public static Where lt(String key, float value) { return metadataScalarCondition(key, OP_LT, Float.valueOf(value)); }
    /** Metadata less-than-or-equal for integer values. */
    public static Where lte(String key, int value) { return metadataScalarCondition(key, OP_LTE, Integer.valueOf(value)); }
    /** Metadata less-than-or-equal for float values (must be finite). */
    public static Where lte(String key, float value) { return metadataScalarCondition(key, OP_LTE, Float.valueOf(value)); }

    // --- Set operations ---

    /** Metadata set inclusion for string values (elements may be empty but must not be null). */
    public static Where in(String key, String... values) { return metadataStringSetCondition(key, OP_IN, values); }
    /** Metadata set inclusion for integer values. */
    public static Where in(String key, int... values) { return metadataIntSetCondition(key, OP_IN, values); }
    /** Metadata set inclusion for float values (all elements must be finite). */
    public static Where in(String key, float... values) { return metadataFloatSetCondition(key, OP_IN, values); }
    /** Metadata set inclusion for boolean values. */
    public static Where in(String key, boolean... values) { return metadataBooleanSetCondition(key, OP_IN, values); }
    /** Metadata set exclusion for string values (elements may be empty but must not be null). */
    public static Where nin(String key, String... values) { return metadataStringSetCondition(key, OP_NIN, values); }
    /** Metadata set exclusion for integer values. */
    public static Where nin(String key, int... values) { return metadataIntSetCondition(key, OP_NIN, values); }
    /** Metadata set exclusion for float values (all elements must be finite). */
    public static Where nin(String key, float... values) { return metadataFloatSetCondition(key, OP_NIN, values); }
    /** Metadata set exclusion for boolean values. */
    public static Where nin(String key, boolean... values) { return metadataBooleanSetCondition(key, OP_NIN, values); }

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
     * use collection builder {@code whereDocument(...)} methods with
     * {@link WhereDocument#fromMap(Map)}.</p>
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
     * use collection builder {@code whereDocument(...)} methods with
     * {@link WhereDocument#fromMap(Map)}.</p>
     *
     * @param text non-blank document text fragment to exclude; leading/trailing whitespace is trimmed
     * @return where clause equivalent to {@code {"#document":{"$not_contains":"..."}}}
     * @throws IllegalArgumentException if {@code text} is null or blank
     */
    public static Where documentNotContains(String text) {
        return stringCondition(KEY_DOCUMENT, OP_NOT_CONTAINS, "text", text);
    }

    // --- Array metadata operators ---

    /** Metadata array containment for string values (value must be non-null and non-empty). */
    public static Where contains(String key, String value) { return metadataContainsStringCondition(key, OP_CONTAINS, value); }
    /** Metadata array containment for integer values. */
    public static Where contains(String key, int value) { return metadataScalarCondition(key, OP_CONTAINS, Integer.valueOf(value)); }
    /** Metadata array containment for float values (must be finite). */
    public static Where contains(String key, float value) { return metadataScalarCondition(key, OP_CONTAINS, Float.valueOf(value)); }
    /** Metadata array containment for boolean values. */
    public static Where contains(String key, boolean value) { return metadataScalarCondition(key, OP_CONTAINS, Boolean.valueOf(value)); }

    /** Metadata array non-containment for string values (value must be non-null and non-empty). */
    public static Where notContains(String key, String value) { return metadataContainsStringCondition(key, OP_NOT_CONTAINS, value); }
    /** Metadata array non-containment for integer values. */
    public static Where notContains(String key, int value) { return metadataScalarCondition(key, OP_NOT_CONTAINS, Integer.valueOf(value)); }
    /** Metadata array non-containment for float values (must be finite). */
    public static Where notContains(String key, float value) { return metadataScalarCondition(key, OP_NOT_CONTAINS, Float.valueOf(value)); }
    /** Metadata array non-containment for boolean values. */
    public static Where notContains(String key, boolean value) { return metadataScalarCondition(key, OP_NOT_CONTAINS, Boolean.valueOf(value)); }

    // --- Logical combinators ---

    /**
     * Logical conjunction of child clauses.
     *
     * <p><strong>Compatibility:</strong> one-clause logical expressions are serialized as provided.
     * Some Chroma deployments may require two or more clauses.</p>
     *
     * @param conditions one or more non-null where clauses
     * @return where clause equivalent to {@code {"$and":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, empty,
     *                                  contains null entries, or contains a clause with null {@code toMap()} output
     */
    public static Where and(Where... conditions) { return logicalCondition(OP_AND, conditions); }

    /**
     * Logical disjunction of child clauses.
     *
     * <p><strong>Compatibility:</strong> one-clause logical expressions are serialized as provided.
     * Some Chroma deployments may require two or more clauses.</p>
     *
     * @param conditions one or more non-null where clauses
     * @return where clause equivalent to {@code {"$or":[...]}}
     * @throws IllegalArgumentException if {@code conditions} is null, empty,
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

    private static Where metadataStringCondition(String key, String operator, String value) {
        requireNonNullArgument(value, "value");
        return operatorCondition(requireMetadataKey(key), operator, value);
    }

    private static Where metadataContainsStringCondition(String key, String operator, String value) {
        return operatorCondition(requireMetadataKey(key), operator, requireNonEmpty(value, "value"));
    }

    private static Where metadataScalarCondition(String key, String operator, Object value) {
        requireNonNullArgument(value, "value");
        if (value instanceof Float) {
            requireFiniteFloat(((Float) value).floatValue(), "value");
        }
        return operatorCondition(requireMetadataKey(key), operator, value);
    }

    private static Where metadataStringSetCondition(String key, String operator, String... values) {
        String metadataKey = requireMetadataKey(key);
        requireNonNullArgument(values, "values");
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }

        List<String> operands = new ArrayList<String>(values.length);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            requireNonNullArgument(value, "values[" + i + "]");
            operands.add(value);
        }

        return operatorCondition(
                metadataKey,
                operator,
                Collections.<String>unmodifiableList(operands)
        );
    }

    private static Where metadataIntSetCondition(String key, String operator, int... values) {
        return intSetCondition(requireMetadataKey(key), operator, "values", values);
    }

    private static Where metadataFloatSetCondition(String key, String operator, float... values) {
        return floatSetCondition(requireMetadataKey(key), operator, "values", values);
    }

    private static Where metadataBooleanSetCondition(String key, String operator, boolean... values) {
        return booleanSetCondition(requireMetadataKey(key), operator, "values", values);
    }

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

    private static Where intSetCondition(String key, String operator, String fieldName, int... values) {
        requireNonNullArgument(values, fieldName);
        if (values.length == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }

        List<Integer> operands = new ArrayList<Integer>(values.length);
        for (int i = 0; i < values.length; i++) {
            operands.add(Integer.valueOf(values[i]));
        }
        return operatorCondition(key, operator, Collections.<Integer>unmodifiableList(operands));
    }

    private static Where floatSetCondition(String key, String operator, String fieldName, float... values) {
        requireNonNullArgument(values, fieldName);
        if (values.length == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }

        List<Float> operands = new ArrayList<Float>(values.length);
        for (int i = 0; i < values.length; i++) {
            operands.add(Float.valueOf(requireFiniteFloat(values[i], fieldName + "[" + i + "]")));
        }
        return operatorCondition(key, operator, Collections.<Float>unmodifiableList(operands));
    }

    private static Where booleanSetCondition(String key, String operator, String fieldName, boolean... values) {
        requireNonNullArgument(values, fieldName);
        if (values.length == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }

        List<Boolean> operands = new ArrayList<Boolean>(values.length);
        for (int i = 0; i < values.length; i++) {
            operands.add(Boolean.valueOf(values[i]));
        }
        return operatorCondition(key, operator, Collections.<Boolean>unmodifiableList(operands));
    }

    private static Where operatorCondition(String key, String operator, Object operand) {
        Map<String, Object> operatorMap = new LinkedHashMap<String, Object>();
        operatorMap.put(operator, operand);

        Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
        conditionMap.put(key, Collections.<String, Object>unmodifiableMap(operatorMap));

        return new MapWhere(conditionMap);
    }

    private static String requireMetadataKey(String key) {
        String validated = requireNonEmpty(key, "key");
        if (validated.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be blank (whitespace-only)");
        }
        if (validated.startsWith("#")) {
            throw new IllegalArgumentException(
                    "key must not start with '#'; use dedicated factories for #id and #document filters"
            );
        }
        if (validated.startsWith("$")) {
            throw new IllegalArgumentException(
                    "key must not start with '$' (reserved for operators): " + key
            );
        }
        return validated;
    }

    private static Where logicalCondition(String operator, Where... conditions) {
        requireNonNullArgument(conditions, "conditions");
        if (conditions.length == 0) {
            throw new IllegalArgumentException("conditions must contain at least 1 clause");
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
            clauses.add(conditionMap);
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

    private static String requireNonEmpty(String value, String fieldName) {
        requireNonNullArgument(value, fieldName);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value;
    }

    private static float requireFiniteFloat(float value, String fieldName) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be a finite float");
        }
        return value;
    }

    private static void requireNonNullArgument(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static Map<String, Object> immutableMapCopy(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object rawKey = entry.getKey();
            if (!(rawKey instanceof String)) {
                String type = rawKey == null ? "null" : rawKey.getClass().getName();
                throw new IllegalArgumentException(
                        "where map keys must be String, but found key type " + type
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

    private static final class MapWhere extends Where {
        private final Map<String, Object> map;

        private MapWhere(Map<?, ?> map) {
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
