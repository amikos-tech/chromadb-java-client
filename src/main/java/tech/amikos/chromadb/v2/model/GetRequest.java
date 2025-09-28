package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GetRequest {
    @SerializedName("ids")
    private final List<String> ids;

    @SerializedName("where")
    private final Map<String, Object> where;

    @SerializedName("where_document")
    private final Map<String, Object> whereDocument;

    @SerializedName("include")
    private final List<Include> include;

    @SerializedName("limit")
    private final Integer limit;

    @SerializedName("offset")
    private final Integer offset;

    private GetRequest(Builder builder) {
        this.ids = builder.ids;
        this.where = builder.where;
        this.whereDocument = builder.whereDocument;
        this.include = builder.include;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> ids;
        private Map<String, Object> where;
        private Map<String, Object> whereDocument;
        private List<Include> include;
        private Integer limit;
        private Integer offset;

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

        public Builder include(Include... include) {
            this.include = Arrays.asList(include);
            return this;
        }

        public Builder include(List<Include> include) {
            this.include = include;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}