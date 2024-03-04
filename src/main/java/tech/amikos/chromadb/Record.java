package tech.amikos.chromadb;

import java.util.List;

public class Record {
    private final String content;
    private final String id;
    private final String uri;
    private final Object metadata;
    private final List<Float> embedding;

    public Record(String content, String id, Object metadata, String uri, List<Float> embedding) {
        this.content = content;
        this.id = id;
        this.metadata = metadata;
        this.uri = uri;
        this.embedding = embedding;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public Object getMetadata() {
        return metadata;
    }

    public String getUri() {
        return uri;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }
}
