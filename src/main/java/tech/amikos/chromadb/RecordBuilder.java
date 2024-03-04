package tech.amikos.chromadb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordBuilder {

    private String content;
    private String id;

    private Map<String, Object> metadata;

    private String uri;

    private List<Float> embedding;

    private RecordBuilder() {
    }

    public static RecordBuilder forContent(String content) {
        RecordBuilder builder = new RecordBuilder();
        builder.content = content;
        return builder;
    }

    public static RecordBuilder forEmbedding(List<Float> embedding) {
        RecordBuilder builder = new RecordBuilder();
        builder.embedding = embedding;
        return builder;
    }

    public RecordBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RecordBuilder withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    public RecordBuilder withMetadatas(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public RecordBuilder withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public RecordBuilder withEmbedding(List<Float> embedding) {
        this.embedding = embedding;
        return this;
    }

    public Record build() {
        return new Record(content, id, metadata, uri, embedding);
    }
}
