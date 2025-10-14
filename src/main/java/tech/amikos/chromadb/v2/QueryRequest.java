package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QueryRequest {
    @SerializedName("query_embeddings")
    private final Object queryEmbeddings;

    @SerializedName("query_texts")
    private final List<String> queryTexts;

    @SerializedName("n_results")
    private final Integer nResults;

    @SerializedName("where")
    private final Map<String, Object> where;

    @SerializedName("where_document")
    private final Map<String, Object> whereDocument;

    @SerializedName("include")
    private final List<Include> include;

    private QueryRequest(Builder builder) {
        this.queryEmbeddings = builder.queryEmbeddings;
        this.queryTexts = builder.queryTexts;
        this.nResults = builder.nResults;
        this.where = builder.where;
        this.whereDocument = builder.whereDocument;
        this.include = builder.include;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object queryEmbeddings;
        private List<String> queryTexts;
        private Integer nResults = 10;
        private Map<String, Object> where;
        private Map<String, Object> whereDocument;
        private List<Include> include;

        public Builder queryEmbeddings(List<List<Float>> embeddings) {
            this.queryEmbeddings = embeddings;
            return this;
        }

        public Builder queryEmbeddingsAsBase64(List<String> embeddings) {
            this.queryEmbeddings = embeddings;
            return this;
        }

        public Builder queryTexts(List<String> texts) {
            this.queryTexts = texts;
            return this;
        }

        public Builder nResults(int nResults) {
            this.nResults = nResults;
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

        public QueryRequest build() {
            if (queryEmbeddings == null && queryTexts == null) {
                throw new IllegalArgumentException("Either queryEmbeddings or queryTexts must be provided");
            }
            if (queryEmbeddings != null && queryTexts != null) {
                throw new IllegalArgumentException("Cannot provide both queryEmbeddings and queryTexts");
            }
            return new QueryRequest(this);
        }
    }
}