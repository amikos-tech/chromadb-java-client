package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class DeleteRecordsRequest {
    @SerializedName("ids")
    private final List<String> ids;

    @SerializedName("where")
    private final Map<String, Object> where;

    @SerializedName("where_document")
    private final Map<String, Object> whereDocument;

    private DeleteRecordsRequest(Builder builder) {
        this.ids = builder.ids;
        this.where = builder.where;
        this.whereDocument = builder.whereDocument;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> ids;
        private Map<String, Object> where;
        private Map<String, Object> whereDocument;

        public Builder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        public Builder where(Where where) {
            this.where = where != null ? where.toMap() : null;
            return this;
        }

        public Builder whereDocument(WhereDocument whereDocument) {
            this.whereDocument = whereDocument != null ? whereDocument.toMap() : null;
            return this;
        }

        public DeleteRecordsRequest build() {
            return new DeleteRecordsRequest(this);
        }
    }
}