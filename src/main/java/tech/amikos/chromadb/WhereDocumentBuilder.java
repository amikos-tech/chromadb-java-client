package tech.amikos.chromadb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class WhereDocumentBuilder {

    private final JsonObject filter = new JsonObject();

    private WhereDocumentBuilder() {
    }

    public static WhereDocumentBuilder create() {
        return new WhereDocumentBuilder();
    }

    public WhereDocumentBuilder contains(String value) {
        return operation("$contains", value);
    }

    public WhereDocumentBuilder notContains(String value) {
        return operation("$not_contains", value);
    }

    public WhereDocumentBuilder and(WhereDocumentBuilder... builders) {
        JsonArray jsonArray = new JsonArray();
        for (WhereDocumentBuilder builder : builders) {
            jsonArray.add(builder.filter);
        }
        filter.add("$and", jsonArray);
        return this;
    }

    public WhereDocumentBuilder or(WhereDocumentBuilder... builders) {
        JsonArray jsonArray = new JsonArray();
        for (WhereDocumentBuilder builder : builders) {
            jsonArray.add(builder.filter);
        }
        filter.add("$or", jsonArray);
        return this;
    }

    private WhereDocumentBuilder operation(String operation, Object value) {
        filter.addProperty(operation, value.toString()); // Gson handles various value types
        return this;
    }

    public JsonObject build() {
        return filter;
    }
}
