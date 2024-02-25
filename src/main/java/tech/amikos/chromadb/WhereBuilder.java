package tech.amikos.chromadb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class WhereBuilder {

    private final JsonObject filter = new JsonObject();

    private WhereBuilder() {
    }

    public static WhereBuilder create() {
        return new WhereBuilder();
    }

    public WhereBuilder eq(String field, Object value) {
        return operation("$eq", field, value);
    }

    public WhereBuilder gt(String field, Object value) {
        return operation("$gt", field, value);
    }

    public WhereBuilder gte(String field, Object value) {
        return operation("$gte", field, value);
    }

    public WhereBuilder lt(String field, Object value) {
        return operation("$lt", field, value);
    }

    public WhereBuilder lte(String field, Object value) {
        return operation("$lte", field, value);
    }

    public WhereBuilder ne(String field, Object value) {
        return operation("$ne", field, value);
    }

    public WhereBuilder in(String field, List<Object> value) {
        return operation("$in", field, value);
    }

    public WhereBuilder nin(String field, List<Object> value) {
        return operation("$nin", field, value);
    }

    public WhereBuilder and(WhereBuilder... builders) {
        JsonArray jsonArray = new JsonArray();
        for (WhereBuilder builder : builders) {
            jsonArray.add(builder.filter);
        }
        filter.add("$and", jsonArray);
        return this;
    }

    public WhereBuilder or(WhereBuilder... builders) {
        JsonArray jsonArray = new JsonArray();
        for (WhereBuilder builder : builders) {
            jsonArray.add(builder.filter);
        }
        filter.add("$or", jsonArray);
        return this;
    }

    private WhereBuilder operation(String operation, String field, Object value) {
        JsonObject innerFilter = new JsonObject();
        if (value instanceof List<?>) {
            JsonArray jsonArray = new JsonArray();
            for (Object o : (List<?>) value) {
                if (o instanceof String)
                    jsonArray.add(o.toString());
                else if (o instanceof Integer)
                    jsonArray.add((Integer) o);
                else if (o instanceof Float)
                    jsonArray.add((Float) o);
                else if (o instanceof Boolean)
                    jsonArray.add((Boolean) o);
                else {
                    throw new IllegalArgumentException("Unsupported type: " + o.getClass().getName());
                }
            }
            innerFilter.add(operation, jsonArray);
        } else {
            innerFilter.addProperty(operation, value.toString());
        }
        filter.add(field, innerFilter); // Gson handles various value types
        return this;
    }

    public JsonObject build() {
        return filter;
    }

}
