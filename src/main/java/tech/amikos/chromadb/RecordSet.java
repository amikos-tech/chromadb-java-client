package tech.amikos.chromadb;

import tech.amikos.chromadb.ids.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

public class RecordSet {
    private List<Record> records;
    private IdGenerator idGenerator;

    public List<String> getIds() {
        return records.stream().map(Record::getId).collect(Collectors.toList());
    }

    public List<String> getDocuments() {
        return records.stream().map(Record::getContent).collect(Collectors.toList());
    }

    public List<List<Float>> getEmbeddings() {
        return records.stream().map(Record::getEmbedding).collect(Collectors.toList());
    }

    public List<Object> getMetadatas() {
        return records.stream().map(Record::getMetadata).collect(Collectors.toList());
    }

    public List<String> getUris() {
        return records.stream().map(Record::getUri).collect(Collectors.toList());
    }
}
