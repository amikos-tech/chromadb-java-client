package tech.amikos.chromadb.v2.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Where {
    private final Map<String, Object> conditions;

    private Where(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public Map<String, Object> toMap() {
        return conditions;
    }

    public static Where empty() {
        return new Where(new HashMap<>());
    }

    public static Where eq(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        condition.put(key, value);
        return new Where(condition);
    }

    public static Where ne(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$ne", value);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where gt(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$gt", value);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where gte(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$gte", value);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where lt(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$lt", value);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where lte(String key, Object value) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$lte", value);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where in(String key, List<?> values) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$in", values);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where nin(String key, List<?> values) {
        Map<String, Object> condition = new HashMap<>();
        Map<String, Object> op = new HashMap<>();
        op.put("$nin", values);
        condition.put(key, op);
        return new Where(condition);
    }

    public static Where and(Where... conditions) {
        Map<String, Object> combined = new HashMap<>();
        Map<String, Object> andClause = new HashMap<>();
        Object[] condArray = new Object[conditions.length];
        for (int i = 0; i < conditions.length; i++) {
            condArray[i] = conditions[i].toMap();
        }
        andClause.put("$and", condArray);
        return new Where(andClause);
    }

    public static Where or(Where... conditions) {
        Map<String, Object> combined = new HashMap<>();
        Map<String, Object> orClause = new HashMap<>();
        Object[] condArray = new Object[conditions.length];
        for (int i = 0; i < conditions.length; i++) {
            condArray[i] = conditions[i].toMap();
        }
        orClause.put("$or", condArray);
        return new Where(orClause);
    }

    public Where and(Where other) {
        return and(this, other);
    }

    public Where or(Where other) {
        return or(this, other);
    }
}