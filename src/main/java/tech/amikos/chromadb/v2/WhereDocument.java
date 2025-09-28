package tech.amikos.chromadb.v2;

import java.util.HashMap;
import java.util.Map;

public class WhereDocument {
    private final Map<String, Object> conditions;

    private WhereDocument(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public Map<String, Object> toMap() {
        return conditions;
    }

    public static WhereDocument contains(String text) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("$contains", text);
        return new WhereDocument(condition);
    }

    public static WhereDocument notContains(String text) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("$not_contains", text);
        return new WhereDocument(condition);
    }

    public static WhereDocument and(WhereDocument... conditions) {
        Map<String, Object> andClause = new HashMap<>();
        Object[] condArray = new Object[conditions.length];
        for (int i = 0; i < conditions.length; i++) {
            condArray[i] = conditions[i].toMap();
        }
        andClause.put("$and", condArray);
        return new WhereDocument(andClause);
    }

    public static WhereDocument or(WhereDocument... conditions) {
        Map<String, Object> orClause = new HashMap<>();
        Object[] condArray = new Object[conditions.length];
        for (int i = 0; i < conditions.length; i++) {
            condArray[i] = conditions[i].toMap();
        }
        orClause.put("$or", condArray);
        return new WhereDocument(orClause);
    }
}